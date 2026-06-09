package hr.performancemanagement.controllers.assessment;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.EvidenceRepository;
import hr.performancemanagement.repository.ProbationAssessmentRepository;
import hr.performancemanagement.repository.ProbationKpiRepository;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.service.OutcomeService;
import hr.performancemanagement.service.OutputService;
import hr.performancemanagement.service.OverallCommentService;
import hr.performancemanagement.service.OverallScoreService;
import hr.performancemanagement.service.PdfGeneratorService;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.constants.AccessPermissions;
import hr.performancemanagement.utils.constants.Pages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/performance-review")
public class AssessmentController {

    private static final Logger log = LoggerFactory.getLogger(AssessmentController.class);

    @Autowired
    ReportingPeriodService reportingPeriodService;
    @Autowired
    ScorecardService scorecardService;

    @Autowired
    PerformanceImprovementPlanService performanceImprovementPlanService;
    @Autowired
    ActionPlanService actionPlanService;
    @Autowired
    private final TargetService targetService;
    @Autowired
    private final GoalService goalService;
    @Autowired
    private final OutcomeService outcomeService;
    @Autowired
    private final AccountService accountService;
    private static final String PDF_RESOURCES = "";
    @Autowired
    private SpringTemplateEngine templateEngine;
    @Autowired
    CommonService commonService;
    @Autowired
    OutputService outputService;
    @Autowired
    OverallScoreService overallScoreService;
    @Autowired
    ReportingDateService reportingDateService;
    @Autowired
    CommonService cs;
    @Autowired
    private PdfGeneratorService pdfGeneratorService;
    @Autowired
    OverallCommentService overallCommentService;
    @Autowired
    ScoreRepository scoreRepository;
    @Autowired
    EvidenceRepository evidenceRepository;
    @Autowired
    ProbationAssessmentRepository probationAssessmentRepository;
    @Autowired
    ProbationKpiRepository probationKpiRepository;
    @Autowired
    SystemSettingService systemSettingService;
    @Autowired
    AccessControlService accessControlService;
    private List<Double> scores;

    public AssessmentController(TargetService targetService, GoalService goalService, OutcomeService outcomeService, AccountService accountService) {
        this.targetService = targetService;
        this.goalService = goalService;
        this.outcomeService = outcomeService;
        this.accountService = accountService;
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {
        preparePage(modelAndView, request, true);
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request, boolean loadSupportingData) {
        List<Account> ACCOUNTS_LIST = loadSupportingData
                ? accountService.listAllAccounts()
                : Collections.emptyList();
        ReportingDate activeReportingDate = loadSupportingData ? reportingDateService.getActiveReportingDate() : null;
        boolean captureWindowOpen = loadSupportingData && reportingDateService.isReportingDateOpen(activeReportingDate);
        modelAndView.addObject("pageDomain", "Performance Review");
        modelAndView.addObject("pageName", "Assessments");
        modelAndView.addObject("profile", "moderator");
        modelAndView.addObject("accountsList", ACCOUNTS_LIST);
        modelAndView.addObject("captureWindowOpen", captureWindowOpen);
        PortletUtils.addMessagesToPage(modelAndView, request);
    }


    @RequestMapping(value="/view-scores-select-year")
    public ModelAndView viewScoresSelectYear(HttpServletRequest request) {
        List<ReportingPeriod> reportingPeriods = reportingPeriodService.listAllReportingPeriods();
        ReportingPeriod reportingPeriod = resolveDefaultScoresReportingPeriod(reportingPeriods);
        if (reportingPeriod != null) {
            return new ModelAndView("redirect:/performance-review/view-scores/" + reportingPeriod.getId());
        }

        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_SCORES_SELECT_YEAR);
        modelAndView.addObject("pageTitle", "Select Reporting Period");
        modelAndView.addObject("reportingPeriodsList", reportingPeriods);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value="/view-performance-levels-select-year")
    public ModelAndView viewPerformanceLevelsSelectYear(@RequestParam(value = "fromDate", required = false) String fromDate,
                                                        @RequestParam(value = "toDate", required = false) String toDate,
                                                        @RequestParam(value = "fromReportingDateId", required = false) Long fromReportingDateId,
                                                        @RequestParam(value = "toReportingDateId", required = false) Long toReportingDateId,
                                                        @RequestParam(value = "employeeIds", required = false) List<Long> employeeIds,
                                                        HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PERFORMANCE_LEVELS_SELECT_YEAR);
        modelAndView.addObject("pageTitle", "Select Employees");
        List<ReportingDate> reportingDates = resolveAllReportingDates(reportingPeriodService.listAllReportingPeriods());
        String defaultDate = resolveDefaultPerformanceLevelDate(reportingDates);
        ReportingDate selectedFromReportingDate = resolvePerformanceLevelReportingDateSelection(reportingDates, fromReportingDateId, fromDate, defaultDate);
        ReportingDate selectedToReportingDate = resolvePerformanceLevelReportingDateSelection(reportingDates, toReportingDateId, toDate, defaultDate);
        modelAndView.addObject("accountsList", accountService.listAllAccounts());
        modelAndView.addObject("performanceLevelReportingDateOptions", buildPerformanceLevelReportingDateOptions(reportingDates));
        modelAndView.addObject("selectedFromReportingDateId", selectedFromReportingDate == null ? null : selectedFromReportingDate.getId());
        modelAndView.addObject("selectedToReportingDateId", selectedToReportingDate == null ? null : selectedToReportingDate.getId());
        modelAndView.addObject("selectedEmployeeIds", normalizeEmployeeIds(employeeIds));
        modelAndView.addObject("selectedFromDate", selectedFromReportingDate == null ? (hasText(fromDate) ? fromDate : defaultDate) : resolveReportingDateLabel(selectedFromReportingDate));
        modelAndView.addObject("selectedToDate", selectedToReportingDate == null ? (hasText(toDate) ? toDate : defaultDate) : resolveReportingDateLabel(selectedToReportingDate));
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value="/view-individual-trends-select-year")
    public ModelAndView viewIndividualTrendsSelectEmployee(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_INDIVIDUAL_TRENDS_SELECT_EMPLOYEE);
        modelAndView.addObject("pageTitle", "Select Employee");
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value="/select-scorecards/{reportingPeriodId}")
    public ModelAndView viewPerformanceLevelsSelectScorecards(@PathVariable("reportingPeriodId") long reportingPeriodId,
                                                              @RequestParam(value = "fromDate", required = false) String fromDate,
                                                              @RequestParam(value = "toDate", required = false) String toDate,
                                                              @RequestParam(value = "fromReportingDateId", required = false) Long fromReportingDateId,
                                                              @RequestParam(value = "toReportingDateId", required = false) Long toReportingDateId,
                                                              @RequestParam(value = "employeeIds", required = false) List<Long> employeeIds,
                                                              HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PERFORMANCE_LEVELS_SELECT_SCORECARDS);
        modelAndView.addObject("pageTitle", "Select Scorecards");
        ReportingPeriod period = reportingPeriodService.getReportingPeriodById(reportingPeriodId);
        List<Scorecard> scoresList = scorecardService.getScoresByPeriodId(period);
        modelAndView.addObject("scoresList", scoresList);
        modelAndView.addObject("selectedReportingPeriodId", period == null ? null : period.getId());
        modelAndView.addObject("selectedEmployeeIds", normalizeEmployeeIds(employeeIds));
        modelAndView.addObject("selectedFromDate", fromDate);
        modelAndView.addObject("selectedToDate", toDate);
        modelAndView.addObject("selectedFromReportingDateId", fromReportingDateId);
        modelAndView.addObject("selectedToReportingDateId", toReportingDateId);
        preparePage(modelAndView, request);
        return modelAndView;
    }


    @RequestMapping(value = "/view-scores-select-year", method = RequestMethod.POST)
    public String goToViewScores(HttpServletRequest request,
                                 @RequestParam(value = "reportingPeriodId", required = false) Long reportingPeriodId,
                                 @RequestParam(value = "reportingDateId", required = false) Long reportingDateId) {
        if (reportingPeriodId == null || reportingPeriodId <= 0) {
            PortletUtils.addValidationErrorMsg("Select a reporting period before applying filters.", request);
            return "redirect:/performance-review/view-scores-select-year";
        }
        String redirectUrl = "redirect:/performance-review/view-scores/" + reportingPeriodId + "?applyFilters=true";
        if (reportingDateId != null && reportingDateId > 0) {
            redirectUrl += "&reportingDateId=" + reportingDateId;
        }
        return redirectUrl;
    }

    @RequestMapping(value = "/view-performance-levels-select-year", method = RequestMethod.POST)
    public String goToSelectScorecards(HttpServletRequest request,
                                       @RequestParam(value = "reportingPeriodId", required = false) Long reportingPeriodId) {
        if (reportingPeriodId == null || reportingPeriodId <= 0) {
            PortletUtils.addValidationErrorMsg("Select a reporting period before continuing.", request);
            return "redirect:/performance-review/view-performance-levels-select-year";
        }

        return "redirect:/performance-review/select-scorecards/"+ reportingPeriodId;
    }


    @RequestMapping("/view-scores/{id}")
    public ModelAndView viewScores(@PathVariable("id") long id,
                                   @RequestParam(value = "reportingDateId", required = false) Long reportingDateId,
                                   @RequestParam(value = "scoreFilter", required = false) String scoreFilter,
                                   @RequestParam(value = "applyFilters", defaultValue = "false") boolean applyFilters,
                                   HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_SCORES);
        modelAndView.addObject("pageTitle", "View Scores");
        modelAndView.addObject("scoreFiltersApplied", applyFilters);

        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(id);
        List<ReportingPeriod> reviewReportingPeriods = reportingPeriodService.listAllReportingPeriods();
        String startDate = reportingPeriod.getStartDate();
        String endDate = reportingPeriod.getEndDate();
        List<ReportingDate> reportingDates = sortReportingDates(reportingDateService.listAllReportingDates(reportingPeriod));
        ReportingDate selectedReportingDate = resolveSelectedReviewReportingDate(reportingDates, reportingDateId);
        modelAndView.addObject("reviewReportingPeriods", reviewReportingPeriods);
        modelAndView.addObject("reviewReportingDateOptions", buildReviewReportingDateOptions(reviewReportingPeriods));
        modelAndView.addObject("selectedReportingPeriod", reportingPeriod);
        modelAndView.addObject("selectedReportingPeriodId", reportingPeriod.getId());
        modelAndView.addObject("selectedReportingDate", selectedReportingDate);
        modelAndView.addObject("selectedReportingDateId", selectedReportingDate == null ? null : selectedReportingDate.getId());
        modelAndView.addObject("selectedReportingPeriodLabel", startDate + " to " + endDate);
        modelAndView.addObject("selectedReportingDateLabel", selectedReportingDate == null ? "No reporting date selected" : selectedReportingDate.getEndDate());
        modelAndView.addObject("startDate", startDate);
        modelAndView.addObject("endDate", endDate);

        if (!applyFilters) {
            preparePage(modelAndView, request, false);
            return modelAndView;
        }

        List<Scorecard> scorecards = scorecardService.getScorecardsByReportingPeriodId(reportingPeriod);
        if (scorecards == null) {
            scorecards = new ArrayList<Scorecard>();
        }
        Map<Long, Map<Long, OverallScore>> overallScoresByDate =
                overallScoreService.getOverallScoresByScorecardsAndReportingDates(scorecards, reportingDates);
        ReportingDate insightReportingDate = selectedReportingDate;
        Map<Long, OverallScore> selectedOverallScores = selectedReportingDate == null
                ? null
                : overallScoresByDate.get(selectedReportingDate.getId());
        Map<Long, Integer> pipCountsByScorecardId = new HashMap<>();
        Map<Long, Integer> actionPlanCountsByScorecardId = new HashMap<>();
        Map<Long, ScorecardRiskProfile> riskProfilesByScorecardId = new HashMap<Long, ScorecardRiskProfile>();
        Map<Long, OverallScore> latestOverallScoresByScorecardId = new HashMap<Long, OverallScore>();
        double totalLatestWeightedScore = 0.0;
        int scorecardsWithWeightedScore = 0;
        int belowThresholdCount = 0;
        int improvingCount = 0;
        int decliningCount = 0;
        int activePipCount = 0;
        int openActionPlanCount = 0;
        int probationDecisionCount = 0;

        Account loggedUser = commonService.getLoggedUser();
        long loggedUserId = loggedUser.getId();
        String role = effectiveReviewRole(loggedUser);

        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getOwner() == null) {
                continue;
            }
            Account owner = scorecard.getOwner();
            List<PerformanceImprovementPlan> pips =
                    performanceImprovementPlanService.listPerformanceImprovementPlansByEmployee(owner, reportingPeriod);
            List<ActionPlan> actionPlans =
                    actionPlanService.listActionPlansByManagerAndReportingPeriod(owner, reportingPeriod);
            pipCountsByScorecardId.put(scorecard.getId(), pips.size());
            actionPlanCountsByScorecardId.put(scorecard.getId(), actionPlans.size());

            List<Target> targets = targetService.getAllTargetsByScorecard(scorecard.getId());
            ReportInsight insight = buildReportInsight(scorecard, reportingPeriod, targets, pips, actionPlans, selectedReportingDate);
            ScorecardRiskProfile riskProfile = buildScorecardRiskProfile(insight);
            riskProfilesByScorecardId.put(scorecard.getId(), riskProfile);
            latestOverallScoresByScorecardId.put(scorecard.getId(), resolveOverallScore(selectedOverallScores, scorecard, insightReportingDate));

            OverallScore selectedScore = selectedOverallScores == null ? null : selectedOverallScores.get(scorecard.getId());
            if (hasAnyOverallScore(selectedScore)) {
                if (riskProfile.latestWeightedScore > 0.0) {
                    totalLatestWeightedScore += riskProfile.latestWeightedScore;
                    scorecardsWithWeightedScore++;
                }
                if (riskProfile.latestWeightedScore > 0.0 && riskProfile.latestWeightedScore < 50.0) {
                    belowThresholdCount++;
                }
                if (insight.scoreMovement > 0.0) {
                    improvingCount++;
                } else if (insight.scoreMovement < 0.0) {
                    decliningCount++;
                }
                activePipCount += insight.pipOpenCount + insight.pipInProgressCount;
                openActionPlanCount += insight.actionOpenCount + insight.actionInProgressCount;
                if (insight.probationDecisionRequired) {
                    probationDecisionCount++;
                }
            }
        }

        for(ReportingDate reportingDate : reportingDates) {
            List<OverallScore> overallScores = new ArrayList<>();
            Map<Long, OverallScore> scoreByScorecard = reportingDate == null ? null : overallScoresByDate.get(reportingDate.getId());

            for(Scorecard scorecard : scorecards) {
                OverallScore overallScore = scoreByScorecard == null ? null : scoreByScorecard.get(scorecard.getId());
                if (hasAnyOverallScore(overallScore)) {
                    overallScores.add(overallScore);
                }
            }
            reportingDate.setOverallScores(overallScores);

        }

        String activeScoreFilter = normalizeScoreFilter(scoreFilter);
        List<Scorecard> scorecardsWithSelectedScores = scorecardsWithActualScores(scorecards, selectedOverallScores);
        List<Scorecard> filteredScorecards = filterScorecardsForView(scorecardsWithSelectedScores, riskProfilesByScorecardId, activeScoreFilter);
        boolean showScoreResults = !filteredScorecards.isEmpty();
        String scoreResultsMessage = resolveScoreResultsMessage(
                scorecards,
                reportingDates,
                selectedReportingDate,
                scorecardsWithSelectedScores,
                filteredScorecards
        );
        List<Long> scorecardIdsForView = new ArrayList<Long>();
        for (Scorecard scorecard : filteredScorecards) {
            if (scorecard != null) {
                scorecardIdsForView.add(scorecard.getId());
            }
        }
        modelAndView.addObject("reportingDates", reportingDates);
        modelAndView.addObject("reviewReportingPeriods", reviewReportingPeriods);
        modelAndView.addObject("reviewReportingDateOptions", buildReviewReportingDateOptions(reviewReportingPeriods));
        modelAndView.addObject("selectedReportingPeriod", reportingPeriod);
        modelAndView.addObject("selectedReportingPeriodId", reportingPeriod.getId());
        modelAndView.addObject("selectedReportingDate", selectedReportingDate);
        modelAndView.addObject("selectedReportingDateId", selectedReportingDate == null ? null : selectedReportingDate.getId());
        modelAndView.addObject("selectedReportingPeriodLabel", startDate + " to " + endDate);
        modelAndView.addObject("selectedReportingDateLabel", selectedReportingDate == null ? "No reporting date selected" : selectedReportingDate.getEndDate());
        modelAndView.addObject("scoresList", filteredScorecards);
        modelAndView.addObject("scorecardIdsForView", scorecardIdsForView);
        modelAndView.addObject("filteredScorecardCount", filteredScorecards.size());
        modelAndView.addObject("showScoreResults", showScoreResults);
        modelAndView.addObject("scoreResultsMessage", scoreResultsMessage);
        modelAndView.addObject("activeScoreFilter", activeScoreFilter);
        modelAndView.addObject("activeScoreFilterLabel", resolveScoreFilterLabel(activeScoreFilter));
        modelAndView.addObject("allScorecardsFilterUrl", buildViewScoresFilterUrl(reportingPeriod.getId(), selectedReportingDate, "all"));
        modelAndView.addObject("scoredScorecardsFilterUrl", buildViewScoresFilterUrl(reportingPeriod.getId(), selectedReportingDate, "scored"));
        modelAndView.addObject("belowThresholdFilterUrl", buildViewScoresFilterUrl(reportingPeriod.getId(), selectedReportingDate, "below-threshold"));
        modelAndView.addObject("probationDecisionFilterUrl", buildViewScoresFilterUrl(reportingPeriod.getId(), selectedReportingDate, "probation-decisions"));
        modelAndView.addObject("improvingFilterUrl", buildViewScoresFilterUrl(reportingPeriod.getId(), selectedReportingDate, "improving"));
        modelAndView.addObject("decliningFilterUrl", buildViewScoresFilterUrl(reportingPeriod.getId(), selectedReportingDate, "declining"));
        modelAndView.addObject("activePipFilterUrl", buildViewScoresFilterUrl(reportingPeriod.getId(), selectedReportingDate, "active-pips"));
        modelAndView.addObject("openActionPlanFilterUrl", buildViewScoresFilterUrl(reportingPeriod.getId(), selectedReportingDate, "open-action-plans"));
        modelAndView.addObject("pipCountsByScorecardId", pipCountsByScorecardId);
        modelAndView.addObject("actionPlanCountsByScorecardId", actionPlanCountsByScorecardId);
        modelAndView.addObject("riskProfilesByScorecardId", riskProfilesByScorecardId);
        modelAndView.addObject("latestOverallScoresByScorecardId", latestOverallScoresByScorecardId);
        modelAndView.addObject("scoreSummaryReportingDateLabel", insightReportingDate == null ? "Latest available context" : insightReportingDate.getEndDate());
        modelAndView.addObject("reportAverageScore", scorecardsWithWeightedScore == 0 ? 0.0 : roundTwoDecimals(totalLatestWeightedScore / scorecardsWithWeightedScore));
        modelAndView.addObject("belowThresholdCount", belowThresholdCount);
        modelAndView.addObject("improvingCount", improvingCount);
        modelAndView.addObject("decliningCount", decliningCount);
        modelAndView.addObject("activePipCount", activePipCount);
        modelAndView.addObject("openActionPlanCount", openActionPlanCount);
        modelAndView.addObject("probationDecisionCount", probationDecisionCount);
        modelAndView.addObject("totalScorecards", scorecards == null ? 0 : scorecards.size());
        modelAndView.addObject("loggedUserId", loggedUserId);
        modelAndView.addObject("role", role);
        modelAndView.addObject("startDate", startDate);
        modelAndView.addObject("endDate", endDate);
//        PortletUtils.addInfoMsg("Showing scores for the period: "+ startDate + " to "+ endDate, request);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/view-performance-levels", method = RequestMethod.GET)
    public ModelAndView viewPerformanceLevels(@RequestParam(value = "reportingDateId", required = false) Long reportingDateId,
                                              @RequestParam(value = "fromDate", required = false) String fromDate,
                                              @RequestParam(value = "toDate", required = false) String toDate,
                                              @RequestParam(value = "fromReportingDateId", required = false) Long fromReportingDateId,
                                              @RequestParam(value = "toReportingDateId", required = false) Long toReportingDateId,
                                              @RequestParam(value = "employeeIds", required = false) List<Long> employeeIds,
                                              HttpServletRequest request) {
        return buildPerformanceLevelsView(fromDate, toDate, fromReportingDateId, toReportingDateId, reportingDateId, null, employeeIds, request);
    }

    @RequestMapping(value = "/view-performance-levels", method = RequestMethod.POST)
    public ModelAndView viewPerformanceLevels(@RequestParam(value = "scorecards", required = false) List<Long> scorecardIds,
                                              @RequestParam(value = "reportingDateId", required = false) Long reportingDateId,
                                              @RequestParam(value = "fromDate", required = false) String fromDate,
                                              @RequestParam(value = "toDate", required = false) String toDate,
                                              @RequestParam(value = "fromReportingDateId", required = false) Long fromReportingDateId,
                                              @RequestParam(value = "toReportingDateId", required = false) Long toReportingDateId,
                                              @RequestParam(value = "employeeIds", required = false) List<Long> employeeIds,
                                              HttpServletRequest request) {
        return buildPerformanceLevelsView(fromDate, toDate, fromReportingDateId, toReportingDateId, reportingDateId, scorecardIds, employeeIds, request);
    }

    private ModelAndView buildPerformanceLevelsView(String fromDate,
                                                    String toDate,
                                                    Long fromReportingDateId,
                                                    Long toReportingDateId,
                                                    Long reportingDateId,
                                                    List<Long> scorecardIds,
                                                    List<Long> employeeIds,
                                                    HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PERFORMANCE_LEVELS);
        modelAndView.addObject("pageTitle", "View performance Levels");
        List<ReportingPeriod> reportingPeriods = reportingPeriodService.listAllReportingPeriods();
        List<ReportingDate> allReportingDates = resolveAllReportingDates(reportingPeriods);
        PerformanceLevelDateRange dateRange = resolvePerformanceLevelDateRange(
                fromDate,
                toDate,
                fromReportingDateId,
                toReportingDateId,
                reportingDateId,
                allReportingDates,
                request
        );
        List<ReportingDate> selectedReportingDates = dateRange.valid
                ? filterReportingDatesByRange(allReportingDates, dateRange.from, dateRange.to)
                : new ArrayList<ReportingDate>();

        List<Scorecard> postedScorecards = scorecardIds == null || scorecardIds.isEmpty()
                ? new ArrayList<Scorecard>()
                : scorecardService.getScorecardsByIds(scorecardIds);

        List<Long> selectedEmployeeIds = normalizeEmployeeIds(employeeIds);
        if (selectedEmployeeIds.isEmpty()) {
            selectedEmployeeIds = extractEmployeeIdsFromScorecards(postedScorecards);
        }
        boolean groupSelectionActive = !selectedEmployeeIds.isEmpty();

        List<Account> selectedEmployees = resolvePerformanceLevelEmployees(selectedEmployeeIds, selectedReportingDates);
        List<Long> formEmployeeIds = groupSelectionActive
                ? extractEmployeeIdsFromAccounts(selectedEmployees)
                : new ArrayList<Long>();

        List<Scorecard> scorecardList = resolvePerformanceLevelScorecards(selectedEmployees, selectedReportingDates);
        Map<Long, Map<Long, OverallScore>> overallScoresByDate =
                overallScoreService.getOverallScoresByScorecardsAndReportingDates(scorecardList, selectedReportingDates);
        Map<String, Scorecard> scorecardByEmployeeAndPeriod = mapScorecardsByEmployeeAndPeriod(scorecardList);

        List<Account> graphEmployees = filterEmployeesWithScorecardInRange(
                selectedEmployees,
                selectedReportingDates,
                scorecardByEmployeeAndPeriod
        );

        List<String> names = new ArrayList<>();
        for (Account employee : graphEmployees) {
            if (employee == null) {
                continue;
            }
            names.add(employee.getFullName());
        }

        List<String> dateLabels = new ArrayList<String>();
        List<List<Double>> scoreSeries = new ArrayList<List<Double>>();
        List<List<String>> colorSeries = new ArrayList<List<String>>();
        for (ReportingDate reportingDate : selectedReportingDates) {
            dateLabels.add(resolvePerformanceLevelDateLabel(reportingDate));
            List<Double> dateScores = new ArrayList<Double>();
            List<String> dateColors = new ArrayList<String>();
            Map<Long, OverallScore> scoreByScorecard = reportingDate == null ? null : overallScoresByDate.get(reportingDate.getId());
            for (Account employee : graphEmployees) {
                Scorecard scorecard = resolveEmployeeScorecardForReportingDate(scorecardByEmployeeAndPeriod, employee, reportingDate);
                if (scorecard == null) {
                    dateScores.add(null);
                    dateColors.add(resolvePerformanceLevelMissingColor());
                    continue;
                }
                OverallScore overallScore = resolveOverallScore(scoreByScorecard, scorecard, reportingDate);
                double score = overallModeratedPercent(overallScore);
                dateScores.add(score);
                dateColors.add(resolvePerformanceLevelColor(score));
            }
            scoreSeries.add(dateScores);
            colorSeries.add(dateColors);
        }

        List<String> missingScorecardDetails = resolveEmployeesWithoutScorecardsInRange(selectedEmployees, selectedReportingDates, scorecardByEmployeeAndPeriod);
        List<String> reportingPeriodLabels = resolveReportingPeriodLabels(selectedReportingDates);

        List<Double> scores = scoreSeries.isEmpty() ? new ArrayList<Double>() : scoreSeries.get(0);
        if (dateRange.valid && selectedReportingDates.isEmpty()) {
            PortletUtils.addErrorMsg("No reporting dates found in the selected range.", request);
        }

        modelAndView.addObject("names", names);
        modelAndView.addObject("scores", scores);
        modelAndView.addObject("performanceLevelDateLabels", dateLabels);
        modelAndView.addObject("performanceLevelScoreSeries", scoreSeries);
        modelAndView.addObject("performanceLevelColorSeries", colorSeries);
        modelAndView.addObject("performanceLevelDateCount", selectedReportingDates.size());
        modelAndView.addObject("performanceLevelReportingPeriodLabels", reportingPeriodLabels);
        modelAndView.addObject("missingScorecardEmployees", missingScorecardDetails);
        modelAndView.addObject("missingScorecardDetails", missingScorecardDetails);
        modelAndView.addObject("performanceLevelScorecardCount", names.size());
        modelAndView.addObject("selectedEmployeeIds", formEmployeeIds);
        modelAndView.addObject("selectedEmployeeCount", selectedEmployees.size());
        modelAndView.addObject("groupSelectionActive", groupSelectionActive);
        modelAndView.addObject("selectEmployeeGroupUrl", buildPerformanceLevelEmployeeSelectionUrl(dateRange, formEmployeeIds));
        modelAndView.addObject("performanceLevelReportingDateOptions", buildPerformanceLevelReportingDateOptions(allReportingDates));
        modelAndView.addObject("selectedFromReportingDateId", dateRange.fromReportingDateId);
        modelAndView.addObject("selectedToReportingDateId", dateRange.toReportingDateId);
        modelAndView.addObject("selectedFromDate", dateRange.fromDate);
        modelAndView.addObject("selectedToDate", dateRange.toDate);
        modelAndView.addObject("selectedDateRangeLabel", dateRange.fromDate + " to " + dateRange.toDate);
        modelAndView.addObject("selectedReportingPeriodLabel", reportingPeriodLabels.isEmpty() ? "No reporting period in range" : String.join(", ", reportingPeriodLabels));
        modelAndView.addObject("selectedReportingDateLabel", dateRange.fromDate.equals(dateRange.toDate) ? dateRange.fromDate : dateRange.fromDate + " to " + dateRange.toDate);
        modelAndView.addObject("startDate", dateRange.fromDate);
        modelAndView.addObject("endDate", dateRange.toDate);

        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/view-individual-trends", method = RequestMethod.POST)
    public ModelAndView viewIndividualTrends(@RequestParam(value = "employeeId", required = false) Long employeeId,
                                             HttpServletRequest request) {
        if (employeeId == null || employeeId <= 0) {
            PortletUtils.addValidationErrorMsg("Select an employee before viewing individual trends.", request);
            return new ModelAndView("redirect:/performance-review/view-individual-trends-select-year");
        }
        Account employee = accountService.getAccountById(employeeId);
        if (employee == null) {
            PortletUtils.addValidationErrorMsg("The selected employee could not be found.", request);
            return new ModelAndView("redirect:/performance-review/view-individual-trends-select-year");
        }
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_INDIVIDUAL_TRENDS);
        modelAndView.addObject("pageTitle", "View Individual Trends");
        ScorecardTrend trend = buildAccountTrend(employee);

        modelAndView.addObject("monthNames", trend.labels);
        modelAndView.addObject("scores", trend.scores);
        modelAndView.addObject("employee", employee);

        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/view-performance-report/{id}")
    public ModelAndView viewPerformanceReport(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PERFORMANCE_REPORT);
        modelAndView.addObject("pageTitle", "View Performance Score");

        Scorecard scoreCard = scorecardService.getScorecardById(id);
        List<Output> outputs = outputService.listAllOutputs(scoreCard);
        List<Target> targetsList = targetService.getAllTargetsByScorecard(scoreCard.getId());
        ReportingPeriod reportingPeriod = scoreCard.getReportingPeriod();
        String startDate = reportingPeriod.getStartDate();
        String endDate = reportingPeriod.getEndDate();

        Account loggedUser = commonService.getLoggedUser();
        Account owner = scoreCard.getOwner();
        List<PerformanceImprovementPlan> pips = performanceImprovementPlanService.listPerformanceImprovementPlansByEmployee(owner, reportingPeriod);
        List<ActionPlan> actionPlans = actionPlanService.listActionPlansByManagerAndReportingPeriod(owner, reportingPeriod);
        long loggedUserId = loggedUser.getId();
        String role = effectiveReviewRole(loggedUser);

        double averageModeratedScore = outcomeService.getAverageModeratorScore(id);
        double weightedScore;
        try {
            weightedScore = (averageModeratedScore / 5 ) * 100;
        }catch (Exception e){
            weightedScore = 0;
        }
        double totalWeightedScore = 0.0;
        for (Target target : targetsList) {
            if (target != null && target.getWeightedScore() != null) {
                totalWeightedScore += target.getWeightedScore();
            }
        }

        modelAndView.addObject("loggedUserId", loggedUserId);
        modelAndView.addObject("pips", pips);
        modelAndView.addObject("actionPlans", actionPlans);
        modelAndView.addObject("owner", owner);
        modelAndView.addObject("role", role);
        modelAndView.addObject("startDate", startDate);
        modelAndView.addObject("endDate", endDate);
        modelAndView.addObject("scorecard", scoreCard);
        modelAndView.addObject("outputs", outputs);
        modelAndView.addObject("targetsList", targetsList);
        modelAndView.addObject("averageModeratedScore", averageModeratedScore);
        modelAndView.addObject("weightedScore", weightedScore);
        modelAndView.addObject("totalWeightedScore", totalWeightedScore);
        modelAndView.addObject("browserReportLogoPath", browserReportLogoPath());
        populatePerformanceInsights(modelAndView, scoreCard, reportingPeriod, targetsList, pips, actionPlans);
        PortletUtils.addInfoMsg("Showing scores for the period: "+ startDate + " to "+ endDate, request);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/view-chart/{id}")
    public ModelAndView viewChart(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_CHART);
        modelAndView.addObject("pageTitle", "View Chart");
        preparePage(modelAndView, request);
        return modelAndView;
    }


    @RequestMapping("/download-report/{id}")
    public ModelAndView downloadReport(@PathVariable("id") long id, HttpServletRequest request, HttpServletResponse response) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PERFORMANCE_REPORT);
        modelAndView.addObject("pageTitle", "View Performance Score");

        Scorecard scoreCard = scorecardService.getScorecardById(id);
        List<Output> outputs = outputService.listAllOutputs(scoreCard);
        ReportingPeriod reportingPeriod = scoreCard.getReportingPeriod();
        String startDate = reportingPeriod.getStartDate();
        String endDate = reportingPeriod.getEndDate();

        Account loggedUser = cs.getLoggedUser();
        Account owner = scoreCard.getOwner();
        List<PerformanceImprovementPlan> pips = performanceImprovementPlanService.listPerformanceImprovementPlansByEmployee(owner, reportingPeriod);
        List<ActionPlan> actionPlans = actionPlanService.listActionPlansByManagerAndReportingPeriod(owner, reportingPeriod);
        long loggedUserId = loggedUser.getId();
        String role = effectiveReviewRole(loggedUser);

        double averageModeratedScore = outcomeService.getAverageModeratorScore(id);
        List<Target> targetsList = targetService.getAllTargetsByScorecard(scoreCard.getId());
        double totalWeightedScore = 0.0;
        for(Target target: targetsList){
            if (target != null && target.getWeightedScore() != null) {
                totalWeightedScore += target.getWeightedScore();
            }
        }

        modelAndView.addObject("loggedUserId", loggedUserId);
        modelAndView.addObject("pips", pips);
        modelAndView.addObject("actionPlans", actionPlans);
        modelAndView.addObject("owner", owner);
        modelAndView.addObject("role", role);
        modelAndView.addObject("startDate", startDate);
        modelAndView.addObject("endDate", endDate);
        modelAndView.addObject("scorecard", scoreCard);
        modelAndView.addObject("targetsList", targetsList);
        modelAndView.addObject("averageModeratedScore", averageModeratedScore);
        modelAndView.addObject("totalWeightedScore", totalWeightedScore);
        modelAndView.addObject("browserReportLogoPath", browserReportLogoPath());
        populatePerformanceInsights(modelAndView, scoreCard, reportingPeriod, targetsList, pips, actionPlans);
        PortletUtils.addInfoMsg("Showing scores for the period: "+ startDate + " to "+ endDate, request);
        preparePage(modelAndView, request);
        return modelAndView;
    }


//    public File generatePdf(Long id, HttpServletRequest request) throws Exception{
//        Context context = getContext(id, request);
//        String html = loadAndFillTemplate(context, request);
//        return renderPdf(html);
//    }


    private File renderPdf(String html) throws Exception {
        File file = File.createTempFile("performance-report", ".pdf");
        OutputStream outputStream = new FileOutputStream(file);
//        ITextRenderer renderer = new ITextRenderer(20f * 4f / 3f, 20);
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html, new ClassPathResource(PDF_RESOURCES).getURL().toExternalForm());
        renderer.layout();
        renderer.createPDF(outputStream);
        outputStream.close();
        file.deleteOnExit();
        return file;
    }


//    private Context getContext(Long id, HttpServletRequest request) {
//        Context context = new Context();
//        String username = PortletUtils.getUsername(request);
//        Scorecard scorecard = scorecardService.getScorecardById(id);
//        ReportingPeriod reportingPeriod = scorecard.getReportingPeriod();
//        Account loggedUser = cs.getLoggedUser();
//        Account owner = scorecard.getOwner();
//        List<PerformanceImprovementPlan> pips = performanceImprovementPlanService.listPerformanceImprovementPlansByEmployee(owner, reportingPeriod);
//        long loggedUserId = loggedUser.getId();
//        String role = loggedUser.getRole();
//
//        for(ReportingDate reportingDate: reportingPeriod.getReportingDates()){
//        }
//
//        context.setVariable("loggedUserId", loggedUserId);
//        context.setVariable("pips", pips);
//        context.setVariable("owner", owner);
//        context.setVariable("role", role);
//        context.setVariable("reportingPeriod", reportingPeriod);
//        context.setVariable("scorecard", scorecard);
//        context.setVariable("username", username);
//        return context;
//    }


    private String loadAndFillTemplate(Context context, HttpServletRequest request) {
        return templateEngine.process(Pages.DOWNLOADABLE_REPORT, context);
    }

    @GetMapping("/download-pdf/{id}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable("id") Long id, HttpServletRequest request, HttpServletResponse response) {
        try {
            Context context = new Context();
            String username = PortletUtils.getUsername(request);
            Scorecard scorecard = scorecardService.getScorecardById(id);
            ReportingPeriod reportingPeriod = scorecard.getReportingPeriod();
            Account loggedUser = cs.getLoggedUser();
            Account owner = scorecard.getOwner();
            List<PerformanceImprovementPlan> pips = performanceImprovementPlanService.listPerformanceImprovementPlansByEmployee(owner, reportingPeriod);
            List<ActionPlan> actionPlans = actionPlanService.listActionPlansByManagerAndReportingPeriod(owner, reportingPeriod);
            List<Target> targetsList = targetService.getAllTargetsByScorecard(scorecard.getId());
            List<OverallComment> overallComments = overallCommentService.getOverallCommentsByScorecard(scorecard);
            long loggedUserId = loggedUser.getId();
            String role = effectiveReviewRole(loggedUser);
            List<ReportingDate> reportingDates = reportingPeriod == null || reportingPeriod.getReportingDates() == null
                    ? Collections.emptyList()
                    : reportingPeriod.getReportingDates();
            ReportingDate finalReportingDate = resolveInsightReportingDate(reportingPeriod);

            OverallScore finalOverallScore = finalReportingDate == null
                    ? null
                    : overallScoreService.getOverallScoreByScorecardAndReportingDate(scorecard, finalReportingDate);

            context.setVariable("loggedUserId", loggedUserId);
            context.setVariable("pips", pips);
            context.setVariable("actionPlans", actionPlans);
            context.setVariable("overallComments", overallComments);
            context.setVariable("owner", owner);
            context.setVariable("role", role);
            context.setVariable("reportingPeriod", reportingPeriod);
            context.setVariable("scorecard", scorecard);
            context.setVariable("username", username);
            context.setVariable("finalReportingDate", finalReportingDate);
            context.setVariable("finalOverallScore", finalOverallScore);
            context.setVariable("reportLogoPath", pdfReportLogoPath());
            ReportInsight reportInsight = buildReportInsight(scorecard, reportingPeriod, targetsList, pips, actionPlans);
            context.setVariable("reportInsight", reportInsight);
            context.setVariable("averageEmployeeScore", reportInsight.averageEmployeeScore);
            context.setVariable("averageManagerScore", reportInsight.averageManagerScore);
            context.setVariable("averageAgreedScore", reportInsight.averageAgreedScore);
            context.setVariable("averageModeratedScore", reportInsight.averageModeratedScore);
            context.setVariable("latestWeightedScore", reportInsight.latestWeightedScore);
            context.setVariable("previousWeightedScore", reportInsight.previousWeightedScore);
            context.setVariable("scoreMovement", reportInsight.scoreMovement);
            context.setVariable("scoreMovementLabel", reportInsight.scoreMovementLabel);
            context.setVariable("performanceBand", reportInsight.performanceBand);
            context.setVariable("decisionRiskLevel", reportInsight.decisionRiskLevel);
            context.setVariable("decisionRecommendation", reportInsight.decisionRecommendation);
            context.setVariable("alignmentGapEmployeeManager", reportInsight.alignmentGapEmployeeManager);
            context.setVariable("alignmentGapManagerAgreed", reportInsight.alignmentGapManagerAgreed);
            context.setVariable("alignmentGapAgreedModerated", reportInsight.alignmentGapAgreedModerated);
            context.setVariable("totalTargets", reportInsight.totalTargets);
            context.setVariable("targetsWithScore", reportInsight.targetsWithScore);
            context.setVariable("targetsWithoutScore", reportInsight.targetsWithoutScore);
            context.setVariable("scoreCoveragePercent", reportInsight.scoreCoveragePercent);
            context.setVariable("riskRedTargets", reportInsight.riskRedTargets);
            context.setVariable("riskAmberTargets", reportInsight.riskAmberTargets);
            context.setVariable("riskGreenTargets", reportInsight.riskGreenTargets);
            context.setVariable("targetsWithEvidence", reportInsight.targetsWithEvidence);
            context.setVariable("targetsWithoutEvidence", reportInsight.targetsWithoutEvidence);
            context.setVariable("evidenceCoveragePercent", reportInsight.evidenceCoveragePercent);
            context.setVariable("insightReportingDateLabel", reportInsight.insightReportingDateLabel);
            context.setVariable("pipOpenCount", reportInsight.pipOpenCount);
            context.setVariable("pipInProgressCount", reportInsight.pipInProgressCount);
            context.setVariable("pipClosedCount", reportInsight.pipClosedCount);
            context.setVariable("actionOpenCount", reportInsight.actionOpenCount);
            context.setVariable("actionInProgressCount", reportInsight.actionInProgressCount);
            context.setVariable("actionClosedCount", reportInsight.actionClosedCount);
            context.setVariable("outstandingInterventionCount", reportInsight.outstandingInterventionCount);
            context.setVariable("topRiskItems", reportInsight.topRiskItems);
            context.setVariable("topStrengthItems", reportInsight.topStrengthItems);
            context.setVariable("lowPerformingTargets", reportInsight.lowPerformingTargets);
            context.setVariable("missingEvidenceTargets", reportInsight.missingEvidenceTargets);
            context.setVariable("highVarianceTargets", reportInsight.highVarianceTargets);
            context.setVariable("scoreTrendLabels", reportInsight.scoreTrendLabels);
            context.setVariable("scoreTrendScores", reportInsight.scoreTrendScores);
            context.setVariable("probationStatus", reportInsight.probationStatus);
            context.setVariable("probationPeriod", reportInsight.probationPeriod);
            context.setVariable("probationCurrentStep", reportInsight.probationCurrentStep);
            context.setVariable("probationEndDate", reportInsight.probationEndDate);
            context.setVariable("probationRecommendation", reportInsight.probationRecommendation);
            context.setVariable("probationKpiCount", reportInsight.probationKpiCount);
            context.setVariable("probationFlaggedKpiCount", reportInsight.probationFlaggedKpiCount);
            context.setVariable("probationAverageProgress", reportInsight.probationAverageProgress);
            context.setVariable("probationDecisionRequired", reportInsight.probationDecisionRequired);

            String fileName = "Performance Report - " + owner.getFullName().trim() + ".pdf";
            String page = Pages.DOWNLOADABLE_REPORT;
            try {
                byte[] pdfBytes = pdfGeneratorService.generatePdfFromTemplate(page, context, false);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDisposition(ContentDisposition.builder("inline")
                        .filename(fileName)
                        .build());
                headers.setContentLength(pdfBytes.length);

                ByteArrayResource resource = new ByteArrayResource(pdfBytes);

                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(pdfBytes.length)
                        .body(resource);
            } catch (Exception e) {
                log.error("Error generating PDF for assessment", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }catch (Exception e){
            log.error("Error in assessment download endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void populatePerformanceInsights(ModelAndView modelAndView,
                                             Scorecard scorecard,
                                             ReportingPeriod reportingPeriod,
                                             List<Target> targetsList,
                                             List<PerformanceImprovementPlan> pips,
                                             List<ActionPlan> actionPlans) {
        ReportInsight reportInsight = buildReportInsight(scorecard, reportingPeriod, targetsList, pips, actionPlans);
        modelAndView.addObject("reportInsight", reportInsight);
        modelAndView.addObject("averageEmployeeScore", reportInsight.averageEmployeeScore);
        modelAndView.addObject("averageManagerScore", reportInsight.averageManagerScore);
        modelAndView.addObject("averageAgreedScore", reportInsight.averageAgreedScore);
        modelAndView.addObject("averageModeratedScore", reportInsight.averageModeratedScore);
        modelAndView.addObject("latestWeightedScore", reportInsight.latestWeightedScore);
        modelAndView.addObject("previousWeightedScore", reportInsight.previousWeightedScore);
        modelAndView.addObject("scoreMovement", reportInsight.scoreMovement);
        modelAndView.addObject("scoreMovementLabel", reportInsight.scoreMovementLabel);
        modelAndView.addObject("performanceBand", reportInsight.performanceBand);
        modelAndView.addObject("decisionRiskLevel", reportInsight.decisionRiskLevel);
        modelAndView.addObject("decisionRecommendation", reportInsight.decisionRecommendation);
        modelAndView.addObject("alignmentGapEmployeeManager", reportInsight.alignmentGapEmployeeManager);
        modelAndView.addObject("alignmentGapManagerAgreed", reportInsight.alignmentGapManagerAgreed);
        modelAndView.addObject("alignmentGapAgreedModerated", reportInsight.alignmentGapAgreedModerated);
        modelAndView.addObject("totalTargets", reportInsight.totalTargets);
        modelAndView.addObject("targetsWithScore", reportInsight.targetsWithScore);
        modelAndView.addObject("targetsWithoutScore", reportInsight.targetsWithoutScore);
        modelAndView.addObject("scoreCoveragePercent", reportInsight.scoreCoveragePercent);
        modelAndView.addObject("riskRedTargets", reportInsight.riskRedTargets);
        modelAndView.addObject("riskAmberTargets", reportInsight.riskAmberTargets);
        modelAndView.addObject("riskGreenTargets", reportInsight.riskGreenTargets);
        modelAndView.addObject("targetsWithEvidence", reportInsight.targetsWithEvidence);
        modelAndView.addObject("targetsWithoutEvidence", reportInsight.targetsWithoutEvidence);
        modelAndView.addObject("evidenceCoveragePercent", reportInsight.evidenceCoveragePercent);
        modelAndView.addObject("insightReportingDateLabel", reportInsight.insightReportingDateLabel);
        modelAndView.addObject("pipOpenCount", reportInsight.pipOpenCount);
        modelAndView.addObject("pipInProgressCount", reportInsight.pipInProgressCount);
        modelAndView.addObject("pipClosedCount", reportInsight.pipClosedCount);
        modelAndView.addObject("actionOpenCount", reportInsight.actionOpenCount);
        modelAndView.addObject("actionInProgressCount", reportInsight.actionInProgressCount);
        modelAndView.addObject("actionClosedCount", reportInsight.actionClosedCount);
        modelAndView.addObject("outstandingInterventionCount", reportInsight.outstandingInterventionCount);
        modelAndView.addObject("topRiskItems", reportInsight.topRiskItems);
        modelAndView.addObject("topStrengthItems", reportInsight.topStrengthItems);
        modelAndView.addObject("lowPerformingTargets", reportInsight.lowPerformingTargets);
        modelAndView.addObject("missingEvidenceTargets", reportInsight.missingEvidenceTargets);
        modelAndView.addObject("highVarianceTargets", reportInsight.highVarianceTargets);
        modelAndView.addObject("scoreTrendLabels", reportInsight.scoreTrendLabels);
        modelAndView.addObject("scoreTrendScores", reportInsight.scoreTrendScores);
        modelAndView.addObject("probationStatus", reportInsight.probationStatus);
        modelAndView.addObject("probationPeriod", reportInsight.probationPeriod);
        modelAndView.addObject("probationCurrentStep", reportInsight.probationCurrentStep);
        modelAndView.addObject("probationEndDate", reportInsight.probationEndDate);
        modelAndView.addObject("probationRecommendation", reportInsight.probationRecommendation);
        modelAndView.addObject("probationKpiCount", reportInsight.probationKpiCount);
        modelAndView.addObject("probationFlaggedKpiCount", reportInsight.probationFlaggedKpiCount);
        modelAndView.addObject("probationAverageProgress", reportInsight.probationAverageProgress);
        modelAndView.addObject("probationDecisionRequired", reportInsight.probationDecisionRequired);
    }

    private String browserReportLogoPath() {
        return reportLogoPath("/img/");
    }

    private String pdfReportLogoPath() {
        return reportLogoPath("img/");
    }

    private String reportLogoPath(String imgPrefix) {
        String logo = systemSettingService == null ? null : systemSettingService.getCompanyLogo();
        if (!hasText(logo)) {
            logo = "zimlogo.png";
        }
        logo = logo.trim().replace("\\", "/");
        if (logo.startsWith("http://") || logo.startsWith("https://") || logo.startsWith("data:")) {
            return logo;
        }
        while (logo.startsWith("/")) {
            logo = logo.substring(1);
        }
        if (logo.startsWith("static/")) {
            logo = logo.substring("static/".length());
        }
        if (logo.startsWith("img/")) {
            logo = logo.substring("img/".length());
        }
        return imgPrefix + logo;
    }

    private double overallModeratedPercent(OverallScore overallScore) {
        if (overallScore == null || overallScore.getModeratedOverall() == null || overallScore.getModeratedOverall() <= 0.0) {
            return 0.0;
        }
        return roundTwoDecimals((overallScore.getModeratedOverall() / 5.0) * 100.0);
    }

    private ReportInsight buildReportInsight(Scorecard scorecard,
                                             ReportingPeriod reportingPeriod,
                                             List<Target> targetsList,
                                             List<PerformanceImprovementPlan> pips,
                                             List<ActionPlan> actionPlans) {
        return buildReportInsight(scorecard, reportingPeriod, targetsList, pips, actionPlans, null);
    }

    private ReportInsight buildReportInsight(Scorecard scorecard,
                                             ReportingPeriod reportingPeriod,
                                             List<Target> targetsList,
                                             List<PerformanceImprovementPlan> pips,
                                             List<ActionPlan> actionPlans,
                                             ReportingDate selectedReportingDate) {
        ReportInsight insight = new ReportInsight();
        if (scorecard == null) {
            return insight;
        }

        List<Target> safeTargets = targetsList == null ? Collections.emptyList() : targetsList;
        ReportingDate insightReportingDate = selectedReportingDate == null
                ? resolveInsightReportingDate(reportingPeriod)
                : selectedReportingDate;
        insight.insightReportingDateLabel = insightReportingDate == null || insightReportingDate.getEndDate() == null
                ? "Latest available context"
                : insightReportingDate.getEndDate();

        applyReportInsightAverages(insight, scorecard, safeTargets, insightReportingDate);
        insight.alignmentGapEmployeeManager = roundTwoDecimals(Math.abs(insight.averageEmployeeScore - insight.averageManagerScore));
        insight.alignmentGapManagerAgreed = roundTwoDecimals(Math.abs(insight.averageManagerScore - insight.averageAgreedScore));
        insight.alignmentGapAgreedModerated = roundTwoDecimals(Math.abs(insight.averageAgreedScore - insight.averageModeratedScore));

        ScorecardTrend trend = buildScorecardTrend(scorecard, reportingPeriod, insightReportingDate);
        insight.scoreTrendLabels = trend.labels;
        insight.scoreTrendScores = trend.scores;
        insight.latestWeightedScore = trend.latestScore > 0.0
                ? trend.latestScore
                : roundTwoDecimals((insight.averageModeratedScore / 5.0) * 100.0);
        insight.previousWeightedScore = trend.previousScore;
        insight.scoreMovement = trend.movement;
        insight.scoreMovementLabel = trend.movementLabel;
        insight.performanceBand = resolvePerformanceBand(insight.latestWeightedScore);

        insight.totalTargets = safeTargets.size();

        for (Target target : safeTargets) {
            if (target == null) {
                continue;
            }
            Score score = insightReportingDate == null ? resolveLatestInsightScore(target) : resolveInsightScore(target, insightReportingDate);
            double scoreValue = resolveInsightScoreValue(score);
            if (scoreValue <= 0.0 && insightReportingDate == null) {
                scoreValue = resolveTargetInsightScore(target);
            }
            if (scoreValue > 0.0) {
                insight.targetsWithScore++;
                if (scoreValue < 2.5) {
                    insight.riskRedTargets++;
                    addLimited(insight.lowPerformingTargets, describeTargetScore(target, scoreValue), 8);
                    addLimited(insight.topRiskItems, "Low score: " + describeTargetScore(target, scoreValue), 5);
                } else if (scoreValue < 3.5) {
                    insight.riskAmberTargets++;
                    addLimited(insight.topRiskItems, "Watch target: " + describeTargetScore(target, scoreValue), 5);
                } else {
                    insight.riskGreenTargets++;
                    addLimited(insight.topStrengthItems, describeTargetScore(target, scoreValue), 5);
                }
            } else {
                addLimited(insight.topRiskItems, "No score captured: " + targetLabel(target), 5);
            }

            Evidence evidence = insightReportingDate == null
                    ? resolveLatestEvidence(target)
                    : resolveLatestEvidence(target, insightReportingDate);
            boolean hasEvidence = hasEvidence(evidence) || hasEvidence(score);
            if (hasEvidence) {
                insight.targetsWithEvidence++;
            } else {
                addLimited(insight.missingEvidenceTargets, targetLabel(target), 8);
                addLimited(insight.topRiskItems, "Missing evidence: " + targetLabel(target), 5);
            }

            double variance = score == null ? 0.0 : highestScoreVariance(score);
            if (variance >= 1.0) {
                addLimited(insight.highVarianceTargets, targetLabel(target) + " (gap " + roundTwoDecimals(variance) + ")", 8);
                addLimited(insight.topRiskItems, "Rating alignment gap: " + targetLabel(target), 5);
            }
        }

        insight.targetsWithoutScore = Math.max(0, insight.totalTargets - insight.targetsWithScore);
        insight.targetsWithoutEvidence = Math.max(0, insight.totalTargets - insight.targetsWithEvidence);
        insight.scoreCoveragePercent = percentage(insight.targetsWithScore, insight.totalTargets);
        insight.evidenceCoveragePercent = percentage(insight.targetsWithEvidence, insight.totalTargets);

        List<PerformanceImprovementPlan> safePips = pips == null ? Collections.emptyList() : pips;
        for (PerformanceImprovementPlan pip : safePips) {
            if (pip == null) {
                continue;
            }
            int statusCategory = classifyProgressStatus(pip.getStatus(), pip.getProgress());
            if (statusCategory == 2) {
                insight.pipClosedCount++;
            } else if (statusCategory == 1) {
                insight.pipInProgressCount++;
            } else {
                insight.pipOpenCount++;
            }
        }

        List<ActionPlan> safeActionPlans = actionPlans == null ? Collections.emptyList() : actionPlans;
        for (ActionPlan actionPlan : safeActionPlans) {
            if (actionPlan == null) {
                continue;
            }
            int statusCategory = classifyProgressStatus(actionPlan.getStatus(), actionPlan.getProgress());
            if (statusCategory == 2) {
                insight.actionClosedCount++;
            } else if (statusCategory == 1) {
                insight.actionInProgressCount++;
            } else {
                insight.actionOpenCount++;
            }
        }
        insight.outstandingInterventionCount = insight.pipOpenCount
                + insight.pipInProgressCount
                + insight.actionOpenCount
                + insight.actionInProgressCount;
        applyProbationSnapshot(insight, scorecard.getOwner());
        finalizeDecisionSummary(insight);
        return insight;
    }

    private ScorecardRiskProfile buildScorecardRiskProfile(ReportInsight insight) {
        ScorecardRiskProfile profile = new ScorecardRiskProfile();
        if (insight == null) {
            return profile;
        }
        profile.latestWeightedScore = insight.latestWeightedScore;
        profile.previousWeightedScore = insight.previousWeightedScore;
        profile.scoreMovement = insight.scoreMovement;
        profile.scoreMovementLabel = insight.scoreMovementLabel;
        profile.performanceBand = insight.performanceBand;
        profile.decisionRiskLevel = insight.decisionRiskLevel;
        profile.decisionRecommendation = insight.decisionRecommendation;
        profile.openInterventionCount = insight.outstandingInterventionCount;
        profile.activePipCount = insight.pipOpenCount + insight.pipInProgressCount;
        profile.openActionPlanCount = insight.actionOpenCount + insight.actionInProgressCount;
        profile.probationStatus = insight.probationStatus;
        profile.probationDecisionRequired = insight.probationDecisionRequired;
        profile.exceptionCount = insight.riskRedTargets
                + insight.targetsWithoutScore
                + insight.targetsWithoutEvidence
                + insight.highVarianceTargets.size();
        return profile;
    }

    private List<Scorecard> filterScorecardsForView(List<Scorecard> scorecards,
                                                    Map<Long, ScorecardRiskProfile> riskProfilesByScorecardId,
                                                    String scoreFilter) {
        List<Scorecard> filteredScorecards = new ArrayList<Scorecard>();
        if (scorecards == null) {
            return filteredScorecards;
        }
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null) {
                continue;
            }
            ScorecardRiskProfile profile = riskProfilesByScorecardId == null
                    ? null
                    : riskProfilesByScorecardId.get(scorecard.getId());
            if (matchesScoreFilter(profile, scoreFilter)) {
                filteredScorecards.add(scorecard);
            }
        }
        return filteredScorecards;
    }

    private List<Scorecard> scorecardsWithActualScores(List<Scorecard> scorecards,
                                                       Map<Long, OverallScore> selectedOverallScores) {
        List<Scorecard> scoredScorecards = new ArrayList<Scorecard>();
        if (scorecards == null || selectedOverallScores == null || selectedOverallScores.isEmpty()) {
            return scoredScorecards;
        }
        for (Scorecard scorecard : scorecards) {
            if (scorecard != null && hasAnyOverallScore(selectedOverallScores.get(scorecard.getId()))) {
                scoredScorecards.add(scorecard);
            }
        }
        return scoredScorecards;
    }

    private String resolveScoreResultsMessage(List<Scorecard> scorecards,
                                              List<ReportingDate> reportingDates,
                                              ReportingDate selectedReportingDate,
                                              List<Scorecard> scorecardsWithSelectedScores,
                                              List<Scorecard> filteredScorecards) {
        if (scorecards == null || scorecards.isEmpty()) {
            return "No scorecards exist for this reporting period.";
        }
        if (reportingDates == null || reportingDates.isEmpty() || selectedReportingDate == null) {
            return "No reporting dates are configured for this reporting period.";
        }
        if (scorecardsWithSelectedScores == null || scorecardsWithSelectedScores.isEmpty()) {
            return "Scorecards are yet to be scored for the selected reporting date.";
        }
        if (filteredScorecards == null || filteredScorecards.isEmpty()) {
            return "No scored scorecards match the selected filter.";
        }
        return "";
    }

    private boolean matchesScoreFilter(ScorecardRiskProfile profile, String scoreFilter) {
        String normalizedFilter = normalizeScoreFilter(scoreFilter);
        if ("all".equals(normalizedFilter)) {
            return true;
        }
        if (profile == null) {
            return false;
        }
        if ("scored".equals(normalizedFilter)) {
            return profile.latestWeightedScore > 0.0;
        }
        if ("below-threshold".equals(normalizedFilter)) {
            return profile.latestWeightedScore > 0.0 && profile.latestWeightedScore < 50.0;
        }
        if ("probation-decisions".equals(normalizedFilter)) {
            return profile.probationDecisionRequired;
        }
        if ("improving".equals(normalizedFilter)) {
            return profile.scoreMovement > 0.0;
        }
        if ("declining".equals(normalizedFilter)) {
            return profile.scoreMovement < 0.0;
        }
        if ("active-pips".equals(normalizedFilter)) {
            return profile.activePipCount > 0;
        }
        if ("open-action-plans".equals(normalizedFilter)) {
            return profile.openActionPlanCount > 0;
        }
        return true;
    }

    private String normalizeScoreFilter(String scoreFilter) {
        if (!hasText(scoreFilter)) {
            return "all";
        }
        String normalizedFilter = scoreFilter.trim().toLowerCase();
        if ("scored".equals(normalizedFilter)
                || "below-threshold".equals(normalizedFilter)
                || "probation-decisions".equals(normalizedFilter)
                || "improving".equals(normalizedFilter)
                || "declining".equals(normalizedFilter)
                || "active-pips".equals(normalizedFilter)
                || "open-action-plans".equals(normalizedFilter)) {
            return normalizedFilter;
        }
        return "all";
    }

    private String resolveScoreFilterLabel(String scoreFilter) {
        String normalizedFilter = normalizeScoreFilter(scoreFilter);
        if ("scored".equals(normalizedFilter)) {
            return "Scorecards With Final Scores";
        }
        if ("below-threshold".equals(normalizedFilter)) {
            return "Below Threshold";
        }
        if ("probation-decisions".equals(normalizedFilter)) {
            return "Probation Decisions";
        }
        if ("improving".equals(normalizedFilter)) {
            return "Improving";
        }
        if ("declining".equals(normalizedFilter)) {
            return "Declining";
        }
        if ("active-pips".equals(normalizedFilter)) {
            return "Active PIPs";
        }
        if ("open-action-plans".equals(normalizedFilter)) {
            return "Open Action Plans";
        }
        return "All Scorecards";
    }

    private String buildViewScoresFilterUrl(long reportingPeriodId, ReportingDate reportingDate, String scoreFilter) {
        StringBuilder url = new StringBuilder("/performance-review/view-scores/");
        url.append(reportingPeriodId);
        url.append("?applyFilters=true");
        String separator = "&";
        if (reportingDate != null && reportingDate.getId() > 0) {
            url.append(separator).append("reportingDateId=").append(reportingDate.getId());
            separator = "&";
        }
        String normalizedFilter = normalizeScoreFilter(scoreFilter);
        if (!"all".equals(normalizedFilter)) {
            url.append(separator).append("scoreFilter=").append(normalizedFilter);
        }
        return url.toString();
    }

    private ScorecardTrend buildScorecardTrend(Scorecard scorecard, ReportingPeriod reportingPeriod) {
        return buildScorecardTrend(scorecard, reportingPeriod, null);
    }

    private ScorecardTrend buildScorecardTrend(Scorecard scorecard, ReportingPeriod reportingPeriod, ReportingDate selectedReportingDate) {
        ScorecardTrend trend = new ScorecardTrend();
        if (scorecard == null || reportingPeriod == null) {
            return trend;
        }

        List<ScorecardTrendPoint> trendPoints = buildScorecardTrendPoints(scorecard, reportingPeriod, selectedReportingDate);
        if (trendPoints.isEmpty()) {
            return trend;
        }

        List<ReportingDate> reportingDates = new ArrayList<ReportingDate>();
        List<Scorecard> trendScorecards = new ArrayList<Scorecard>();
        for (ScorecardTrendPoint point : trendPoints) {
            addUniqueReportingDate(reportingDates, point.reportingDate);
            addUniqueScorecard(trendScorecards, point.scorecard);
        }

        Map<Long, Map<Long, Double>> scoresByDate;
        try {
            scoresByDate = scorecardService.getScoresByReportingDatesAndScorecardIds(
                    reportingDates,
                    trendScorecards
            );
        } catch (Exception exception) {
            scoresByDate = Collections.emptyMap();
        }

        Double previousNonZeroScore = null;
        Double latestNonZeroScore = null;
        boolean selectedDateRequested = selectedReportingDate != null && selectedReportingDate.getId() > 0;
        boolean selectedDateFound = false;
        for (ScorecardTrendPoint point : trendPoints) {
            ReportingDate reportingDate = point.reportingDate;
            if (reportingDate == null || reportingDate.getId() <= 0) {
                continue;
            }
            Double score = null;
            Map<Long, Double> scoreByScorecard = scoresByDate.get(reportingDate.getId());
            if (scoreByScorecard != null && point.scorecard != null) {
                score = scoreByScorecard.get(point.scorecard.getId());
            }
            double safeValue = score == null ? 0.0 : roundTwoDecimals(score);
            trend.labels.add(resolveTrendPointLabel(point));
            trend.scores.add(safeValue);
            if (selectedDateRequested
                    && point.scorecard != null
                    && point.scorecard.getId() == scorecard.getId()
                    && reportingDate.getId() == selectedReportingDate.getId()) {
                trend.latestScore = safeValue;
                trend.previousScore = previousNonZeroScore == null ? 0.0 : previousNonZeroScore;
                trend.movement = roundTwoDecimals(trend.latestScore - trend.previousScore);
                trend.movementLabel = trend.latestScore <= 0.0
                        ? "No score captured"
                        : resolveMovementLabel(trend.movement, trend.previousScore);
                selectedDateFound = true;
                break;
            }
            if (safeValue > 0.0) {
                previousNonZeroScore = latestNonZeroScore;
                latestNonZeroScore = safeValue;
            }
        }

        if (selectedDateRequested && selectedDateFound) {
            return trend;
        }
        trend.latestScore = latestNonZeroScore == null ? 0.0 : latestNonZeroScore;
        trend.previousScore = previousNonZeroScore == null ? 0.0 : previousNonZeroScore;
        trend.movement = roundTwoDecimals(trend.latestScore - trend.previousScore);
        trend.movementLabel = resolveMovementLabel(trend.movement, trend.previousScore);
        return trend;
    }

    private ScorecardTrend buildAccountTrend(Account owner) {
        ScorecardTrend trend = new ScorecardTrend();
        List<ScorecardTrendPoint> trendPoints = buildAccountTrendPoints(owner);
        if (trendPoints.isEmpty()) {
            return trend;
        }

        List<ReportingDate> reportingDates = new ArrayList<ReportingDate>();
        List<Scorecard> trendScorecards = new ArrayList<Scorecard>();
        for (ScorecardTrendPoint point : trendPoints) {
            addUniqueReportingDate(reportingDates, point.reportingDate);
            addUniqueScorecard(trendScorecards, point.scorecard);
        }

        Map<Long, Map<Long, Double>> scoresByDate;
        try {
            scoresByDate = scorecardService.getScoresByReportingDatesAndScorecardIds(
                    reportingDates,
                    trendScorecards
            );
        } catch (Exception exception) {
            scoresByDate = Collections.emptyMap();
        }

        Double previousNonZeroScore = null;
        Double latestNonZeroScore = null;
        for (ScorecardTrendPoint point : trendPoints) {
            ReportingDate reportingDate = point.reportingDate;
            if (reportingDate == null || reportingDate.getId() <= 0) {
                continue;
            }
            Double score = null;
            Map<Long, Double> scoreByScorecard = scoresByDate.get(reportingDate.getId());
            if (scoreByScorecard != null && point.scorecard != null) {
                score = scoreByScorecard.get(point.scorecard.getId());
            }
            double safeValue = score == null ? 0.0 : roundTwoDecimals(score);
            trend.labels.add(resolveTrendPointLabel(point));
            trend.scores.add(safeValue);
            if (safeValue > 0.0) {
                previousNonZeroScore = latestNonZeroScore;
                latestNonZeroScore = safeValue;
            }
        }

        trend.latestScore = latestNonZeroScore == null ? 0.0 : latestNonZeroScore;
        trend.previousScore = previousNonZeroScore == null ? 0.0 : previousNonZeroScore;
        trend.movement = roundTwoDecimals(trend.latestScore - trend.previousScore);
        trend.movementLabel = resolveMovementLabel(trend.movement, trend.previousScore);
        return trend;
    }

    private List<ScorecardTrendPoint> buildAccountTrendPoints(Account owner) {
        List<ScorecardTrendPoint> points = new ArrayList<ScorecardTrendPoint>();
        if (owner == null) {
            return points;
        }

        List<Scorecard> ownerScorecards = scorecardService.getScorecardsByOwner(owner);
        if (ownerScorecards == null) {
            return points;
        }

        for (Scorecard scorecard : ownerScorecards) {
            if (scorecard == null || scorecard.getReportingPeriod() == null) {
                continue;
            }
            ReportingPeriod reportingPeriod = scorecard.getReportingPeriod();
            List<ReportingDate> reportingDates = sortReportingDates(reportingDateService.listAllReportingDates(reportingPeriod));
            for (ReportingDate reportingDate : reportingDates) {
                if (reportingDate == null || reportingDate.getId() <= 0) {
                    continue;
                }
                points.add(new ScorecardTrendPoint(scorecard, reportingPeriod, reportingDate));
            }
        }

        points.sort(Comparator
                .comparing((ScorecardTrendPoint point) -> reportingPeriodSortKey(point.reportingPeriod))
                .thenComparing(point -> sortDateKey(point.reportingDate))
                .thenComparingLong(point -> point.reportingDate == null ? 0L : point.reportingDate.getId())
                .thenComparingLong(point -> point.scorecard == null ? 0L : point.scorecard.getId()));
        return points;
    }

    private List<ScorecardTrendPoint> buildScorecardTrendPoints(Scorecard currentScorecard,
                                                                 ReportingPeriod currentReportingPeriod,
                                                                 ReportingDate selectedReportingDate) {
        List<ScorecardTrendPoint> points = new ArrayList<ScorecardTrendPoint>();
        if (currentScorecard == null || currentReportingPeriod == null) {
            return points;
        }

        List<Scorecard> ownerScorecards = currentScorecard.getOwner() == null
                ? new ArrayList<Scorecard>()
                : scorecardService.getScorecardsByOwner(currentScorecard.getOwner());
        ownerScorecards = ownerScorecards == null
                ? new ArrayList<Scorecard>()
                : new ArrayList<Scorecard>(ownerScorecards);
        addUniqueScorecard(ownerScorecards, currentScorecard);

        for (Scorecard candidate : ownerScorecards) {
            if (candidate == null || candidate.getReportingPeriod() == null) {
                continue;
            }
            ReportingPeriod candidatePeriod = candidate.getReportingPeriod();
            boolean samePeriod = candidatePeriod.getId() == currentReportingPeriod.getId();
            if (samePeriod && candidate.getId() != currentScorecard.getId()) {
                continue;
            }
            if (!samePeriod && !isReportingPeriodBefore(candidatePeriod, currentReportingPeriod)) {
                continue;
            }

            List<ReportingDate> candidateDates = sortReportingDates(reportingDateService.listAllReportingDates(candidatePeriod));
            for (ReportingDate reportingDate : candidateDates) {
                if (reportingDate == null || reportingDate.getId() <= 0) {
                    continue;
                }
                if (samePeriod && isReportingDateAfterSelection(reportingDate, selectedReportingDate)) {
                    continue;
                }
                points.add(new ScorecardTrendPoint(candidate, candidatePeriod, reportingDate));
            }
        }

        points.sort(Comparator
                .comparing((ScorecardTrendPoint point) -> reportingPeriodSortKey(point.reportingPeriod))
                .thenComparing(point -> sortDateKey(point.reportingDate))
                .thenComparingLong(point -> point.reportingDate == null ? 0L : point.reportingDate.getId())
                .thenComparingLong(point -> point.scorecard == null ? 0L : point.scorecard.getId()));
        return points;
    }

    private boolean isReportingPeriodBefore(ReportingPeriod candidate, ReportingPeriod current) {
        if (candidate == null || current == null) {
            return false;
        }
        LocalDate candidateDate = reportingPeriodSortKey(candidate);
        LocalDate currentDate = reportingPeriodSortKey(current);
        if (!LocalDate.MIN.equals(candidateDate) && !LocalDate.MIN.equals(currentDate)) {
            return candidateDate.isBefore(currentDate);
        }
        return candidate.getId() < current.getId();
    }

    private boolean isReportingDateAfterSelection(ReportingDate reportingDate, ReportingDate selectedReportingDate) {
        if (reportingDate == null || selectedReportingDate == null || selectedReportingDate.getId() <= 0) {
            return false;
        }
        LocalDate reportingDateKey = sortDateKey(reportingDate);
        LocalDate selectedDateKey = sortDateKey(selectedReportingDate);
        if (!LocalDate.MIN.equals(reportingDateKey) && !LocalDate.MIN.equals(selectedDateKey)) {
            return reportingDateKey.isAfter(selectedDateKey);
        }
        return reportingDate.getId() > selectedReportingDate.getId();
    }

    private void addUniqueReportingDate(List<ReportingDate> reportingDates, ReportingDate reportingDate) {
        if (reportingDates == null || reportingDate == null || reportingDate.getId() <= 0) {
            return;
        }
        for (ReportingDate existing : reportingDates) {
            if (existing != null && existing.getId() == reportingDate.getId()) {
                return;
            }
        }
        reportingDates.add(reportingDate);
    }

    private void addUniqueScorecard(List<Scorecard> scorecards, Scorecard scorecard) {
        if (scorecards == null || scorecard == null || scorecard.getId() <= 0) {
            return;
        }
        for (Scorecard existing : scorecards) {
            if (existing != null && existing.getId() == scorecard.getId()) {
                return;
            }
        }
        scorecards.add(scorecard);
    }

    private LocalDate reportingPeriodSortKey(ReportingPeriod reportingPeriod) {
        if (reportingPeriod == null) {
            return LocalDate.MIN;
        }
        LocalDate endDate = parseLocalDate(reportingPeriod.getEndDate());
        if (endDate != null) {
            return endDate;
        }
        LocalDate startDate = parseLocalDate(reportingPeriod.getStartDate());
        return startDate == null ? LocalDate.MIN : startDate;
    }

    private String resolveTrendPointLabel(ScorecardTrendPoint point) {
        if (point == null) {
            return "N/A";
        }
        return resolveReportingDateLabel(point.reportingDate);
    }

    private LocalDate sortDateKey(ReportingDate reportingDate) {
        LocalDate parsedDate = reportingDate == null ? null : parseLocalDate(reportingDate.getEndDate());
        return parsedDate == null ? LocalDate.MIN : parsedDate;
    }

    private String resolveReportingDateLabel(ReportingDate reportingDate) {
        if (reportingDate != null && hasText(reportingDate.getEndDate())) {
            return reportingDate.getEndDate();
        }
        return "N/A";
    }

    private String resolveMovementLabel(double movement, double previousScore) {
        if (previousScore <= 0.0) {
            return "No previous score";
        }
        if (movement > 0.0) {
            return "Improved by " + movement + "%";
        }
        if (movement < 0.0) {
            return "Declined by " + Math.abs(movement) + "%";
        }
        return "No movement";
    }

    private String resolvePerformanceBand(double weightedScore) {
        if (weightedScore >= 80.0) {
            return "Exceeds expectations";
        }
        if (weightedScore >= 70.0) {
            return "Strong performance";
        }
        if (weightedScore >= 50.0) {
            return "Meets expectations";
        }
        if (weightedScore >= 40.0) {
            return "Needs improvement";
        }
        if (weightedScore > 0.0) {
            return "Critical performance risk";
        }
        return "Not fully assessed";
    }

    private void applyProbationSnapshot(ReportInsight insight, Account owner) {
        if (insight == null || owner == null) {
            return;
        }
        List<ProbationAssessment> assessments = probationAssessmentRepository.findProbationAssessmentsByEmployeeOrderByDateDesc(owner);
        if (assessments == null || assessments.isEmpty()) {
            insight.probationStatus = "No probation contract";
            insight.probationRecommendation = "No probation decision required from available records.";
            return;
        }

        ProbationAssessment latest = assessments.get(0);
        insight.probationStatus = blankToDefault(latest.getStatus(), "Draft");
        insight.probationPeriod = blankToDefault(latest.getPerformancePeriod(), latest.getStartDate() + " to " + latest.getEndDate());
        insight.probationCurrentStep = blankToDefault(latest.getCurrentStepName(), "Not submitted");
        insight.probationEndDate = blankToDefault(latest.getEndDate(), "N/A");

        List<ProbationKpi> kpis = probationKpiRepository.findProbationKpisByAssessmentOrderByIdAsc(latest);
        insight.probationKpiCount = kpis == null ? 0 : kpis.size();
        double progressTotal = 0.0;
        int progressCount = 0;
        double supervisorMarkTotal = 0.0;
        int supervisorMarkCount = 0;
        if (kpis != null) {
            for (ProbationKpi kpi : kpis) {
                if (kpi == null) {
                    continue;
                }
                if (hasText(kpi.getFlag())) {
                    insight.probationFlaggedKpiCount++;
                }
                if (kpi.getProgressPercent() != null) {
                    progressTotal += kpi.getProgressPercent();
                    progressCount++;
                }
                if (kpi.getSupervisorMark() != null) {
                    supervisorMarkTotal += kpi.getSupervisorMark();
                    supervisorMarkCount++;
                }
            }
        }
        insight.probationAverageProgress = progressCount == 0 ? 0.0 : roundTwoDecimals(progressTotal / progressCount);
        double averageSupervisorMark = supervisorMarkCount == 0 ? 0.0 : roundTwoDecimals(supervisorMarkTotal / supervisorMarkCount);
        boolean probationClosed = isClosedStatus(insight.probationStatus);
        boolean probationDue = isPastDate(latest.getEndDate());
        boolean weakProgress = progressCount > 0 && insight.probationAverageProgress < 50.0;
        boolean weakSupervisorResult = supervisorMarkCount > 0 && averageSupervisorMark < 2.5;
        insight.probationDecisionRequired = !probationClosed
                && (probationDue || insight.probationFlaggedKpiCount > 0 || weakProgress || weakSupervisorResult);
        if (insight.probationDecisionRequired) {
            insight.probationRecommendation = "Review probation before confirmation; the contract is due or KPI progress, flags, or supervisor marks require attention.";
        } else if (probationClosed) {
            insight.probationRecommendation = supervisorMarkCount == 0
                    ? "Probation evaluation is completed; no supervisor marks are available in the latest record."
                    : "Probation evaluation is completed. Supervisor result: " + averageSupervisorMark + "/5 - "
                    + resolveProbationPerformanceBand(averageSupervisorMark) + ".";
        } else if (supervisorMarkCount > 0) {
            insight.probationRecommendation = "Current supervisor result: " + averageSupervisorMark + "/5 - "
                    + resolveProbationPerformanceBand(averageSupervisorMark) + ". Continue the probation workflow.";
        } else {
            insight.probationRecommendation = "Continue monitoring probation contract progress.";
        }
    }

    private void finalizeDecisionSummary(ReportInsight insight) {
        if (insight == null) {
            return;
        }
        if (insight.topRiskItems.isEmpty()) {
            insight.topRiskItems.add("No critical target exceptions identified from available scores.");
        }
        if (insight.topStrengthItems.isEmpty()) {
            insight.topStrengthItems.add("No high-performing targets identified from available scores.");
        }

        boolean highRisk = insight.latestWeightedScore > 0.0 && insight.latestWeightedScore < 50.0
                || insight.riskRedTargets > 0
                || insight.targetsWithoutScore > 0
                || insight.probationDecisionRequired;
        boolean mediumRisk = insight.riskAmberTargets > 0
                || insight.targetsWithoutEvidence > 0
                || insight.outstandingInterventionCount > 0
                || insight.alignmentGapEmployeeManager >= 1.0
                || insight.alignmentGapManagerAgreed >= 1.0
                || insight.alignmentGapAgreedModerated >= 1.0;

        if (highRisk) {
            insight.decisionRiskLevel = "High";
            insight.decisionRecommendation = "Escalate for management decision; confirm intervention ownership before closure.";
        } else if (mediumRisk) {
            insight.decisionRiskLevel = "Medium";
            insight.decisionRecommendation = "Monitor with targeted follow-up on evidence, action plans, and score alignment.";
        } else {
            insight.decisionRiskLevel = "Low";
            insight.decisionRecommendation = "Maintain current performance trajectory and close completed interventions.";
        }
    }

    private void addLimited(List<String> values, String value, int limit) {
        if (values == null || !hasText(value) || values.size() >= limit) {
            return;
        }
        values.add(value);
    }

    private String describeTargetScore(Target target, double scoreValue) {
        return targetLabel(target) + " (score " + roundTwoDecimals(scoreValue) + ", weight " + weightLabel(target) + ")";
    }

    private String targetLabel(Target target) {
        if (target == null) {
            return "Unknown target";
        }
        if (hasText(target.getMeasure())) {
            return target.getMeasure();
        }
        if (target.getOutput() != null && hasText(target.getOutput().getName())) {
            return target.getOutput().getName();
        }
        return "Target #" + target.getId();
    }

    private String weightLabel(Target target) {
        if (target == null || target.getAllocatedWeight() == null) {
            return "0%";
        }
        return roundTwoDecimals(target.getAllocatedWeight()) + "%";
    }

    private double highestScoreVariance(Score score) {
        if (score == null) {
            return 0.0;
        }
        double variance = 0.0;
        variance = Math.max(variance, positiveGap(score.getEmployeeScore(), score.getManagerScore()));
        variance = Math.max(variance, positiveGap(score.getManagerScore(), score.getAgreedScore()));
        variance = Math.max(variance, positiveGap(score.getAgreedScore(), score.getModeratedScore()));
        return roundTwoDecimals(variance);
    }

    private double positiveGap(double first, double second) {
        if (first <= 0.0 || second <= 0.0) {
            return 0.0;
        }
        return Math.abs(first - second);
    }

    private boolean isClosedStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        return "CLOSED".equals(normalized)
                || "COMPLETED".equals(normalized)
                || PMConstants.PROBATION_STATUS_EVALUATION_COMPLETED.equals(normalized)
                || PMConstants.PROBATION_STATUS_AUTHORIZED.equals(normalized)
                || "APPROVED".equals(normalized)
                || "CONFIRMED".equals(normalized)
                || "ARCHIVED".equals(normalized);
    }

    private String resolveProbationPerformanceBand(double mark) {
        if (mark >= 4.5) {
            return "Outstanding";
        }
        if (mark >= 3.5) {
            return "Exceeds expectations";
        }
        if (mark >= 2.5) {
            return "Meets expectations";
        }
        if (mark >= 1.5) {
            return "Needs improvement";
        }
        return mark > 0.0 ? "Unsatisfactory" : "Not assessed";
    }

    private boolean isPastDate(String value) {
        LocalDate date = parseLocalDate(value);
        return date != null && date.isBefore(LocalDate.now());
    }

    private String blankToDefault(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private void applyReportInsightAverages(ReportInsight insight,
                                            Scorecard scorecard,
                                            List<Target> targets,
                                            ReportingDate reportingDate) {
        if (reportingDate == null) {
            insight.averageEmployeeScore = safeScore(goalService.getAverageEmployeeScore(scorecard.getId()));
            insight.averageManagerScore = safeScore(goalService.getAverageManagerScore(scorecard.getId()));
            insight.averageAgreedScore = safeScore(goalService.getAverageAgreedScore(scorecard.getId()));
            insight.averageModeratedScore = safeScore(goalService.getAverageModeratorScore(scorecard.getId()));
            return;
        }

        OverallScore overallScore = overallScoreService.getOverallScoreByScorecardAndReportingDate(scorecard, reportingDate);
        if (hasAnyOverallScore(overallScore)) {
            insight.averageEmployeeScore = safeScore(orZero(overallScore.getEmployeeOverall()));
            insight.averageManagerScore = safeScore(orZero(overallScore.getManagerOverall()));
            insight.averageAgreedScore = safeScore(orZero(overallScore.getAgreedOverall()));
            insight.averageModeratedScore = safeScore(orZero(overallScore.getModeratedOverall()));
            return;
        }

        Map<Long, Score> finalScoresById = new HashMap<Long, Score>();
        for (Target target : targets) {
            Score score = resolveInsightScore(target, reportingDate);
            if (score != null && score.getId() > 0) {
                finalScoresById.put(score.getId(), score);
            }
        }

        double employeeTotal = 0.0;
        int employeeCount = 0;
        double managerTotal = 0.0;
        int managerCount = 0;
        double agreedTotal = 0.0;
        int agreedCount = 0;
        double moderatedTotal = 0.0;
        int moderatedCount = 0;

        for (Score score : finalScoresById.values()) {
            if (score.getEmployeeScore() > 0.0) {
                employeeTotal += score.getEmployeeScore();
                employeeCount++;
            }
            if (score.getManagerScore() > 0.0) {
                managerTotal += score.getManagerScore();
                managerCount++;
            }
            if (score.getAgreedScore() > 0.0) {
                agreedTotal += score.getAgreedScore();
                agreedCount++;
            }
            if (score.getModeratedScore() > 0.0) {
                moderatedTotal += score.getModeratedScore();
                moderatedCount++;
            }
        }

        insight.averageEmployeeScore = averageScore(employeeTotal, employeeCount);
        insight.averageManagerScore = averageScore(managerTotal, managerCount);
        insight.averageAgreedScore = averageScore(agreedTotal, agreedCount);
        insight.averageModeratedScore = averageScore(moderatedTotal, moderatedCount);
    }

    private boolean hasAnyOverallScore(OverallScore overallScore) {
        return overallScore != null
                && (positive(overallScore.getEmployeeOverall())
                || positive(overallScore.getManagerOverall())
                || positive(overallScore.getAgreedOverall())
                || positive(overallScore.getModeratedOverall()));
    }

    private boolean positive(Double value) {
        return value != null && value > 0.0;
    }

    private double orZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private double averageScore(double total, int count) {
        if (count <= 0) {
            return 0.0;
        }
        return roundTwoDecimals(total / count);
    }

    private Score resolveInsightScore(Target target, ReportingDate reportingDate) {
        if (target == null || reportingDate == null) {
            return null;
        }
        List<Score> byTarget = scoreRepository.findScoresByTargetAndReportingDateOrderByIdDesc(target, reportingDate);
        if (byTarget != null && !byTarget.isEmpty()) {
            return byTarget.get(0);
        }
        if (target.getOutput() == null) {
            return null;
        }
        List<Score> byOutput = scoreRepository.findScoresByOutputAndReportingDateOrderByIdDesc(target.getOutput(), reportingDate);
        if (byOutput == null || byOutput.isEmpty()) {
            return null;
        }
        for (Score score : byOutput) {
            if (score != null && score.getTarget() != null && score.getTarget().getId() == target.getId()) {
                return score;
            }
        }
        for (Score score : byOutput) {
            if (score != null && score.getTarget() == null) {
                return score;
            }
        }
        return byOutput.get(0);
    }

    private Score resolveLatestInsightScore(Target target) {
        if (target == null) {
            return null;
        }
        List<Score> byTarget = scoreRepository.findScoresByTargetOrderByReportingDate_DateDescIdDesc(target);
        if (byTarget != null && !byTarget.isEmpty()) {
            return byTarget.get(0);
        }
        if (target.getOutput() == null) {
            return null;
        }
        List<Score> byOutput = scoreRepository.findScoresByOutputOrderByReportingDate_DateDescIdDesc(target.getOutput());
        if (byOutput == null || byOutput.isEmpty()) {
            return null;
        }
        for (Score score : byOutput) {
            if (isScoreForTarget(score, target)) {
                return score;
            }
        }
        for (Score score : byOutput) {
            if (score != null && score.getTarget() == null) {
                return score;
            }
        }
        return byOutput.get(0);
    }

    private Evidence resolveLatestEvidence(Target target, ReportingDate reportingDate) {
        if (target == null || target.getId() <= 0 || reportingDate == null || reportingDate.getId() <= 0) {
            return null;
        }
        List<Evidence> evidenceList = evidenceRepository.findEvidenceByTarget_IdAndReportingDate_IdOrderByIdDesc(target.getId(), reportingDate.getId());
        if (evidenceList == null || evidenceList.isEmpty()) {
            return null;
        }
        return evidenceList.get(0);
    }

    private Evidence resolveLatestEvidence(Target target) {
        if (target == null || target.getId() <= 0) {
            return null;
        }
        List<Evidence> evidenceList = evidenceRepository.findEvidenceByTarget_IdOrderByReportingDate_DateDescIdDesc(target.getId());
        if (evidenceList == null || evidenceList.isEmpty()) {
            return null;
        }
        return evidenceList.get(0);
    }

    private boolean isScoreForTarget(Score score, Target target) {
        return score != null
                && score.getTarget() != null
                && target != null
                && score.getTarget().getId() == target.getId();
    }

    private ReportingDate resolveInsightReportingDate(ReportingPeriod reportingPeriod) {
        if (reportingPeriod == null || reportingPeriod.getReportingDates() == null || reportingPeriod.getReportingDates().isEmpty()) {
            return null;
        }
        ReportingDate selected = null;
        LocalDate selectedDate = null;
        for (ReportingDate reportingDate : reportingPeriod.getReportingDates()) {
            if (reportingDate == null) {
                continue;
            }
            LocalDate candidateDate = parseLocalDate(reportingDate.getEndDate());
            if (candidateDate == null) {
                if (selected == null) {
                    selected = reportingDate;
                }
                continue;
            }
            if (selectedDate == null || candidateDate.isAfter(selectedDate)) {
                selected = reportingDate;
                selectedDate = candidateDate;
            }
        }
        return selected;
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private double resolveTargetInsightScore(Target target) {
        if (target == null) {
            return 0.0;
        }
        Double[] candidates = new Double[]{
                target.getModeratedScore(),
                target.getAgreedScore(),
                target.getManagerScore(),
                target.getEmployeeScore()
        };
        for (Double candidate : candidates) {
            if (candidate != null && candidate > 0.0) {
                return candidate;
            }
        }
        return 0.0;
    }

    private double resolveInsightScoreValue(Score score) {
        if (score == null) {
            return 0.0;
        }
        double[] candidates = new double[]{
                score.getModeratedScore(),
                score.getAgreedScore(),
                score.getManagerScore(),
                score.getEmployeeScore(),
                score.getActual()
        };
        for (double candidate : candidates) {
            if (candidate > 0.0) {
                return candidate;
            }
        }
        return 0.0;
    }

    private boolean hasEvidence(Evidence evidence) {
        return evidence != null && (hasText(evidence.getEvidence()) || hasText(evidence.getAttachmentName()));
    }

    private boolean hasEvidence(Score score) {
        return score != null && (hasText(score.getEvidence()) || hasText(score.getAttachmentName()));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private double safeScore(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return roundTwoDecimals(value);
    }

    private double percentage(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return roundTwoDecimals((numerator * 100.0) / denominator);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private ReportingPeriod resolveDefaultScoresReportingPeriod(List<ReportingPeriod> reportingPeriods) {
        ReportingPeriod activeReportingPeriod = reportingPeriodService.getActiveReportingPeriod();
        if (activeReportingPeriod != null) {
            return activeReportingPeriod;
        }
        if (reportingPeriods == null || reportingPeriods.isEmpty()) {
            return null;
        }

        ReportingPeriod selected = null;
        LocalDate selectedEndDate = null;
        for (ReportingPeriod reportingPeriod : reportingPeriods) {
            if (reportingPeriod == null) {
                continue;
            }
            LocalDate endDate = parseLocalDate(reportingPeriod.getEndDate());
            if (selected == null
                    || (endDate != null && (selectedEndDate == null || endDate.isAfter(selectedEndDate)))) {
                selected = reportingPeriod;
                selectedEndDate = endDate;
            }
        }
        return selected;
    }

    private ReportingPeriod resolveReportingPeriodForPerformanceLevels(List<ReportingPeriod> reportingPeriods, Long reportingPeriodId) {
        if (reportingPeriodId != null && reportingPeriodId > 0) {
            ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(reportingPeriodId);
            if (reportingPeriod != null) {
                return reportingPeriod;
            }
        }
        return resolveDefaultScoresReportingPeriod(reportingPeriods);
    }

    private ReportingPeriod resolveReportingPeriodFromScorecards(List<Scorecard> scorecards) {
        if (scorecards == null) {
            return null;
        }
        for (Scorecard scorecard : scorecards) {
            if (scorecard != null && scorecard.getReportingPeriod() != null) {
                return scorecard.getReportingPeriod();
            }
        }
        return null;
    }

    private List<Long> extractEmployeeIdsFromScorecards(List<Scorecard> scorecards) {
        List<Long> employeeIds = new ArrayList<Long>();
        if (scorecards == null) {
            return employeeIds;
        }
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getOwner() == null || scorecard.getOwner().getId() <= 0) {
                continue;
            }
            addUniqueLong(employeeIds, scorecard.getOwner().getId());
        }
        return employeeIds;
    }

    private List<Long> normalizeEmployeeIds(List<Long> employeeIds) {
        List<Long> normalizedEmployeeIds = new ArrayList<Long>();
        if (employeeIds == null) {
            return normalizedEmployeeIds;
        }
        for (Long employeeId : employeeIds) {
            if (employeeId != null && employeeId > 0) {
                addUniqueLong(normalizedEmployeeIds, employeeId);
            }
        }
        return normalizedEmployeeIds;
    }

    private List<Scorecard> filterScorecardsByEmployeeIds(List<Scorecard> scorecards, List<Long> employeeIds) {
        List<Scorecard> filteredScorecards = new ArrayList<Scorecard>();
        if (scorecards == null || employeeIds == null || employeeIds.isEmpty()) {
            return filteredScorecards;
        }
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getOwner() == null) {
                continue;
            }
            if (containsLong(employeeIds, scorecard.getOwner().getId())) {
                filteredScorecards.add(scorecard);
            }
        }
        return filteredScorecards;
    }

    private void addUniqueLong(List<Long> values, Long value) {
        if (values == null || value == null || value <= 0) {
            return;
        }
        if (!containsLong(values, value)) {
            values.add(value);
        }
    }

    private boolean containsLong(List<Long> values, Long value) {
        if (values == null || value == null) {
            return false;
        }
        for (Long existing : values) {
            if (existing != null && existing.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private List<ReportingDate> resolveAllReportingDates(List<ReportingPeriod> reportingPeriods) {
        List<ReportingDate> reportingDates = new ArrayList<ReportingDate>();
        if (reportingPeriods == null) {
            return reportingDates;
        }
        for (ReportingPeriod reportingPeriod : reportingPeriods) {
            if (reportingPeriod == null || reportingPeriod.getId() <= 0) {
                continue;
            }
            List<ReportingDate> periodReportingDates = reportingDateService.listAllReportingDates(reportingPeriod);
            if (periodReportingDates == null) {
                continue;
            }
            for (ReportingDate reportingDate : periodReportingDates) {
                addUniqueReportingDate(reportingDates, reportingDate);
            }
        }
        return sortReportingDates(reportingDates);
    }

    private String resolveDefaultPerformanceLevelDate(List<ReportingDate> reportingDates) {
        ReportingDate latestReportingDate = resolveLatestReportingDate(reportingDates);
        return latestReportingDate == null ? "" : resolveReportingDateLabel(latestReportingDate);
    }

    private PerformanceLevelDateRange resolvePerformanceLevelDateRange(String fromDate,
                                                                       String toDate,
                                                                       Long fromReportingDateId,
                                                                       Long toReportingDateId,
                                                                       Long reportingDateId,
                                                                       List<ReportingDate> reportingDates,
                                                                       HttpServletRequest request) {
        String defaultDate = resolveDefaultPerformanceLevelDate(reportingDates);
        ReportingDate selectedReportingDate = findReportingDateById(reportingDates, reportingDateId);
        if (selectedReportingDate != null && !hasText(fromDate) && !hasText(toDate)) {
            defaultDate = resolveReportingDateLabel(selectedReportingDate);
        }

        ReportingDate fromReportingDate = resolvePerformanceLevelReportingDateSelection(reportingDates, fromReportingDateId, fromDate, defaultDate);
        ReportingDate toReportingDate = resolvePerformanceLevelReportingDateSelection(reportingDates, toReportingDateId, toDate, null);
        if (toReportingDate == null) {
            toReportingDate = fromReportingDate;
        }

        String resolvedFromDate = fromReportingDate == null
                ? (hasText(fromDate) ? fromDate.trim() : defaultDate)
                : resolveReportingDateLabel(fromReportingDate);
        String resolvedToDate = toReportingDate == null
                ? (hasText(toDate) ? toDate.trim() : resolvedFromDate)
                : resolveReportingDateLabel(toReportingDate);
        LocalDate resolvedFrom = parseLocalDate(resolvedFromDate);
        LocalDate resolvedTo = parseLocalDate(resolvedToDate);
        boolean valid = true;

        if (resolvedFrom == null || resolvedTo == null) {
            valid = false;
            if (hasText(resolvedFromDate) || hasText(resolvedToDate)) {
                PortletUtils.addErrorMsg("Select valid From and To dates.", request);
            }
        } else if (resolvedFrom.isAfter(resolvedTo)) {
            valid = false;
            PortletUtils.addErrorMsg("From date cannot be greater than To date.", request);
        }

        return new PerformanceLevelDateRange(
                resolvedFromDate,
                resolvedToDate,
                resolvedFrom,
                resolvedTo,
                fromReportingDate == null ? 0L : fromReportingDate.getId(),
                toReportingDate == null ? 0L : toReportingDate.getId(),
                valid
        );
    }

    private ReportingDate findReportingDateById(List<ReportingDate> reportingDates, Long reportingDateId) {
        if (reportingDates == null || reportingDateId == null || reportingDateId <= 0) {
            return null;
        }
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate != null && reportingDate.getId() == reportingDateId) {
                return reportingDate;
            }
        }
        return null;
    }

    private ReportingDate resolvePerformanceLevelReportingDateSelection(List<ReportingDate> reportingDates,
                                                                        Long reportingDateId,
                                                                        String reportingDateValue,
                                                                        String defaultDateValue) {
        ReportingDate reportingDate = findReportingDateById(reportingDates, reportingDateId);
        if (reportingDate != null) {
            return reportingDate;
        }
        reportingDate = findReportingDateByValue(reportingDates, reportingDateValue);
        if (reportingDate != null) {
            return reportingDate;
        }
        return findReportingDateByValue(reportingDates, defaultDateValue);
    }

    private ReportingDate findReportingDateByValue(List<ReportingDate> reportingDates, String reportingDateValue) {
        if (reportingDates == null || !hasText(reportingDateValue)) {
            return null;
        }
        LocalDate selectedDate = parseLocalDate(reportingDateValue);
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate == null) {
                continue;
            }
            if (selectedDate != null) {
                LocalDate candidateDate = parseLocalDate(reportingDate.getEndDate());
                if (selectedDate.equals(candidateDate)) {
                    return reportingDate;
                }
            } else if (reportingDateValue.trim().equals(reportingDate.getEndDate())) {
                return reportingDate;
            }
        }
        return null;
    }

    private List<PerformanceLevelReportingDateOption> buildPerformanceLevelReportingDateOptions(List<ReportingDate> reportingDates) {
        List<PerformanceLevelReportingDateOption> options = new ArrayList<PerformanceLevelReportingDateOption>();
        if (reportingDates == null) {
            return options;
        }
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate == null || reportingDate.getId() <= 0) {
                continue;
            }
            LocalDate reportingDateKey = sortDateKey(reportingDate);
            options.add(new PerformanceLevelReportingDateOption(
                    reportingDate.getId(),
                    LocalDate.MIN.equals(reportingDateKey) ? resolveReportingDateLabel(reportingDate) : reportingDateKey.toString(),
                    resolvePerformanceLevelDateLabel(reportingDate)
            ));
        }
        return options;
    }

    private List<ReportingDate> filterReportingDatesByRange(List<ReportingDate> reportingDates, LocalDate fromDate, LocalDate toDate) {
        List<ReportingDate> filteredReportingDates = new ArrayList<ReportingDate>();
        if (reportingDates == null || fromDate == null || toDate == null) {
            return filteredReportingDates;
        }
        for (ReportingDate reportingDate : reportingDates) {
            LocalDate reportingDateKey = reportingDate == null ? null : parseLocalDate(reportingDate.getEndDate());
            if (reportingDateKey == null) {
                continue;
            }
            if (!reportingDateKey.isBefore(fromDate) && !reportingDateKey.isAfter(toDate)) {
                filteredReportingDates.add(reportingDate);
            }
        }
        return sortReportingDates(filteredReportingDates);
    }

    private List<Account> resolvePerformanceLevelEmployees(List<Long> selectedEmployeeIds, List<ReportingDate> reportingDates) {
        List<Account> employees = new ArrayList<Account>();
        if (selectedEmployeeIds != null && !selectedEmployeeIds.isEmpty()) {
            for (Long employeeId : selectedEmployeeIds) {
                if (employeeId == null || employeeId <= 0) {
                    continue;
                }
                try {
                    addUniqueAccount(employees, accountService.getAccountById(employeeId));
                } catch (Exception ignored) {

                }
            }
            return employees;
        }

        List<Scorecard> scorecards = resolveScorecardsForReportingDates(reportingDates);
        for (Scorecard scorecard : scorecards) {
            if (scorecard != null && scorecard.getOwner() != null) {
                addUniqueAccount(employees, scorecard.getOwner());
            }
        }
        return employees;
    }

    private List<Long> extractEmployeeIdsFromAccounts(List<Account> accounts) {
        List<Long> employeeIds = new ArrayList<Long>();
        if (accounts == null) {
            return employeeIds;
        }
        for (Account account : accounts) {
            if (account != null && account.getId() > 0) {
                addUniqueLong(employeeIds, account.getId());
            }
        }
        return employeeIds;
    }

    private String buildPerformanceLevelEmployeeSelectionUrl(PerformanceLevelDateRange dateRange, List<Long> employeeIds) {
        StringBuilder url = new StringBuilder("/performance-review/view-performance-levels-select-year");
        String separator = "?";
        if (dateRange != null && dateRange.fromReportingDateId > 0) {
            url.append(separator).append("fromReportingDateId=").append(dateRange.fromReportingDateId);
            separator = "&";
        }
        if (dateRange != null && dateRange.toReportingDateId > 0) {
            url.append(separator).append("toReportingDateId=").append(dateRange.toReportingDateId);
            separator = "&";
        }
        if (employeeIds != null) {
            for (Long employeeId : employeeIds) {
                if (employeeId == null || employeeId <= 0) {
                    continue;
                }
                url.append(separator).append("employeeIds=").append(employeeId);
                separator = "&";
            }
        }
        return url.toString();
    }

    private List<Scorecard> resolvePerformanceLevelScorecards(List<Account> employees, List<ReportingDate> reportingDates) {
        List<Scorecard> scorecards = resolveScorecardsForReportingDates(reportingDates);
        List<Long> employeeIds = extractEmployeeIdsFromAccounts(employees);
        if (employeeIds.isEmpty()) {
            return scorecards;
        }
        return filterScorecardsByEmployeeIds(scorecards, employeeIds);
    }

    private List<Scorecard> resolveScorecardsForReportingDates(List<ReportingDate> reportingDates) {
        List<Scorecard> scorecards = new ArrayList<Scorecard>();
        if (reportingDates == null) {
            return scorecards;
        }
        List<ReportingPeriod> reportingPeriods = new ArrayList<ReportingPeriod>();
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate == null || reportingDate.getReportingPeriod() == null) {
                continue;
            }
            addUniqueReportingPeriod(reportingPeriods, reportingDate.getReportingPeriod());
        }
        for (ReportingPeriod reportingPeriod : reportingPeriods) {
            List<Scorecard> periodScorecards = scorecardService.getScoresByPeriodId(reportingPeriod);
            if (periodScorecards == null) {
                continue;
            }
            for (Scorecard scorecard : periodScorecards) {
                addUniqueScorecard(scorecards, scorecard);
            }
        }
        return scorecards;
    }

    private Map<String, Scorecard> mapScorecardsByEmployeeAndPeriod(List<Scorecard> scorecards) {
        Map<String, Scorecard> scorecardByEmployeeAndPeriod = new HashMap<String, Scorecard>();
        if (scorecards == null) {
            return scorecardByEmployeeAndPeriod;
        }
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getOwner() == null || scorecard.getReportingPeriod() == null) {
                continue;
            }
            scorecardByEmployeeAndPeriod.put(
                    performanceLevelScorecardKey(scorecard.getOwner().getId(), scorecard.getReportingPeriod().getId()),
                    scorecard
            );
        }
        return scorecardByEmployeeAndPeriod;
    }

    private Scorecard resolveEmployeeScorecardForReportingDate(Map<String, Scorecard> scorecardByEmployeeAndPeriod,
                                                               Account employee,
                                                               ReportingDate reportingDate) {
        if (scorecardByEmployeeAndPeriod == null
                || employee == null
                || reportingDate == null
                || reportingDate.getReportingPeriod() == null) {
            return null;
        }
        return scorecardByEmployeeAndPeriod.get(performanceLevelScorecardKey(employee.getId(), reportingDate.getReportingPeriod().getId()));
    }

    private String performanceLevelScorecardKey(long employeeId, long reportingPeriodId) {
        return employeeId + ":" + reportingPeriodId;
    }

    private List<Account> filterEmployeesWithScorecardInRange(List<Account> employees,
                                                              List<ReportingDate> reportingDates,
                                                              Map<String, Scorecard> scorecardByEmployeeAndPeriod) {
        List<Account> filteredEmployees = new ArrayList<Account>();
        if (employees == null || reportingDates == null || reportingDates.isEmpty()) {
            return filteredEmployees;
        }
        for (Account employee : employees) {
            if (hasAnyScorecardInRange(employee, reportingDates, scorecardByEmployeeAndPeriod)) {
                filteredEmployees.add(employee);
            }
        }
        return filteredEmployees;
    }

    private List<String> resolveEmployeesWithoutScorecardsInRange(List<Account> employees,
                                                                  List<ReportingDate> reportingDates,
                                                                  Map<String, Scorecard> scorecardByEmployeeAndPeriod) {
        List<String> missingEmployees = new ArrayList<String>();
        if (employees == null || reportingDates == null || reportingDates.isEmpty()) {
            return missingEmployees;
        }
        for (Account employee : employees) {
            if (employee == null || hasAnyScorecardInRange(employee, reportingDates, scorecardByEmployeeAndPeriod)) {
                continue;
            }
            missingEmployees.add(employee.getFullName());
        }
        return missingEmployees;
    }

    private boolean hasAnyScorecardInRange(Account employee,
                                           List<ReportingDate> reportingDates,
                                           Map<String, Scorecard> scorecardByEmployeeAndPeriod) {
        if (employee == null || reportingDates == null || reportingDates.isEmpty()) {
            return false;
        }
        for (ReportingDate reportingDate : reportingDates) {
            if (resolveEmployeeScorecardForReportingDate(scorecardByEmployeeAndPeriod, employee, reportingDate) != null) {
                return true;
            }
        }
        return false;
    }

    private List<String> resolveReportingPeriodLabels(List<ReportingDate> reportingDates) {
        List<String> labels = new ArrayList<String>();
        if (reportingDates == null) {
            return labels;
        }
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate == null || reportingDate.getReportingPeriod() == null) {
                continue;
            }
            addUniqueString(labels, resolveReportingPeriodLabel(reportingDate.getReportingPeriod()));
        }
        return labels;
    }

    private String resolveReportingPeriodLabel(ReportingPeriod reportingPeriod) {
        if (reportingPeriod == null) {
            return "Unknown period";
        }
        return blankToDefault(reportingPeriod.getStartDate(), "N/A") + " to " + blankToDefault(reportingPeriod.getEndDate(), "N/A");
    }

    private String resolvePerformanceLevelDateLabel(ReportingDate reportingDate) {
        if (reportingDate == null) {
            return "N/A";
        }
        return resolveReportingDateLabel(reportingDate) + " | " + resolveReportingPeriodLabel(reportingDate.getReportingPeriod());
    }

    private String resolvePerformanceLevelColor(double score) {
        if (score < 50.0) {
            return "#bf4a4a";
        }
        if (score < 70.0) {
            return "rgba(217, 165, 52, 0.74)";
        }
        return "rgba(31, 143, 95, 0.74)";
    }

    private String resolvePerformanceLevelMissingColor() {
        return "rgba(140, 153, 165, 0.42)";
    }

    private void addUniqueReportingPeriod(List<ReportingPeriod> reportingPeriods, ReportingPeriod reportingPeriod) {
        if (reportingPeriods == null || reportingPeriod == null || reportingPeriod.getId() <= 0) {
            return;
        }
        for (ReportingPeriod existing : reportingPeriods) {
            if (existing != null && existing.getId() == reportingPeriod.getId()) {
                return;
            }
        }
        reportingPeriods.add(reportingPeriod);
    }

    private void addUniqueAccount(List<Account> accounts, Account account) {
        if (accounts == null || account == null || account.getId() <= 0) {
            return;
        }
        for (Account existing : accounts) {
            if (existing != null && existing.getId() == account.getId()) {
                return;
            }
        }
        accounts.add(account);
    }

    private void addUniqueString(List<String> values, String value) {
        if (values == null || !hasText(value)) {
            return;
        }
        for (String existing : values) {
            if (value.equals(existing)) {
                return;
            }
        }
        values.add(value);
    }

    private List<ReportingDate> sortReportingDates(List<ReportingDate> reportingDates) {
        List<ReportingDate> sortedReportingDates = reportingDates == null
                ? new ArrayList<ReportingDate>()
                : new ArrayList<ReportingDate>(reportingDates);
        sortedReportingDates.sort(Comparator.comparing(this::sortDateKey));
        return sortedReportingDates;
    }

    private ReportingDate resolveSelectedReviewReportingDate(List<ReportingDate> reportingDates, Long reportingDateId) {
        if (reportingDates == null || reportingDates.isEmpty()) {
            return null;
        }
        if (reportingDateId != null && reportingDateId > 0) {
            for (ReportingDate reportingDate : reportingDates) {
                if (reportingDate != null && reportingDate.getId() == reportingDateId) {
                    return reportingDate;
                }
            }
        }
        return resolveLatestReportingDate(reportingDates);
    }

    private ReportingDate resolveLatestReportingDate(List<ReportingDate> reportingDates) {
        ReportingDate selected = null;
        LocalDate selectedDate = null;
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate == null) {
                continue;
            }
            LocalDate candidateDate = parseLocalDate(reportingDate.getEndDate());
            if (candidateDate == null) {
                if (selected == null) {
                    selected = reportingDate;
                }
                continue;
            }
            if (selectedDate == null || candidateDate.isAfter(selectedDate)) {
                selected = reportingDate;
                selectedDate = candidateDate;
            }
        }
        return selected;
    }

    private List<ReviewReportingDateOption> buildReviewReportingDateOptions(List<ReportingPeriod> reportingPeriods) {
        List<ReviewReportingDateOption> options = new ArrayList<ReviewReportingDateOption>();
        if (reportingPeriods == null) {
            return options;
        }
        for (ReportingPeriod reportingPeriod : reportingPeriods) {
            if (reportingPeriod == null || reportingPeriod.getId() <= 0) {
                continue;
            }
            List<ReportingDate> reportingDates = sortReportingDates(reportingDateService.listAllReportingDates(reportingPeriod));
            for (ReportingDate reportingDate : reportingDates) {
                if (reportingDate == null || reportingDate.getId() <= 0) {
                    continue;
                }
                options.add(new ReviewReportingDateOption(
                        reportingPeriod.getId(),
                        reportingDate.getId(),
                        resolveReportingDateLabel(reportingDate)
                ));
            }
        }
        return options;
    }

    private int classifyProgressStatus(String status, double progress) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (progress >= 100.0
                || "COMPLETED".equals(normalized)
                || "DONE".equals(normalized)
                || "CLOSED".equals(normalized)
                || "RESOLVED".equals(normalized)) {
            return 2;
        }
        if (progress > 0.0
                || "IN_PROGRESS".equals(normalized)
                || "INPROGRESS".equals(normalized)
                || "ACTIVE".equals(normalized)
                || "ONGOING".equals(normalized)) {
            return 1;
        }
        return 0;
    }

    public static class ReviewReportingDateOption {
        private final long periodId;
        private final long id;
        private final String label;

        ReviewReportingDateOption(long periodId, long id, String label) {
            this.periodId = periodId;
            this.id = id;
            this.label = label;
        }

        public long getPeriodId() {
            return periodId;
        }

        public long getId() {
            return id;
        }

        public String getLabel() {
            return label;
        }
    }

    public static class ScorecardRiskProfile {
        private double latestWeightedScore = 0.0;
        private double previousWeightedScore = 0.0;
        private double scoreMovement = 0.0;
        private String scoreMovementLabel = "No previous score";
        private String performanceBand = "Not fully assessed";
        private String decisionRiskLevel = "Low";
        private String decisionRecommendation = "";
        private int openInterventionCount = 0;
        private int activePipCount = 0;
        private int openActionPlanCount = 0;
        private int exceptionCount = 0;
        private String probationStatus = "No probation contract";
        private boolean probationDecisionRequired = false;

        public double getLatestWeightedScore() {
            return latestWeightedScore;
        }

        public double getPreviousWeightedScore() {
            return previousWeightedScore;
        }

        public double getScoreMovement() {
            return scoreMovement;
        }

        public String getScoreMovementLabel() {
            return scoreMovementLabel;
        }

        public String getPerformanceBand() {
            return performanceBand;
        }

        public String getDecisionRiskLevel() {
            return decisionRiskLevel;
        }

        public String getDecisionRecommendation() {
            return decisionRecommendation;
        }

        public int getOpenInterventionCount() {
            return openInterventionCount;
        }

        public int getActivePipCount() {
            return activePipCount;
        }

        public int getOpenActionPlanCount() {
            return openActionPlanCount;
        }

        public int getExceptionCount() {
            return exceptionCount;
        }

        public String getProbationStatus() {
            return probationStatus;
        }

        public boolean isProbationDecisionRequired() {
            return probationDecisionRequired;
        }
    }

    private static class ScorecardTrend {
        private List<String> labels = new ArrayList<String>();
        private List<Double> scores = new ArrayList<Double>();
        private double latestScore = 0.0;
        private double previousScore = 0.0;
        private double movement = 0.0;
        private String movementLabel = "No previous score";
    }

    private static class ScorecardTrendPoint {
        private final Scorecard scorecard;
        private final ReportingPeriod reportingPeriod;
        private final ReportingDate reportingDate;

        private ScorecardTrendPoint(Scorecard scorecard, ReportingPeriod reportingPeriod, ReportingDate reportingDate) {
            this.scorecard = scorecard;
            this.reportingPeriod = reportingPeriod;
            this.reportingDate = reportingDate;
        }
    }

    private static class PerformanceLevelDateRange {
        private final String fromDate;
        private final String toDate;
        private final LocalDate from;
        private final LocalDate to;
        private final long fromReportingDateId;
        private final long toReportingDateId;
        private final boolean valid;

        private PerformanceLevelDateRange(String fromDate,
                                          String toDate,
                                          LocalDate from,
                                          LocalDate to,
                                          long fromReportingDateId,
                                          long toReportingDateId,
                                          boolean valid) {
            this.fromDate = fromDate == null ? "" : fromDate;
            this.toDate = toDate == null ? "" : toDate;
            this.from = from;
            this.to = to;
            this.fromReportingDateId = fromReportingDateId;
            this.toReportingDateId = toReportingDateId;
            this.valid = valid;
        }
    }

    private String effectiveReviewRole(Account account) {
        if (accessControlService.hasPermissionForAnyScope(account, AccessPermissions.SCORECARD_MODERATE)) {
            return PMConstants.MODERATOR;
        }
        return account == null ? "" : account.getRole();
    }

    public static class PerformanceLevelReportingDateOption {
        private final long id;
        private final String dateValue;
        private final String label;

        private PerformanceLevelReportingDateOption(long id, String dateValue, String label) {
            this.id = id;
            this.dateValue = dateValue;
            this.label = label;
        }

        public long getId() {
            return id;
        }

        public String getDateValue() {
            return dateValue;
        }

        public String getLabel() {
            return label;
        }
    }

    private static class ReportInsight {
        private double averageEmployeeScore = 0.0;
        private double averageManagerScore = 0.0;
        private double averageAgreedScore = 0.0;
        private double averageModeratedScore = 0.0;
        private double latestWeightedScore = 0.0;
        private double previousWeightedScore = 0.0;
        private double scoreMovement = 0.0;
        private String scoreMovementLabel = "No previous score";
        private String performanceBand = "Not fully assessed";
        private String decisionRiskLevel = "Low";
        private String decisionRecommendation = "";
        private double alignmentGapEmployeeManager = 0.0;
        private double alignmentGapManagerAgreed = 0.0;
        private double alignmentGapAgreedModerated = 0.0;
        private int totalTargets = 0;
        private int targetsWithScore = 0;
        private int targetsWithoutScore = 0;
        private double scoreCoveragePercent = 0.0;
        private int riskRedTargets = 0;
        private int riskAmberTargets = 0;
        private int riskGreenTargets = 0;
        private int targetsWithEvidence = 0;
        private int targetsWithoutEvidence = 0;
        private double evidenceCoveragePercent = 0.0;
        private String insightReportingDateLabel = "Latest available context";
        private int pipOpenCount = 0;
        private int pipInProgressCount = 0;
        private int pipClosedCount = 0;
        private int actionOpenCount = 0;
        private int actionInProgressCount = 0;
        private int actionClosedCount = 0;
        private int outstandingInterventionCount = 0;
        private List<String> topRiskItems = new ArrayList<String>();
        private List<String> topStrengthItems = new ArrayList<String>();
        private List<String> lowPerformingTargets = new ArrayList<String>();
        private List<String> missingEvidenceTargets = new ArrayList<String>();
        private List<String> highVarianceTargets = new ArrayList<String>();
        private List<String> scoreTrendLabels = new ArrayList<String>();
        private List<Double> scoreTrendScores = new ArrayList<Double>();
        private String probationStatus = "No probation contract";
        private String probationPeriod = "N/A";
        private String probationCurrentStep = "N/A";
        private String probationEndDate = "N/A";
        private String probationRecommendation = "No probation decision required from available records.";
        private int probationKpiCount = 0;
        private int probationFlaggedKpiCount = 0;
        private double probationAverageProgress = 0.0;
        private boolean probationDecisionRequired = false;
    }

    private OverallScore resolveOverallScore(Map<Long, OverallScore> scoreByScorecard, Scorecard scorecard, ReportingDate reportingDate) {
        if (scorecard == null) {
            return null;
        }
        if (scoreByScorecard != null) {
            OverallScore overallScore = scoreByScorecard.get(scorecard.getId());
            if (overallScore != null) {
                return overallScore;
            }
        }
        OverallScore defaultScore = new OverallScore();
        defaultScore.setScorecard(scorecard);
        defaultScore.setReportingDate(reportingDate);
        defaultScore.setEmployeeOverall(0.0);
        defaultScore.setManagerOverall(0.0);
        defaultScore.setAgreedOverall(0.0);
        defaultScore.setModeratedOverall(0.0);
        return defaultScore;
    }

}
