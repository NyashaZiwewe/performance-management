package hr.performancemanagement.controllers;
import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ActionPlan;
import hr.performancemanagement.entities.PerformanceImprovementPlan;
import hr.performancemanagement.entities.ProbationAssessment;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.entities.StrategicObjective;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.wrappers.ChangePasswordWrapper;
import hr.performancemanagement.utils.wrappers.LoginWrapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.URL;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static hr.performancemanagement.utils.PortletUtils.PortletUtils.ERROR_MSGS;

@Controller
@RequestMapping("/")
public class HomeController {
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);
    private static final String REPORTING_DATE_CONFLICT_ALERT_SHOWN = "REPORTING_DATE_CONFLICT_ALERT_SHOWN";

    @ModelAttribute("loginWrapper")
    public LoginWrapper getLoginWrapper(){
        return new LoginWrapper();
    }

    @Autowired
    StrategicObjectiveService strategicObjectiveService;
    @Autowired
    private final ReportingPeriodService reportingPeriodService;
    @Autowired
    private final ScorecardService scorecardService;
    @Autowired
    AccountService accountService;
    @Autowired
    CommonService commonService;
    @Autowired
    NotificationService notificationService;
    @Autowired
    AuditLogService auditLogService;
    @Autowired
    ReportingDateService reportingDateService;
    @Autowired
    ActionPlanService actionPlanService;
    @Autowired
    PerformanceImprovementPlanService performanceImprovementPlanService;
    @Autowired
    ProbationAssessmentService probationAssessmentService;

    public HomeController(ReportingPeriodService reportingPeriodService, ScorecardService scorecardService) {
        this.reportingPeriodService = reportingPeriodService;
        this.scorecardService = scorecardService;
    }

    @RequestMapping
    public ModelAndView goToHome(@RequestParam(value = "reportingPeriodId", required = false) Long reportingPeriodId,
                                 @RequestParam(value = "reportingDateId", required = false) Long reportingDateId,
                                 HttpServletRequest request) {
        ModelAndView modelAndView =  new ModelAndView("index");
        modelAndView.addObject("pageDomain", "Home");
        modelAndView.addObject("pageName", "Home");
        modelAndView.addObject("pageTitle", "Home");

        Account loggedUser = commonService.getLoggedUser();
        boolean adminOrSpecial = commonService.isAdmin() || commonService.hasSpecialRights();
        addReportingDateConflictAlertForAdmin(loggedUser, request);
        modelAndView.addObject("dashboardAdminOrSpecial", adminOrSpecial);
        modelAndView.addObject("dashboardLoggedUser", loggedUser);

        List<ReportingPeriod> reportingPeriods = Optional
                .ofNullable(reportingPeriodService.listAllReportingPeriods())
                .orElse(Collections.emptyList());
        ReportingPeriod selectedPeriod = resolveSelectedReportingPeriod(reportingPeriods, reportingPeriodId);
        List<ReportingDate> reportingDates = selectedPeriod == null
                ? Collections.emptyList()
                : sortReportingDates(Optional.ofNullable(reportingDateService.listAllReportingDates(selectedPeriod)).orElse(Collections.emptyList()));
        ReportingDate selectedReportingDate = resolveSelectedReportingDate(reportingDates, reportingDateId, selectedPeriod);

        modelAndView.addObject("dashboardReportingPeriods", reportingPeriods);
        modelAndView.addObject("dashboardReportingDates", reportingDates);
        modelAndView.addObject("dashboardSelectedPeriodId", selectedPeriod != null ? selectedPeriod.getId() : null);
        modelAndView.addObject("dashboardSelectedDateId", selectedReportingDate != null ? selectedReportingDate.getId() : null);
        modelAndView.addObject("dashboardSelectedPeriod", selectedPeriod);
        modelAndView.addObject("dashboardSelectedDate", selectedReportingDate);
        modelAndView.addObject("dashboardSelectedPeriodLabel",
                selectedPeriod == null ? "No reporting period selected" : selectedPeriod.getStartDate() + " to " + selectedPeriod.getEndDate());
        modelAndView.addObject("dashboardSelectedDateLabel",
                selectedReportingDate == null ? "All reporting dates in selected period" : selectedReportingDate.getEndDate());

        if (selectedPeriod == null) {
            applyDefaultDashboardState(modelAndView);
            PortletUtils.addInfoMsg("No reporting period found. Configure one under Reporting Period to populate the dashboard.", request);
            PortletUtils.addMessagesToPage(modelAndView, request);
            return modelAndView;
        }

        LocalDate periodStart = parseLocalDate(selectedPeriod.getStartDate());
        LocalDate periodEnd = parseLocalDate(selectedPeriod.getEndDate());
        LocalDate snapshotDate = selectedReportingDate != null ? parseLocalDate(selectedReportingDate.getEndDate()) : periodEnd;
        if (snapshotDate == null) {
            snapshotDate = LocalDate.now();
        }
        if (periodEnd != null && snapshotDate.isAfter(periodEnd)) {
            snapshotDate = periodEnd;
        }

        List<Scorecard> scorecards = filterScorecardsByWindow(
                Optional.ofNullable(scorecardService.getScorecardsByReportingPeriodId(selectedPeriod)).orElse(Collections.emptyList()),
                periodStart,
                snapshotDate
        );
        List<Scorecard> scorecardsWithScores = filterScorecardsByIds(
                Optional.ofNullable(scorecardService.getScoresByPeriodId(selectedPeriod)).orElse(Collections.emptyList()),
                scorecards
        );
        Map<Long, Map<Long, Double>> weightedScoresByDate = scorecardService.getScoresByReportingDatesAndScorecardIds(reportingDates, scorecards);

        int scorecardTotal = scorecards.size();
        int lockedScorecards = 0;
        int pendingApprovalScorecards = 0;
        int completedApprovalScorecards = 0;
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null) {
                continue;
            }
            if ("LOCKED".equalsIgnoreCase(safeText(scorecard.getLockStatus()))) {
                lockedScorecards++;
            }
            if (isCompletedApprovalStatus(scorecard.getApprovalStatus())) {
                completedApprovalScorecards++;
            } else {
                pendingApprovalScorecards++;
            }
        }

        int pass = 0;
        int fail = 0;
        int notScored = 0;
        double scoreSum = 0;
        int scoredCount = 0;
        if (selectedReportingDate != null) {
            Map<Long, Double> selectedDateScores = weightedScoresByDate.get(selectedReportingDate.getId());
            for (Scorecard scorecard : scorecards) {
                double score = resolveWeightedScore(selectedDateScores, scorecard);
                if (score <= 0) {
                    notScored++;
                    continue;
                }
                scoredCount++;
                scoreSum += score;
                if (score >= 50) {
                    pass++;
                } else {
                    fail++;
                }
            }
        } else {
            for (Scorecard scorecard : scorecardsWithScores) {
                double weightedScore = safeScore(scorecard.getWeightedScore());
                if (weightedScore <= 0) {
                    notScored++;
                    continue;
                }
                scoredCount++;
                scoreSum += weightedScore;
                double moderatedScore = safeScore(scorecard.getModeratedScore());
                if (moderatedScore >= 2.5) {
                    pass++;
                } else {
                    fail++;
                }
            }
        }
        double averageWeightedScore = scoredCount > 0 ? scoreSum / scoredCount : 0;

        List<StrategicObjective> strategicObjectivesList = Optional
                .ofNullable(strategicObjectiveService.listAllStrategicObjectives(selectedPeriod.getId()))
                .orElse(Collections.emptyList());
        List<String> strategicObjectives = new ArrayList<>();
        for (StrategicObjective strategicObjective : strategicObjectivesList) {
            if (strategicObjective != null && strategicObjective.getName() != null) {
                strategicObjectives.add(strategicObjective.getName());
            }
        }
        List<Double> averageWeights = Optional
                .ofNullable(scorecardService.findAverageAllocatedWeightPerStrategicObjective(selectedPeriod))
                .orElse(Collections.emptyList());
        List<Double> averageScores = Optional
                .ofNullable(scorecardService.findAverageWeightedScorePerStrategicObjective(selectedPeriod))
                .orElse(Collections.emptyList());

        List<String> trendLabels = new ArrayList<>();
        List<Double> trendScores = new ArrayList<>();
        List<Integer> trendCoverage = new ArrayList<>();
        for (ReportingDate reportingDate : reportingDates) {
            LocalDate reportingDateEnd = parseLocalDate(reportingDate == null ? null : reportingDate.getEndDate());
            if (reportingDateEnd != null && snapshotDate != null && reportingDateEnd.isAfter(snapshotDate)) {
                continue;
            }
            trendLabels.add(shortDate(reportingDate == null ? null : reportingDate.getEndDate()));
            double dateTotalScore = 0;
            int dateCoverage = 0;
            Map<Long, Double> scoresByScorecard = reportingDate == null ? null : weightedScoresByDate.get(reportingDate.getId());
            for (Scorecard scorecard : scorecards) {
                double weightedScore = resolveWeightedScore(scoresByScorecard, scorecard);
                if (weightedScore <= 0) {
                    continue;
                }
                dateTotalScore += weightedScore;
                dateCoverage++;
            }
            trendScores.add(dateCoverage == 0 ? 0 : roundTwoDecimals(dateTotalScore / dateCoverage));
            trendCoverage.add(dateCoverage);
        }

        List<ActionPlan> actionPlans = filterActionPlansByWindow(
                Optional.ofNullable(actionPlanService.listAllActionPlans(selectedPeriod)).orElse(Collections.emptyList()),
                periodStart,
                snapshotDate
        );
        int actionOpen = 0;
        int actionInProgress = 0;
        int actionCompleted = 0;
        for (ActionPlan plan : actionPlans) {
            int category = categorizeProjectStatus(plan == null ? null : plan.getStatus());
            if (category == 2) {
                actionCompleted++;
            } else if (category == 1) {
                actionInProgress++;
            } else {
                actionOpen++;
            }
        }

        List<PerformanceImprovementPlan> improvementPlans = filterPerformancePlansByWindow(
                filterVisiblePerformancePlans(
                        Optional.ofNullable(
                                performanceImprovementPlanService.listAllPerformanceImprovementPlans(
                                        loggedUser != null ? loggedUser.getClientId() : 0,
                                        selectedPeriod
                                )
                        ).orElse(Collections.emptyList()),
                        loggedUser,
                        adminOrSpecial
                ),
                periodStart,
                snapshotDate
        );
        int pipOpen = 0;
        int pipInProgress = 0;
        int pipCompleted = 0;
        for (PerformanceImprovementPlan plan : improvementPlans) {
            int category = categorizeProjectStatus(plan == null ? null : plan.getStatus());
            if (category == 2) {
                pipCompleted++;
            } else if (category == 1) {
                pipInProgress++;
            } else {
                pipOpen++;
            }
        }

        List<ProbationAssessment> assessments = filterAssessmentsByWindow(
                Optional.ofNullable(probationAssessmentService.listVisibleAssessments()).orElse(Collections.emptyList()),
                periodStart,
                snapshotDate
        );
        int probationDraft = 0;
        int probationPending = 0;
        int probationAuthorized = 0;
        int probationRejected = 0;
        for (ProbationAssessment assessment : assessments) {
            String status = safeText(assessment == null ? null : assessment.getStatus()).toUpperCase(Locale.ENGLISH);
            if ("AUTHORIZED".equals(status)) {
                probationAuthorized++;
            } else if ("PENDING".equals(status)) {
                probationPending++;
            } else if ("REJECTED".equals(status)) {
                probationRejected++;
            } else {
                probationDraft++;
            }
        }

        List<Account> visibleAccounts = Optional.ofNullable(accountService.listAllAccounts()).orElse(Collections.emptyList());
        int activeAccounts = 0;
        for (Account account : visibleAccounts) {
            if (account != null && "ACTIVE".equalsIgnoreCase(safeText(account.getStatus()))) {
                activeAccounts++;
            }
        }

        modelAndView.addObject("dashboardAccessScope", buildAccessScope(loggedUser, adminOrSpecial));
        modelAndView.addObject("dashboardSnapshotDate", snapshotDate != null ? snapshotDate.toString() : "");
        modelAndView.addObject("dashboardStrategicObjectives", strategicObjectives);
        modelAndView.addObject("dashboardWeightList", averageWeights);
        modelAndView.addObject("dashboardScoreList", averageScores);
        modelAndView.addObject("dashboardTrendLabels", trendLabels);
        modelAndView.addObject("dashboardTrendScores", trendScores);
        modelAndView.addObject("dashboardTrendCoverage", trendCoverage);

        modelAndView.addObject("dashboardScorecardTotal", scorecardTotal);
        modelAndView.addObject("dashboardScorecardPending", pendingApprovalScorecards);
        modelAndView.addObject("dashboardScorecardCompleted", completedApprovalScorecards);
        modelAndView.addObject("dashboardScorecardLocked", lockedScorecards);
        modelAndView.addObject("dashboardPassCount", pass);
        modelAndView.addObject("dashboardFailCount", fail);
        modelAndView.addObject("dashboardNotScoredCount", notScored);
        modelAndView.addObject("dashboardAverageWeightedScore", roundTwoDecimals(averageWeightedScore));

        modelAndView.addObject("dashboardActionPlanTotal", actionPlans.size());
        modelAndView.addObject("dashboardActionPlanOpen", actionOpen);
        modelAndView.addObject("dashboardActionPlanInProgress", actionInProgress);
        modelAndView.addObject("dashboardActionPlanCompleted", actionCompleted);

        modelAndView.addObject("dashboardPipTotal", improvementPlans.size());
        modelAndView.addObject("dashboardPipOpen", pipOpen);
        modelAndView.addObject("dashboardPipInProgress", pipInProgress);
        modelAndView.addObject("dashboardPipCompleted", pipCompleted);

        modelAndView.addObject("dashboardProbationTotal", assessments.size());
        modelAndView.addObject("dashboardProbationDraft", probationDraft);
        modelAndView.addObject("dashboardProbationPending", probationPending);
        modelAndView.addObject("dashboardProbationAuthorized", probationAuthorized);
        modelAndView.addObject("dashboardProbationRejected", probationRejected);

        modelAndView.addObject("dashboardVisibleAccounts", visibleAccounts.size());
        modelAndView.addObject("dashboardActiveAccounts", activeAccounts);
        modelAndView.addObject("dashboardReportingDateCount", reportingDates.size());
        modelAndView.addObject("dashboardStrategicObjectiveCount", strategicObjectives.size());
        modelAndView.addObject("dashboardWorkstreamLabels", Arrays.asList("Action Plans", "Improvement Plans", "Probation"));
        modelAndView.addObject("dashboardWorkstreamOpen", Arrays.asList(actionOpen, pipOpen, probationDraft + probationRejected));
        modelAndView.addObject("dashboardWorkstreamInProgress", Arrays.asList(actionInProgress, pipInProgress, probationPending));
        modelAndView.addObject("dashboardWorkstreamCompleted", Arrays.asList(actionCompleted, pipCompleted, probationAuthorized));

        PortletUtils.addMessagesToPage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/login")
    public ModelAndView goToLoginPage(HttpServletRequest request ){
        ModelAndView modelAndView =  new ModelAndView("login");
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object authException = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            if (authException instanceof AuthenticationException) {
                PortletUtils.addErrorMsg(((AuthenticationException) authException).getMessage(), request);
            } else if (authException instanceof Exception) {
                PortletUtils.addErrorMsg(((Exception) authException).getMessage(), request);
            }
        }
        addErrorMessagesToLoginPage(modelAndView, request);
        PortletUtils.addMessagesToPage(modelAndView, request);
        modelAndView.addObject("localDate", LocalDate.now());
        return modelAndView;
    }

    private void addReportingDateConflictAlertForAdmin(Account loggedUser, HttpServletRequest request) {
        if (loggedUser == null || request == null) {
            return;
        }
        HttpSession session = request.getSession();
        boolean admin = PMConstants.IS_ADMIN.equalsIgnoreCase(loggedUser.getAdmin());
        if (!admin || loggedUser.getClientId() <= 0) {
            session.removeAttribute(REPORTING_DATE_CONFLICT_ALERT_SHOWN);
            return;
        }

        boolean hasConflict = reportingDateService.hasMultipleOpenOrActiveReportingDates(loggedUser.getClientId());
        if (!hasConflict) {
            session.removeAttribute(REPORTING_DATE_CONFLICT_ALERT_SHOWN);
            return;
        }

        boolean alreadyShown = Boolean.TRUE.equals(session.getAttribute(REPORTING_DATE_CONFLICT_ALERT_SHOWN));
        if (!alreadyShown) {
            PortletUtils.addErrorMsg("More than one reporting date is OPEN/ACTIVE. Score capture is blocked for all users until this is fixed.", request);
            session.setAttribute(REPORTING_DATE_CONFLICT_ALERT_SHOWN, Boolean.TRUE);
        }
    }

    @RequestMapping("/change-password/{reset}")
    public ModelAndView changePassword(@PathVariable("reset") String reset, HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView("changePassword");
        ChangePasswordWrapper wrapper = new ChangePasswordWrapper();
        try {
            Account employee = accountService.getAccountToReset(reset);
            wrapper.setEmail(employee.getEmail());
            wrapper.setResetPassword(reset);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Failed with error: "+ e.getMessage(), request);
        }
        modelAndView.addObject("changePasswordWrapper", wrapper);
        modelAndView.addObject("reset", reset);
        PortletUtils.addMessagesToPage(modelAndView, request);
        modelAndView.addObject("localDate", LocalDate.now());
        return modelAndView;
    }

    @RequestMapping("/change-password")
    public ModelAndView changePassword(HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView("changePassword");
        ChangePasswordWrapper wrapper = new ChangePasswordWrapper();
        try {
            Account account = commonService.getLoggedUser();
            String reset = RandomStringUtils.randomAlphanumeric(40);
            account = accountService.updatePasswordResetToken(account, commonService.hashResetToken(reset));

            wrapper.setResetPassword(reset);
            wrapper.setEmail(account.getEmail());
            modelAndView.addObject("changePasswordWrapper", wrapper);
            modelAndView.addObject("reset", null);
            PortletUtils.addMessagesToPage(modelAndView, request);
            return modelAndView;
        }catch (Exception e){
            PortletUtils.addErrorMsg("Account not found. Please recheck your reset link: ", request);
            PortletUtils.addMessagesToPage(modelAndView, request);
            modelAndView.addObject("localDate", LocalDate.now());
            return modelAndView;
        }


    }

    @RequestMapping(value = "/save-password", method = RequestMethod.POST)
    public String savePassword(ChangePasswordWrapper wrapper, HttpServletRequest request){

          try {
              Account employee = accountService.getAccountToReset(wrapper.getEmail(), wrapper.getResetPassword());
              String oldPassword = wrapper.getOldPassword();

              if(employee != null){
                  if(oldPassword != null && !oldPassword.trim().isEmpty()){
                      if(!commonService.matchesPassword(oldPassword, employee.getPassword())){
                          auditPasswordResetFailure(wrapper.getEmail(), "Password reset rejected: incorrect current password", null);
                          PortletUtils.addErrorMsg("Incorrect Old password", request);
                          return "redirect:/change-password/"+ wrapper.getResetPassword();
                      }
                  }
                  if(!wrapper.getNewPassword().equalsIgnoreCase(wrapper.getRepeatPassword())){
                      auditPasswordResetFailure(wrapper.getEmail(), "Password reset rejected: password mismatch", null);
                      PortletUtils.addErrorMsg("Password mismatch. Check new & repeat Password ", request);
                      return "redirect:/change-password/"+ wrapper.getResetPassword();
                  }else {
                      accountService.updatePasswordFromReset(employee, commonService.encodePassword(wrapper.getNewPassword()));
                      auditPasswordResetSuccess(employee);
                      PortletUtils.addInfoMsg("Password successfully changed. You can login using your new Password ", request);
                      return "redirect:/login";
                  }
              }else {
                  auditPasswordResetFailure(wrapper.getEmail(), "Password reset failed: account not found for reset token", null);
                  PortletUtils.addErrorMsg("Password reset failed. Account not found ", request);
                  return "redirect:/change-password/"+ wrapper.getResetPassword();
              }
          }catch (Exception e){
              auditPasswordResetFailure(wrapper.getEmail(), "Password reset failed with exception", e);
              PortletUtils.addErrorMsg("Password reset failed. with message: "+ e.getMessage(), request);
              return "redirect:/change-password/"+ wrapper.getResetPassword();
          }
    }

    @RequestMapping("/reset-password")
    public ModelAndView resetPassword(HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView("resetPassword");
        modelAndView.addObject("localDate", LocalDate.now());
        PortletUtils.addMessagesToPage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/set-reset", method = RequestMethod.POST)
    public String setReset(@RequestParam("username") String username, HttpServletRequest request){
        try {
            Account account = accountService.findAccountByEmail(username);
            String reset = RandomStringUtils.randomAlphanumeric(40);
            if(account != null){
                accountService.updatePasswordResetToken(account, commonService.hashResetToken(reset));
                URL resetLink = new URL(commonService.getCurrentUrl(request).concat("/change-password/"+ reset));
                boolean sent = notificationService.sendPasswordReset(account, resetLink.toString());
                if (sent) {
                    auditPasswordResetRequestSuccess(account, username);
                    PortletUtils.addInfoMsg("Password reset successfully initiated. Login to your email account "+ username +" and click the reset link to change your password", request);
                    return "redirect:/login";
                }
                auditPasswordResetFailure(username, "Password reset email delivery failed", null);
                PortletUtils.addErrorMsg("Password reset could not be emailed to " + username + ". Ask admin to check email settings and try again.", request);
                return "redirect:/reset-password";
            }else {
                auditPasswordResetFailure(username, "Password reset request failed: account not found", null);
                PortletUtils.addErrorMsg("Password reset failed. Account not found ", request);
                return "redirect:/reset-password";
            }
        }catch (Exception e){
            auditPasswordResetFailure(username, "Password reset request failed with exception", e);
            PortletUtils.addErrorMsg("Password reset failed. with message: "+ e.getMessage(), request);
            return "redirect:/reset-password";
        }
    }

    private void addErrorMessagesToLoginPage(ModelAndView modelAndView, HttpServletRequest request) {
        HttpSession session = request.getSession();
        List<String> errorMsgs = (List<String>) session.getAttribute(ERROR_MSGS);
        if (errorMsgs != null) {
            modelAndView.addObject(ERROR_MSGS, errorMsgs);
        }
        session.removeAttribute(ERROR_MSGS);
    }

    private void applyDefaultDashboardState(ModelAndView modelAndView) {
        modelAndView.addObject("dashboardAccessScope", "No reporting period available");
        modelAndView.addObject("dashboardSnapshotDate", "");
        modelAndView.addObject("dashboardStrategicObjectives", Collections.emptyList());
        modelAndView.addObject("dashboardWeightList", Collections.emptyList());
        modelAndView.addObject("dashboardScoreList", Collections.emptyList());
        modelAndView.addObject("dashboardTrendLabels", Collections.emptyList());
        modelAndView.addObject("dashboardTrendScores", Collections.emptyList());
        modelAndView.addObject("dashboardTrendCoverage", Collections.emptyList());
        modelAndView.addObject("dashboardScorecardTotal", 0);
        modelAndView.addObject("dashboardScorecardPending", 0);
        modelAndView.addObject("dashboardScorecardCompleted", 0);
        modelAndView.addObject("dashboardScorecardLocked", 0);
        modelAndView.addObject("dashboardPassCount", 0);
        modelAndView.addObject("dashboardFailCount", 0);
        modelAndView.addObject("dashboardNotScoredCount", 0);
        modelAndView.addObject("dashboardAverageWeightedScore", 0d);
        modelAndView.addObject("dashboardActionPlanTotal", 0);
        modelAndView.addObject("dashboardActionPlanOpen", 0);
        modelAndView.addObject("dashboardActionPlanInProgress", 0);
        modelAndView.addObject("dashboardActionPlanCompleted", 0);
        modelAndView.addObject("dashboardPipTotal", 0);
        modelAndView.addObject("dashboardPipOpen", 0);
        modelAndView.addObject("dashboardPipInProgress", 0);
        modelAndView.addObject("dashboardPipCompleted", 0);
        modelAndView.addObject("dashboardProbationTotal", 0);
        modelAndView.addObject("dashboardProbationDraft", 0);
        modelAndView.addObject("dashboardProbationPending", 0);
        modelAndView.addObject("dashboardProbationAuthorized", 0);
        modelAndView.addObject("dashboardProbationRejected", 0);
        modelAndView.addObject("dashboardVisibleAccounts", 0);
        modelAndView.addObject("dashboardActiveAccounts", 0);
        modelAndView.addObject("dashboardReportingDateCount", 0);
        modelAndView.addObject("dashboardStrategicObjectiveCount", 0);
        modelAndView.addObject("dashboardWorkstreamLabels", Arrays.asList("Action Plans", "Improvement Plans", "Probation"));
        modelAndView.addObject("dashboardWorkstreamOpen", Arrays.asList(0, 0, 0));
        modelAndView.addObject("dashboardWorkstreamInProgress", Arrays.asList(0, 0, 0));
        modelAndView.addObject("dashboardWorkstreamCompleted", Arrays.asList(0, 0, 0));
    }

    private ReportingPeriod resolveSelectedReportingPeriod(List<ReportingPeriod> reportingPeriods, Long reportingPeriodId) {
        if (reportingPeriods == null || reportingPeriods.isEmpty()) {
            return null;
        }
        if (reportingPeriodId != null) {
            for (ReportingPeriod period : reportingPeriods) {
                if (period != null && period.getId() == reportingPeriodId) {
                    return period;
                }
            }
        }
        ReportingPeriod activePeriod = reportingPeriodService.getActiveReportingPeriod();
        if (activePeriod != null) {
            for (ReportingPeriod period : reportingPeriods) {
                if (period != null && period.getId() == activePeriod.getId()) {
                    return period;
                }
            }
        }
        return reportingPeriods.get(0);
    }

    private ReportingDate resolveSelectedReportingDate(List<ReportingDate> reportingDates, Long reportingDateId, ReportingPeriod selectedPeriod) {
        if (reportingDates == null || reportingDates.isEmpty()) {
            return null;
        }
        if (reportingDateId != null) {
            for (ReportingDate reportingDate : reportingDates) {
                if (reportingDate != null && reportingDate.getId() == reportingDateId) {
                    return reportingDate;
                }
            }
        }
        ReportingDate activeReportingDate = reportingDateService.getActiveReportingDate();
        if (activeReportingDate != null && selectedPeriod != null
                && activeReportingDate.getReportingPeriod() != null
                && activeReportingDate.getReportingPeriod().getId() == selectedPeriod.getId()) {
            for (ReportingDate reportingDate : reportingDates) {
                if (reportingDate != null && reportingDate.getId() == activeReportingDate.getId()) {
                    return reportingDate;
                }
            }
        }
        return null;
    }

    private List<ReportingDate> sortReportingDates(List<ReportingDate> reportingDates) {
        List<ReportingDate> sorted = new ArrayList<>();
        if (reportingDates != null) {
            sorted.addAll(reportingDates);
        }
        sorted.sort((left, right) -> {
            LocalDate leftDate = parseLocalDate(left == null ? null : left.getEndDate());
            LocalDate rightDate = parseLocalDate(right == null ? null : right.getEndDate());
            if (leftDate != null && rightDate != null) {
                return leftDate.compareTo(rightDate);
            }
            if (leftDate != null) {
                return -1;
            }
            if (rightDate != null) {
                return 1;
            }
            Date leftCreated = left == null ? null : left.getDate();
            Date rightCreated = right == null ? null : right.getDate();
            if (leftCreated != null && rightCreated != null) {
                return leftCreated.compareTo(rightCreated);
            }
            return 0;
        });
        return sorted;
    }

    private List<Scorecard> filterScorecardsByWindow(List<Scorecard> scorecards, LocalDate periodStart, LocalDate snapshotDate) {
        List<Scorecard> filtered = new ArrayList<>();
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || isWithinWindow(scorecard.getDate(), periodStart, snapshotDate)) {
                filtered.add(scorecard);
            }
        }
        return filtered;
    }

    private List<Scorecard> filterScorecardsByIds(List<Scorecard> source, List<Scorecard> reference) {
        Set<Long> ids = new HashSet<>();
        for (Scorecard scorecard : reference) {
            if (scorecard != null) {
                ids.add(scorecard.getId());
            }
        }
        List<Scorecard> filtered = new ArrayList<>();
        for (Scorecard scorecard : source) {
            if (scorecard != null && ids.contains(scorecard.getId())) {
                filtered.add(scorecard);
            }
        }
        return filtered;
    }

    private double resolveWeightedScore(Map<Long, Double> scoresByScorecard, Scorecard scorecard) {
        if (scoresByScorecard == null || scorecard == null) {
            return 0.0;
        }
        Double score = scoresByScorecard.get(scorecard.getId());
        return safeScore(score);
    }

    private List<ActionPlan> filterActionPlansByWindow(List<ActionPlan> plans, LocalDate periodStart, LocalDate snapshotDate) {
        List<ActionPlan> filtered = new ArrayList<>();
        for (ActionPlan plan : plans) {
            if (plan == null || isWithinWindow(plan.getDate(), periodStart, snapshotDate)) {
                filtered.add(plan);
            }
        }
        return filtered;
    }

    private List<PerformanceImprovementPlan> filterPerformancePlansByWindow(List<PerformanceImprovementPlan> plans, LocalDate periodStart, LocalDate snapshotDate) {
        List<PerformanceImprovementPlan> filtered = new ArrayList<>();
        for (PerformanceImprovementPlan plan : plans) {
            if (plan == null || isWithinWindow(plan.getDate(), periodStart, snapshotDate)) {
                filtered.add(plan);
            }
        }
        return filtered;
    }

    private List<ProbationAssessment> filterAssessmentsByWindow(List<ProbationAssessment> assessments, LocalDate periodStart, LocalDate snapshotDate) {
        List<ProbationAssessment> filtered = new ArrayList<>();
        for (ProbationAssessment assessment : assessments) {
            if (assessment == null || isWithinWindow(assessment.getDate(), periodStart, snapshotDate)) {
                filtered.add(assessment);
            }
        }
        return filtered;
    }

    private List<PerformanceImprovementPlan> filterVisiblePerformancePlans(List<PerformanceImprovementPlan> plans, Account loggedUser, boolean adminOrSpecial) {
        if (adminOrSpecial || loggedUser == null) {
            return plans;
        }

        String accountType = safeText(loggedUser.getAccountType()).toUpperCase(Locale.ENGLISH);
        List<PerformanceImprovementPlan> visible = new ArrayList<>();
        for (PerformanceImprovementPlan plan : plans) {
            if (plan == null) {
                continue;
            }
            Account employee = plan.getEmployee();
            if (employee == null) {
                continue;
            }
            if ("EMPLOYEE".equals(accountType)) {
                if (employee.getId() == loggedUser.getId()) {
                    visible.add(plan);
                }
                continue;
            }
            if ("SUPERVISOR".equals(accountType)) {
                if (employee.getId() == loggedUser.getId()
                        || (employee.getSupervisor() != null && employee.getSupervisor().getId() == loggedUser.getId())) {
                    visible.add(plan);
                }
                continue;
            }
            if ("DEPARTMENT_MANAGER".equals(accountType) || "DIVISIONAL_DIRECTOR".equals(accountType)) {
                if (loggedUser.getDepartment() != null
                        && employee.getDepartment() != null
                        && employee.getDepartment().getId() == loggedUser.getDepartment().getId()) {
                    visible.add(plan);
                }
                continue;
            }
            if ("ACTING_CEO".equals(accountType) || "CEO".equals(accountType)) {
                if (employee.getClientId() == loggedUser.getClientId()) {
                    visible.add(plan);
                }
                continue;
            }
            if (employee.getId() == loggedUser.getId()) {
                visible.add(plan);
            }
        }
        return visible;
    }

    private boolean isWithinWindow(Date createdDate, LocalDate periodStart, LocalDate snapshotDate) {
        if (createdDate == null) {
            return true;
        }
        LocalDate createdLocalDate = toLocalDate(createdDate);
        if (createdLocalDate == null) {
            return true;
        }
        if (periodStart != null && createdLocalDate.isBefore(periodStart)) {
            return false;
        }
        return snapshotDate == null || !createdLocalDate.isAfter(snapshotDate);
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        if (date instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) date).toLocalDateTime().toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String shortDate(String isoDate) {
        LocalDate parsedDate = parseLocalDate(isoDate);
        if (parsedDate == null) {
            return isoDate == null ? "" : isoDate;
        }
        return parsedDate.format(DateTimeFormatter.ofPattern("dd MMM"));
    }

    private int categorizeProjectStatus(String status) {
        String normalized = safeText(status).toLowerCase(Locale.ENGLISH).replace("_", "").replace("-", "");
        if (normalized.contains("complete") || normalized.contains("closed")) {
            return 2;
        }
        if (normalized.contains("progress") || normalized.contains("hold") || normalized.contains("pending")) {
            return 1;
        }
        return 0;
    }

    private boolean isCompletedApprovalStatus(String status) {
        String normalized = safeText(status).toUpperCase(Locale.ENGLISH);
        return "MODERATED_BY_HR".equals(normalized) || "CLOSED".equals(normalized);
    }

    private double safeScore(Double value) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return 0;
        }
        return value;
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String buildAccessScope(Account loggedUser, boolean adminOrSpecial) {
        if (loggedUser == null) {
            return "Session user not found";
        }
        if (adminOrSpecial) {
            return "Client-wide dashboard view";
        }
        String accountType = safeText(loggedUser.getAccountType());
        if (accountType.isEmpty()) {
            return "Restricted dashboard view";
        }
        return accountType + " dashboard view";
    }

    public List<String> listMonthsWithinAReportingPeriod(String sDate, String eDate) throws ParseException {

        Date startDate = new SimpleDateFormat("yyyy-MM-dd").parse(sDate);
        Date endDate = new SimpleDateFormat("yyyy-MM-dd").parse(eDate);

        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] months = dfs.getMonths();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("CAT"));

        long monthsBetween = ChronoUnit.MONTHS.between(
                YearMonth.from(LocalDate.parse(sDate)),
                YearMonth.from(LocalDate.parse(eDate))
        );
        log.debug("Months between dates: {}", monthsBetween);
        List<String> monthsList = new ArrayList<>();
        for(int x = 0; x <= monthsBetween; x++){

            cal.setTime(startDate);
            cal.add(Calendar.MONTH, x);
            int month = cal.get(Calendar.MONTH);
            log.debug("Processing month: {}", months[month]);
            monthsList.add(months[month]);
        }
      return monthsList;
    }

    private void auditPasswordResetRequestSuccess(Account account, String email) {
        try {
            auditLogService.logSuccess(
                    "initiatePasswordReset",
                    "Account",
                    new Object[]{
                            "email=" + sanitizeAuditValue(email),
                            "accountId=" + (account != null ? account.getId() : null)
                    },
                    "Password reset link generated and email queued"
            );
        } catch (Exception ignored) {
            // Audit failures must not interrupt password reset flow.
        }
    }

    private void auditPasswordResetSuccess(Account account) {
        try {
            auditLogService.logSuccess(
                    "completePasswordReset",
                    "Account",
                    new Object[]{
                            "email=" + sanitizeAuditValue(account != null ? account.getEmail() : null),
                            "accountId=" + (account != null ? account.getId() : null)
                    },
                    "Password updated successfully"
            );
        } catch (Exception ignored) {
            // Audit failures must not interrupt password reset flow.
        }
    }

    private void auditPasswordResetFailure(String email, String reason, Exception exception) {
        try {
            Throwable throwable = exception != null ? exception : new IllegalStateException(reason);
            auditLogService.logFailure(
                    "passwordReset",
                    "Account",
                    new Object[]{
                            "email=" + sanitizeAuditValue(email),
                            "reason=" + sanitizeAuditValue(reason)
                    },
                    throwable
            );
        } catch (Exception ignored) {
            // Audit failures must not interrupt password reset flow.
        }
    }

    private String sanitizeAuditValue(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > 180) {
            return trimmed.substring(0, 180) + "...";
        }
        return trimmed;
    }

}
