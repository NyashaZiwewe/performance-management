package hr.performancemanagement.controllers.scorecard;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.CommentRepository;
import hr.performancemanagement.repository.EvidenceRepository;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.service.EvidenceService;
import hr.performancemanagement.service.GearService;
import hr.performancemanagement.service.OverallCommentService;
import hr.performancemanagement.service.OverallScoreService;
import hr.performancemanagement.service.OutcomeService;
import hr.performancemanagement.service.OutputService;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.service.api.ScoreService.StandardScorecardScoreService;
import hr.performancemanagement.service.api.ScoreService.ValueBasedScoreService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.constants.AccessPermissions;
import hr.performancemanagement.utils.constants.Pages;
import hr.performancemanagement.utils.dto.ScorecardWorkflowDefinition;
import hr.performancemanagement.utils.wrappers.EvidenceWrapper;
import hr.performancemanagement.utils.wrappers.GoalWrapper;
import hr.performancemanagement.utils.wrappers.OutputWrapper;
import hr.performancemanagement.utils.wrappers.ScorecardDisplayRow;
import hr.performancemanagement.utils.wrappers.ScorecardDisplaySection;
import hr.performancemanagement.utils.wrappers.TargetCaptureRow;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.BindingResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping(value="/scorecards")
public class ScorecardController {
    private static final Logger log = LoggerFactory.getLogger(ScorecardController.class);

    private enum ValueBasedCaptureStage {
        EMPLOYEE(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES, Pages.CAPTURE_EMPLOYEE_SCORE, "submit-employee-scores"),
        MANAGER(PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES, Pages.CAPTURE_MANAGER_SCORE, "submit-manager-scores"),
        AGREED(PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES, Pages.CAPTURE_AGREED_SCORE, "submit-agreed-scores"),
        MODERATED(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES, Pages.CAPTURE_MODERATED_SCORE, "submit-moderated-scores");

        private final String requiredActivity;
        private final String page;
        private final String submitUrl;

        ValueBasedCaptureStage(String requiredActivity, String page, String submitUrl) {
            this.requiredActivity = requiredActivity;
            this.page = page;
            this.submitUrl = submitUrl;
        }

        public String getRequiredActivity() {
            return requiredActivity;
        }

        public String getPage() {
            return page;
        }

        public String getSubmitUrl() {
            return submitUrl;
        }
    }

    private static class ScorecardActionState {
        private boolean canCaptureTargets;
        private boolean canSupervisorApproveTargets;
        private boolean canHrApproveTargets;
        private boolean canCaptureEmployeeScore;
        private boolean canCaptureManagerScore;
        private boolean canCaptureAgreedScore;
        private boolean canModerate;
        private boolean canApproveOwnerScores;
        private boolean canApproveAgreedScores;
        private String expectedActivity;

        private boolean canApproveTargets() {
            return canSupervisorApproveTargets || canHrApproveTargets;
        }

        private boolean hasAllowedAction() {
            return canCaptureTargets
                    || canApproveTargets()
                    || canCaptureEmployeeScore
                    || canCaptureManagerScore
                    || canCaptureAgreedScore
                    || canModerate
                    || canApproveOwnerScores
                    || canApproveAgreedScores;
        }
    }

    @Autowired
    private final ReportingPeriodService reportingPeriodService;
    @Autowired
    private final AccountService accountService;
    @Autowired
    private final DepartmentService departmentService;
    @Autowired
    private final ScorecardService scorecardService;
    @Autowired
    private final PerspectiveService perspectiveService;
    @Autowired
    private final GoalService goalService;
    @Autowired
    private final TargetService targetService;
    @Autowired
    private final GearService gearService;
    @Autowired
    private final OutcomeService outcomeService;
    @Autowired
    private final OutputService outputService;
    @Autowired
    private final StrategicObjectiveService strategicObjectiveService;
    @Autowired
    private final CommentService commentService;
    @Autowired
    private final NotificationService notificationService;
    @Autowired
    private final ApprovalService approvalService;
    @Autowired
    private final ReportingDateService reportingDateService;
    @Autowired
    private final StandardScorecardScoreService standardScorecardScoreService;
    @Autowired
    private final ValueBasedScoreService valueBasedScoreService;
    @Autowired
    private final ScorecardModelService scorecardModelService;
    @Autowired
    private final CommonService commonService;
    @Autowired
    private final SystemSettingService systemSettingService;
    @Autowired
    private AccessControlService accessControlService;
    @Autowired
    private PerformanceImprovementPlanService performanceImprovementPlanService;
    @Autowired
    private ReportingDateActivityPeriodService reportingDateActivityPeriodService;
    @Autowired
    private final ScorecardWorkflowService scorecardWorkflowService;
    @Autowired
    private final ScorecardReportingDateStageService scorecardReportingDateStageService;
    @Autowired
    private final EvidenceRepository evidenceRepository;
    @Autowired
    private final EvidenceService evidenceService;
    @Autowired
    private final CommentRepository commentRepository;
    @Autowired
    private final ScoreRepository scoreRepository;
    @Autowired
    private final OverallScoreService overallScoreService;
    @Autowired
    private final OverallCommentService overallCommentService;

    private final Environment environment;


    public ScorecardController(ReportingPeriodService reportingPeriodService, AccountService accountService, DepartmentService departmentService, ScorecardService scorecardService, PerspectiveService perspectiveService, GoalService goalService, TargetService targetService, GearService gearService, OutcomeService outcomeService, OutputService outputService, StrategicObjectiveService strategicObjectiveService, CommentService commentService, NotificationService notificationService, ApprovalService approvalService, ReportingDateService reportingDateService, StandardScorecardScoreService standardScorecardScoreService, ValueBasedScoreService valueBasedScoreService, ScorecardModelService scorecardModelService, CommonService commonService, SystemSettingService systemSettingService, ScorecardWorkflowService scorecardWorkflowService, ScorecardReportingDateStageService scorecardReportingDateStageService, EvidenceRepository evidenceRepository, EvidenceService evidenceService, CommentRepository commentRepository, ScoreRepository scoreRepository, OverallScoreService overallScoreService, OverallCommentService overallCommentService, Environment environment) {
        this.reportingPeriodService = reportingPeriodService;
        this.accountService = accountService;
        this.departmentService = departmentService;
        this.scorecardService = scorecardService;
        this.perspectiveService = perspectiveService;
        this.goalService = goalService;
        this.targetService = targetService;
        this.gearService = gearService;
        this.outcomeService = outcomeService;
        this.outputService = outputService;
        this.strategicObjectiveService = strategicObjectiveService;
        this.commentService = commentService;
        this.notificationService = notificationService;
        this.approvalService = approvalService;
        this.reportingDateService = reportingDateService;
        this.standardScorecardScoreService = standardScorecardScoreService;
        this.valueBasedScoreService = valueBasedScoreService;
        this.scorecardModelService = scorecardModelService;
        this.commonService = commonService;
        this.systemSettingService = systemSettingService;
        this.scorecardWorkflowService = scorecardWorkflowService;
        this.scorecardReportingDateStageService = scorecardReportingDateStageService;
        this.evidenceRepository = evidenceRepository;
        this.evidenceService = evidenceService;
        this.commentRepository = commentRepository;
        this.scoreRepository = scoreRepository;
        this.overallScoreService = overallScoreService;
        this.overallCommentService = overallCommentService;
        this.environment = environment;
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request, HttpSession session) {

        long configuredClientId = commonService.getConfiguredClientId();
        List<ReportingPeriod> REPORTING_PERIODS_LIST = reportingPeriodService.listAllReportingPeriods();
        List<ReportingDate> REPORTING_DATES_LIST = listReportingDates(REPORTING_PERIODS_LIST);
        List<Account> ACCOUNTS_LIST = configuredClientId > 0
                ? accountService.listAllAccountsByClientId(configuredClientId)
                : accountService.listAllAccounts();
        List<Department> DEPARTMENTS_LIST = configuredClientId > 0
                ? departmentService.listAllDepartments(configuredClientId)
                : departmentService.listAllDepartments();
        List<Perspective> PERSPECTIVES_LIST = perspectiveService.listAllPerspectives(configuredClientId);
        Account loggedUser = commonService.getLoggedUser();
        long loggedUserId = loggedUser.getId();
        String role = loggedUser.getRole();
        ReportingDate activeReportingDate = reportingDateService.getActiveReportingDate();
        boolean captureWindowOpen = reportingDateService.isReportingDateOpen(activeReportingDate);
        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();

        modelAndView.addObject("pageDomain", "Performance");
        modelAndView.addObject("pageName", "Scorecards");
        modelAndView.addObject("reportingPeriodsList", REPORTING_PERIODS_LIST);
        modelAndView.addObject("reportingDatesList", REPORTING_DATES_LIST);
        modelAndView.addObject("accountsList", ACCOUNTS_LIST);
        modelAndView.addObject("departmentsList", DEPARTMENTS_LIST);
        modelAndView.addObject("perspectivesList", PERSPECTIVES_LIST);
        modelAndView.addObject("loggedUserId", loggedUserId);
        modelAndView.addObject("loggedUser", loggedUser);
        modelAndView.addObject("role", role);
        modelAndView.addObject("captureWindowOpen", captureWindowOpen);
        modelAndView.addObject("scorecardWorkflow", workflow);
        modelAndView.addObject("approvalStatuses", workflow.getOrderedStatuses());
        modelAndView.addObject("approvalStatusCssClasses", workflow.getStatusCssClasses());
        modelAndView.addObject("approvalStatusLabels", workflow.getStatusDisplayLabels());
        modelAndView.addObject("approvalStatusStages", workflow.getStatusStageNames());
        modelAndView.addObject("approvalStatusActionLabels", workflow.getStatusActionButtonLabels());
        modelAndView.addObject("workflowRoleStageNames", workflow.getRoleStageNames());
        modelAndView.addObject("workflowRoleActionLabels", workflow.getRoleActionButtonLabels());
        modelAndView.addObject("workflowRoleRejectionLabels", workflow.getRoleRejectionButtonLabels());
        addTerminology(modelAndView);
        PortletUtils.addMessagesToPage(modelAndView, request);

    }

    private List<ReportingDate> listReportingDates(List<ReportingPeriod> reportingPeriods) {
        List<ReportingDate> reportingDates = new ArrayList<ReportingDate>();
        if (reportingPeriods == null || reportingPeriods.isEmpty()) {
            return reportingDates;
        }
        for (ReportingPeriod reportingPeriod : reportingPeriods) {
            if (reportingPeriod == null) {
                continue;
            }
            List<ReportingDate> periodReportingDates = reportingDateService.listAllReportingDates(reportingPeriod);
            if (periodReportingDates != null && !periodReportingDates.isEmpty()) {
                reportingDates.addAll(periodReportingDates);
            }
        }
        return reportingDates;
    }

    @RequestMapping
    public ModelAndView viewScorecards(@RequestParam(value = "reportingPeriodId", required = false) String reportingPeriodId,
                                       @RequestParam(value = "reportingDateId", required = false) String reportingDateId,
                                       @RequestParam(value = "departmentId", required = false) String departmentId,
                                       @RequestParam(value = "employeeId", required = false) String employeeId,
                                       @RequestParam(value = "approvalStatus", required = false) String approvalStatus,
                                       @RequestParam(value = "search", required = false) String search,
                                       HttpServletRequest request,
                                       HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_SCORECARDS);
        modelAndView.addObject("pageTitle", "View Scorecards");
        boolean searchSubmitted = search != null
                || reportingPeriodId != null
                || reportingDateId != null
                || departmentId != null
                || employeeId != null
                || StringUtils.hasText(approvalStatus);
        Long selectedReportingPeriodId = parseFilterId(reportingPeriodId);
        Long selectedReportingDateId = parseFilterId(reportingDateId);
        Long selectedDepartmentId = parseFilterId(departmentId);
        Long selectedEmployeeId = parseFilterId(employeeId);
        String approvalFilter = searchSubmitted ? normalizeFilterText(approvalStatus) : "";

        if (!searchSubmitted) {
            ReportingPeriod activeReportingPeriod = reportingPeriodService.getActiveReportingPeriod();
            selectedReportingPeriodId = activeReportingPeriod == null ? null : activeReportingPeriod.getId();
            selectedReportingDateId = null;
            selectedDepartmentId = null;
            selectedEmployeeId = null;
            approvalFilter = "";
        }

        List<Scorecard> scorecards = scorecardService.searchScorecards(
                selectedReportingPeriodId,
                selectedReportingDateId,
                selectedDepartmentId,
                selectedEmployeeId,
                approvalFilter
        );
        scorecards = filterViewableScorecards(scorecards);

        modelAndView.addObject("scorecards", scorecards);
        modelAndView.addObject("unmappedWorkflowScorecardCount", countUnmappedWorkflowScorecards(scorecards));
        modelAndView.addObject("scorecardCaptureScoresAllowed", buildCaptureScoresAllowedMap(scorecards));
        modelAndView.addObject("scorecardEditTargetsAllowed", buildEditTargetsAllowedMap(scorecards));
        modelAndView.addObject("scorecardViewReportAllowed", buildViewReportAllowedMap(scorecards));
        modelAndView.addObject("selectedReportingPeriodId", selectedReportingPeriodId);
        modelAndView.addObject("selectedReportingDateId", selectedReportingDateId);
        modelAndView.addObject("selectedDepartmentId", selectedDepartmentId);
        modelAndView.addObject("selectedEmployeeId", selectedEmployeeId);
        modelAndView.addObject("approvalStatus", approvalFilter);
        modelAndView.addObject("searchSubmitted", searchSubmitted);
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    private Long parseFilterId(String value) {
        if (!StringUtils.hasText(value) || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            Long parsedValue = Long.parseLong(value.trim());
            return parsedValue > 0 ? parsedValue : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalizeFilterText(String value) {
        if (!StringUtils.hasText(value) || "ALL".equalsIgnoreCase(value.trim())) {
            return "";
        }
        return value.trim();
    }

    private List<Scorecard> filterViewableScorecards(List<Scorecard> scorecards) {
        if (scorecards == null || scorecards.isEmpty()) {
            return Collections.emptyList();
        }
        List<Scorecard> viewableScorecards = new ArrayList<Scorecard>();
        for (Scorecard scorecard : scorecards) {
            if (canViewScorecard(scorecard)) {
                viewableScorecards.add(scorecard);
            }
        }
        return viewableScorecards;
    }

    private List<Scorecard> filterScorecards(List<Scorecard> scorecards, String ownerName, String approvalStatus) {
        if (scorecards == null || scorecards.isEmpty()) {
            return Collections.emptyList();
        }

        String ownerFilter = ownerName == null ? "" : ownerName.trim().toLowerCase();
        String approvalFilter = approvalStatus == null ? "" : approvalStatus.trim();

        if (ownerFilter.isEmpty() && approvalFilter.isEmpty()) {
            return scorecards;
        }

        List<Scorecard> filteredScorecards = new ArrayList<Scorecard>();
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null) {
                continue;
            }

            boolean ownerMatches = ownerFilter.isEmpty();
            if (!ownerMatches) {
                String fullName = scorecard.getOwner() != null ? scorecard.getOwner().getFullName() : "";
                ownerMatches = fullName != null && fullName.toLowerCase().contains(ownerFilter);
            }

            boolean approvalMatches = approvalFilter.isEmpty();
            if (!approvalMatches) {
                String status = scorecard.getApprovalStatus();
                approvalMatches = status != null && status.equalsIgnoreCase(approvalFilter);
            }

            if (ownerMatches && approvalMatches) {
                filteredScorecards.add(scorecard);
            }
        }

        return filteredScorecards;
    }

    private Map<Long, Boolean> buildCaptureScoresAllowedMap(List<Scorecard> scorecards) {
        Map<Long, Boolean> map = new HashMap<Long, Boolean>();
        if (scorecards == null || scorecards.isEmpty()) {
            return map;
        }
        ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
        boolean captureWindowOpen = reportingDateService.isReportingDateOpen(reportingDate);
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getId() <= 0) {
                continue;
            }
            String reportingDateRole = captureWindowOpen ? resolveReportingDateRole(scorecard, reportingDate) : null;
            boolean canCapture = captureWindowOpen
                    && isScoreCaptureWindowOpen(scorecard, reportingDate)
                    && PMConstants.STATUS_ACTIVE.equalsIgnoreCase(scorecard.getStatus())
                    && isAnyScoreCaptureAllowed(scorecard, reportingDateRole);
            map.put(scorecard.getId(), canCapture);
        }
        return map;
    }

    private Map<Long, Boolean> buildEditTargetsAllowedMap(List<Scorecard> scorecards) {
        Map<Long, Boolean> map = new HashMap<Long, Boolean>();
        if (scorecards == null || scorecards.isEmpty()) {
            return map;
        }
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getId() <= 0) {
                continue;
            }
            map.put(scorecard.getId(), canCaptureTargets(scorecard));
        }
        return map;
    }

    private Map<Long, Boolean> buildViewReportAllowedMap(List<Scorecard> scorecards) {
        Map<Long, Boolean> map = new HashMap<Long, Boolean>();
        if (scorecards == null || scorecards.isEmpty()) {
            return map;
        }
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getId() <= 0) {
                continue;
            }
            map.put(scorecard.getId(), canViewScorecardReport(scorecard));
        }
        return map;
    }

    @RequestMapping(value = "/view-user-scorecards/{id}")
    public ModelAndView viewUserScorecards(@PathVariable("id") Long userId, HttpServletRequest request, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_USER_SCORECARDS);
        Account owner = accountService.getAccountById(userId);
        modelAndView.addObject("pageTitle", "View " + owner.getFullName() + "'s Scorecards");
        List<Scorecard> scorecards = scorecardService.getScorecardsByOwner(owner);

        modelAndView.addObject("scorecards", scorecards);
        modelAndView.addObject("unmappedWorkflowScorecardCount", countUnmappedWorkflowScorecards(scorecards));
        modelAndView.addObject("scorecardCaptureScoresAllowed", buildCaptureScoresAllowedMap(scorecards));
        modelAndView.addObject("scorecardEditTargetsAllowed", buildEditTargetsAllowedMap(scorecards));
        modelAndView.addObject("scorecardViewReportAllowed", buildViewReportAllowedMap(scorecards));
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    private int countUnmappedWorkflowScorecards(List<Scorecard> scorecards) {
        if (scorecards == null || scorecards.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Scorecard scorecard : scorecards) {
            if (scorecard != null && scorecard.getApprovalStage() == null) {
                count++;
            }
        }
        return count;
    }

    @RequestMapping("/add-scorecard")
    public ModelAndView addAScorecard(HttpServletRequest request, HttpSession session) {
        ScorecardModel scorecardModel = scorecardModelService.getActiveScorecardModel();
        ModelAndView modelAndView = new ModelAndView(Pages.ADD_SCORECARD);
        modelAndView.addObject("pageTitle", "New Scorecard");
        modelAndView.addObject("scorecard", new Scorecard());
        modelAndView.addObject("scorecardModel", scorecardModel);
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping(value = "/save-scorecard", method = RequestMethod.POST)
    public String saveScorecard(HttpServletRequest request, Scorecard newScorecard) throws UnsupportedEncodingException {
        if (newScorecard == null
                || newScorecard.getOwner() == null
                || newScorecard.getOwner().getId() <= 0) {
            PortletUtils.addErrorMsg("Validation failed: Owner is required", request);
            return "redirect:/scorecards/add-scorecard";
        }
        if (newScorecard.getReportingPeriod() == null
                || newScorecard.getReportingPeriod().getId() <= 0) {
            PortletUtils.addErrorMsg("Validation failed: Reporting period is required", request);
            return "redirect:/scorecards/add-scorecard";
        }
        if (newScorecard.getScorecardModel() == null
                || newScorecard.getScorecardModel().getId() <= 0) {
            PortletUtils.addErrorMsg("Validation failed: Scorecard model is required", request);
            return "redirect:/scorecards/add-scorecard";
        }

        Account loggedUser = commonService.getLoggedUser();
        if (loggedUser == null) {
            PortletUtils.addErrorMsg("Validation failed: Unable to identify the logged-in user.", request);
            return "redirect:/scorecards/add-scorecard";
        }

        Account owner = accountService.getAccountById(newScorecard.getOwner().getId());
        if (owner == null) {
            PortletUtils.addErrorMsg("Validation failed: Selected owner could not be found.", request);
            return "redirect:/scorecards/add-scorecard";
        }
        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(newScorecard.getReportingPeriod().getId());
        if (reportingPeriod == null) {
            PortletUtils.addErrorMsg("Validation failed: Selected reporting period could not be found.", request);
            return "redirect:/scorecards/add-scorecard";
        }
        ScorecardModel scorecardModel = scorecardModelService.getScorecardModelById(newScorecard.getScorecardModel().getId());
        if (scorecardModel == null) {
            PortletUtils.addErrorMsg("Validation failed: Selected scorecard model could not be found.", request);
            return "redirect:/scorecards/add-scorecard";
        }
        newScorecard.setOwner(owner);
        newScorecard.setReportingPeriod(reportingPeriod);
        newScorecard.setScorecardModel(scorecardModel);

        if(scorecardService.countActiveScorecards(newScorecard.getOwner(), newScorecard.getReportingPeriod()) >= 1){
            PortletUtils.addErrorMsg(newScorecard.getOwner().getFullName() + " already has an active scorecard for the selected reporting period (" + newScorecard.getReportingPeriod().getStartDate() +" - "+ newScorecard.getReportingPeriod().getEndDate() +")", request);
            return "redirect:/scorecards/add-scorecard";
        }else {
            ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
            newScorecard.setClient(commonService.getConfiguredClient());
            newScorecard.setLockStatus(PMConstants.LOCK_STATUS_OPEN);
            newScorecard.setStatus(PMConstants.STATUS_ACTIVE);
            newScorecard.setApprovalStatus(workflow.getNewStatus());
            try {
                scorecardService.addScorecard(newScorecard);
            } catch (IllegalArgumentException | hr.performancemanagement.exception.custom.InvalidWorkflowStateException exception) {
                PortletUtils.addErrorMsg(exception.getMessage(), request);
                return "redirect:/scorecards/add-scorecard";
            }

            String recipient = newScorecard.getOwner().getEmail();
            String subject = "Scorecard Approval,";
            String template = "Good day, \n\n"
                    + "Please note that your scorecard has been successfully created. "
                    + "You can now login and approve\n\n";
            try {
                sendScorecardEmail(request, recipient, subject, template);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }


            if(newScorecard.getOwner().getId() == loggedUser.getId()){
                PortletUtils.addInfoMsg("Scorecard successfully created. You can proceed with capturing targets", request);
                return "redirect:/scorecards/capture-targets/"+ newScorecard.getId();
            }else {
                PortletUtils.addInfoMsg("Scorecard successfully created. You can proceed with creating other scorecards", request);
                return "redirect:/scorecards/add-scorecard";
            }
        }
    }

    @RequestMapping("/capture-targets/{id}")
    public ModelAndView captureTargets(@PathVariable("id") long id, HttpServletRequest request, HttpSession session) {

        Scorecard scorecard = scorecardService.getScorecardById(id);
        if (scorecard == null) {
            PortletUtils.addErrorMsg("Scorecard not found.", request);
            ModelAndView modelAndView = new ModelAndView(Pages.BLANK_PAGE);
            preparePage(modelAndView, request, session);
            return modelAndView;
        }
        String scorecardModel = scorecard.getScorecardModel().getName();
        String hierarchyModel = resolveHierarchyModel(scorecard);
        long reportingPeriodId = scorecard.getReportingPeriod().getId();
        List<Goal> GOALS_LIST = goalService.listAllGoals(id);
        List<StrategicObjective> STRATEGIC_OBJECTIVES_LIST = strategicObjectiveService.listAllStrategicObjectives(reportingPeriodId);
        ModelAndView modelAndView;

        if(canCaptureTargets(scorecard)){

            modelAndView = new ModelAndView(Pages.CAPTURE_TARGETS);
            modelAndView.addObject("pageTitle", "Capture Targets {"+ scorecard.getOwner().getFullName() +"}");
            modelAndView.addObject("scorecard", scorecard);
            modelAndView.addObject("id", scorecard.getId());
            modelAndView.addObject("output", new OutputWrapper());
            modelAndView.addObject("goalsList", GOALS_LIST);
            modelAndView.addObject("strategicObjectivesList", STRATEGIC_OBJECTIVES_LIST);
            modelAndView.addObject("scorecardModel", scorecardModel);
            List<Target> targetsList;
            if (isLegacyHierarchyModel(hierarchyModel)) {
                List<Gear> gears = gearService.listAllGears(commonService.getConfiguredClientId());
                List<Gear> selectedGears = gearService.listSelectedGears(scorecard);
                List<Gear> remainingGears = gearService.listRemainingGears(scorecard);
                targetsList = attachLegacyTargets(scorecard, selectedGears);
                mergeHydratedTargetSnapshots(scorecard.getId(), targetsList);
                modelAndView.addObject("selectedGears", selectedGears);
                modelAndView.addObject("remainingGears", remainingGears);
                modelAndView.addObject("gears", gears);
                modelAndView.addObject("unitsList", targetService.listAllUnits());
                modelAndView.addObject("totalAllocatedWeight", outcomeService.getTotalAllocatedWeight(id));
            } else {
                targetsList = targetService.getAllTargetsByScorecard(id);
                modelAndView.addObject("totalAllocatedWeight", goalService.getTotalAllocatedWeight(id));
            }
            modelAndView.addObject("targetsList", targetsList);
            modelAndView.addObject("targetRows", buildTargetCaptureRows(targetsList));

        } else {

            modelAndView = new ModelAndView(Pages.BLANK_PAGE);
            PortletUtils.addErrorMsg("Targets can only be captured for an active reporting period.", request);
        }
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    private String resolveHierarchyModel(Scorecard scorecard) {
        if (scorecard != null
                && scorecard.getReportingPeriod() != null
                && StringUtils.hasText(scorecard.getReportingPeriod().getModel())) {
            return scorecard.getReportingPeriod().getModel();
        }
        return "standard";
    }

    private boolean isLegacyHierarchyModel(String hierarchyModel) {
        return "gear".equalsIgnoreCase(hierarchyModel) || "programme".equalsIgnoreCase(hierarchyModel);
    }

    private List<Target> attachLegacyTargets(Scorecard scorecard, List<Gear> selectedGears) {
        List<Target> targetsList = new ArrayList<Target>();
        if (scorecard == null) {
            return targetsList;
        }
        String hierarchyModel = resolveHierarchyModel(scorecard);

        List<Output> outputs = outputService.listAllOutputs(scorecard);
        Map<Long, Gear> selectedById = new HashMap<Long, Gear>();
        if (selectedGears != null) {
            for (Gear gear : selectedGears) {
                gear.setTargetsList(new ArrayList<Target>());
                selectedById.put(gear.getId(), gear);
            }
        }

        for (Output output : outputs) {
            if (output == null || output.getOutcome() == null || output.getTargets() == null) {
                continue;
            }
            Outcome outcome = output.getOutcome();
            Goal goal = resolveGoalFromOutcome(outcome);
            Gear gear = goal == null ? null : goal.getGear();
            if (gear == null) {
                continue;
            }

            for (Target target : output.getTargets()) {
                if (target == null) {
                    continue;
                }
                target.setOutcome(outcome);
                target.setGear(gear);
                targetsList.add(target);

                Gear selectedGear = selectedById.get(gear.getId());
                if (selectedGear != null) {
                    List<Target> gearTargets = selectedGear.getTargetsList();
                    if (gearTargets == null) {
                        gearTargets = new ArrayList<Target>();
                        selectedGear.setTargetsList(gearTargets);
                    }
                    gearTargets.add(target);
                }
            }
        }

        if (selectedGears != null) {
            for (Gear gear : selectedGears) {
                if (gear.getTargetsList() == null) {
                    continue;
                }
                if ("programme".equalsIgnoreCase(hierarchyModel)) {
                    gear.getTargetsList().sort(Comparator
                            .comparingLong((Target target) -> strategicGoalId(target))
                            .thenComparingLong(target -> pillarId(target))
                            .thenComparingLong(target -> outcomeId(target))
                            .thenComparingLong(target -> outputId(target))
                            .thenComparingLong(Target::getId));
                } else {
                    gear.getTargetsList().sort(Comparator
                            .comparingLong((Target target) -> outcomeId(target))
                            .thenComparingLong(target -> outputId(target))
                            .thenComparingLong(Target::getId));
                }
            }
        }

        return targetsList;
    }

    private Goal resolveGoalFromOutcome(Outcome outcome) {
        if (outcome == null) {
            return null;
        }
        if (outcome.getGoal() != null) {
            return outcome.getGoal();
        }
        if (outcome.getPillar() != null) {
            return outcome.getPillar().getGoal();
        }
        return null;
    }

    private List<TargetCaptureRow> buildTargetCaptureRows(List<Target> targetsList) {
        List<TargetCaptureRow> rows = new ArrayList<TargetCaptureRow>();
        if (targetsList == null || targetsList.isEmpty()) {
            return rows;
        }

        for (Target target : targetsList) {
            rows.add(new TargetCaptureRow(target));
        }

        applyPerspectiveRowspans(targetsList, rows);
        applyGoalRowspans(targetsList, rows);
        return rows;
    }

    private void applyPerspectiveRowspans(List<Target> targetsList, List<TargetCaptureRow> rows) {
        int index = 0;
        while (index < targetsList.size()) {
            long currentPerspectiveId = perspectiveId(targetsList.get(index));
            int end = index + 1;
            while (end < targetsList.size() && perspectiveId(targetsList.get(end)) == currentPerspectiveId) {
                end++;
            }
            TargetCaptureRow row = rows.get(index);
            row.setShowPerspective(true);
            row.setPerspectiveRowspan(end - index);
            index = end;
        }
    }

    private void applyStrategicObjectiveRowspans(List<Target> targetsList, List<TargetCaptureRow> rows) {
        int index = 0;
        while (index < targetsList.size()) {
            long currentPerspectiveId = perspectiveId(targetsList.get(index));
            long currentStrategicObjectiveId = strategicObjectiveId(targetsList.get(index));
            int end = index + 1;
            while (end < targetsList.size()
                    && perspectiveId(targetsList.get(end)) == currentPerspectiveId
                    && strategicObjectiveId(targetsList.get(end)) == currentStrategicObjectiveId) {
                end++;
            }
            TargetCaptureRow row = rows.get(index);
            row.setShowStrategicObjective(true);
            row.setStrategicObjectiveRowspan(end - index);
            index = end;
        }
    }

    private void applyGoalRowspans(List<Target> targetsList, List<TargetCaptureRow> rows) {
        int index = 0;
        while (index < targetsList.size()) {
            long currentGoalId = goalId(targetsList.get(index));
            int end = index + 1;
            while (end < targetsList.size() && goalId(targetsList.get(end)) == currentGoalId) {
                end++;
            }
            TargetCaptureRow row = rows.get(index);
            row.setShowGoal(true);
            row.setGoalRowspan(end - index);
            index = end;
        }
    }

    private long perspectiveId(Target target) {
        if (target == null || target.getPerspective() == null) {
            return -1;
        }
        return target.getPerspective().getId();
    }

    private long strategicObjectiveId(Target target) {
        if (target == null || target.getStrategicObjective() == null) {
            return -1;
        }
        return target.getStrategicObjective().getId();
    }

    private long goalId(Target target) {
        if (target == null || target.getGoal() == null) {
            return -1;
        }
        return target.getGoal().getId();
    }

    private void addScorecardDisplayModel(ModelAndView modelAndView, Scorecard scorecard, List<Target> targetsList) {
        addScorecardDisplayModel(modelAndView, scorecard, targetsList, Collections.emptyList());
    }

    private void addScorecardDisplayModel(ModelAndView modelAndView, Scorecard scorecard, List<Target> targetsList, List<ReportingDate> reportingDates) {
        String hierarchyModel = resolveHierarchyModel(scorecard);
        List<ScorecardDisplaySection> displaySections = buildDisplaySections(scorecard, hierarchyModel, targetsList);
        hydrateDisplayScores(displaySections, reportingDates, hierarchyModel);
        modelAndView.addObject("displaySections", displaySections);

        if ("programme".equalsIgnoreCase(hierarchyModel)) {
            modelAndView.addObject("displayStage2Label", "Strategic Goal");
            modelAndView.addObject("displayStage3Label", "Pillar");
            modelAndView.addObject("displayStage4Label", "Outcome");
            modelAndView.addObject("displayShowStage3", true);
            modelAndView.addObject("displayShowStage4", true);
            modelAndView.addObject("displayShowOutput", true);
            modelAndView.addObject("displayHierarchyColumnCount", 4);
        } else if ("gear".equalsIgnoreCase(hierarchyModel)) {
            modelAndView.addObject("displayStage2Label", "Outcome");
            modelAndView.addObject("displayStage3Label", "");
            modelAndView.addObject("displayStage4Label", "");
            modelAndView.addObject("displayShowStage3", false);
            modelAndView.addObject("displayShowStage4", false);
            modelAndView.addObject("displayShowOutput", true);
            modelAndView.addObject("displayHierarchyColumnCount", 2);
        } else {
            modelAndView.addObject("displayStage2Label", "Goal");
            modelAndView.addObject("displayStage3Label", "");
            modelAndView.addObject("displayStage4Label", "");
            modelAndView.addObject("displayShowStage3", false);
            modelAndView.addObject("displayShowStage4", false);
            modelAndView.addObject("displayShowOutput", false);
            modelAndView.addObject("displayHierarchyColumnCount", 1);
        }
    }

    private void hydrateDisplayScores(List<ScorecardDisplaySection> displaySections, List<ReportingDate> reportingDates, String hierarchyModel) {
        if (displaySections == null || displaySections.isEmpty()
                || reportingDates == null || reportingDates.isEmpty()) {
            return;
        }
        boolean legacyHierarchy = isLegacyHierarchyModel(hierarchyModel);
        for (ScorecardDisplaySection section : displaySections) {
            if (section == null || section.getRows() == null) {
                continue;
            }
            for (ScorecardDisplayRow row : section.getRows()) {
                if (row == null || row.getTarget() == null) {
                    continue;
                }
                for (ReportingDate reportingDate : reportingDates) {
                    if (reportingDate == null || reportingDate.getId() <= 0) {
                        continue;
                    }
                    Score score = legacyHierarchy
                            ? resolveOutputReportingDateScore(row.getTarget(), reportingDate)
                            : resolveTargetReportingDateScore(row.getTarget(), reportingDate);
                    if (score != null) {
                        row.getScoresByReportingDate().put(reportingDate.getId(), score);
                    }
                    Evidence evidence = resolveLatestEvidence(row.getTarget(), reportingDate);
                    if (evidence != null) {
                        row.getEvidenceByReportingDate().put(reportingDate.getId(), evidence);
                    }
                }
                row.setScoreHistory(resolveScoreHistory(row.getTarget()));
                row.setComments(commentRepository.findCommentsByTarget(row.getTarget()));
            }
        }
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

    private Score resolveTargetReportingDateScore(Target target, ReportingDate reportingDate) {
        if (target == null || reportingDate == null) {
            return null;
        }
        List<Score> scores = scoreRepository.findScoresByTargetAndReportingDateOrderByIdDesc(target, reportingDate);
        if (scores != null && !scores.isEmpty()) {
            return scores.get(0);
        }
        return resolveOutputReportingDateScore(target, reportingDate);
    }

    private Score resolveOutputReportingDateScore(Target target, ReportingDate reportingDate) {
        if (target == null || target.getOutput() == null || reportingDate == null) {
            return null;
        }
        List<Score> scores = scoreRepository.findScoresByOutputAndReportingDateOrderByIdDesc(target.getOutput(), reportingDate);
        if (scores == null || scores.isEmpty()) {
            return null;
        }

        for (Score score : scores) {
            if (score != null && score.getTarget() == null) {
                return score;
            }
        }
        for (Score score : scores) {
            if (isScoreForTarget(score, target)) {
                return score;
            }
        }
        return scores.get(0);
    }

    private boolean isScoreForTarget(Score score, Target target) {
        return score != null
                && score.getTarget() != null
                && target != null
                && score.getTarget().getId() == target.getId();
    }

    private Map<Long, OverallScore> buildOverallScoresByReportingDate(Scorecard scorecard, List<ReportingDate> reportingDates) {
        Map<Long, OverallScore> overallScoresByReportingDate = new HashMap<Long, OverallScore>();
        if (scorecard == null || reportingDates == null || reportingDates.isEmpty()) {
            return overallScoresByReportingDate;
        }
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate == null || reportingDate.getId() <= 0) {
                continue;
            }
            overallScoresByReportingDate.put(
                    reportingDate.getId(),
                    overallScoreService.getOverallScoreByScorecardAndReportingDate(scorecard, reportingDate)
            );
        }
        return overallScoresByReportingDate;
    }

    private Map<Long, OverallComment> buildOverallCommentsByReportingDate(Scorecard scorecard) {
        Map<Long, OverallComment> commentsByReportingDate = new HashMap<Long, OverallComment>();
        if (scorecard == null) {
            return commentsByReportingDate;
        }
        List<OverallComment> comments = overallCommentService.getOverallCommentsByScorecard(scorecard);
        if (comments == null || comments.isEmpty()) {
            return commentsByReportingDate;
        }
        for (OverallComment comment : comments) {
            if (comment != null && comment.getReportingDate() != null && comment.getReportingDate().getId() > 0) {
                commentsByReportingDate.put(comment.getReportingDate().getId(), comment);
            }
        }
        return commentsByReportingDate;
    }

    private Map<Long, Double> buildDisplayWeightedScoreTotalsByReportingDate(List<ScorecardDisplaySection> displaySections, List<ReportingDate> reportingDates, String hierarchyModel) {
        Map<Long, Double> totalsByReportingDate = new HashMap<Long, Double>();
        if (reportingDates == null || reportingDates.isEmpty()) {
            return totalsByReportingDate;
        }
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate != null && reportingDate.getId() > 0) {
                totalsByReportingDate.put(reportingDate.getId(), 0.0);
            }
        }
        if (displaySections == null || displaySections.isEmpty()) {
            return totalsByReportingDate;
        }

        boolean legacyHierarchy = isLegacyHierarchyModel(hierarchyModel);
        for (ScorecardDisplaySection section : displaySections) {
            if (section == null || section.getRows() == null) {
                continue;
            }
            for (ScorecardDisplayRow row : section.getRows()) {
                if (row == null || (legacyHierarchy && !row.isShowOutput())) {
                    continue;
                }
                for (ReportingDate reportingDate : reportingDates) {
                    if (reportingDate == null || reportingDate.getId() <= 0) {
                        continue;
                    }
                    Score score = row.getScoresByReportingDate().get(reportingDate.getId());
                    if (score == null) {
                        continue;
                    }
                    Double total = totalsByReportingDate.get(reportingDate.getId());
                    totalsByReportingDate.put(reportingDate.getId(), (total == null ? 0.0 : total) + score.getWeightedScore());
                }
            }
        }
        return totalsByReportingDate;
    }

    private int resolveViewScoreColumnCount(String scorecardModel) {
        return PMConstants.VALUE_BASED.equalsIgnoreCase(scorecardModel) ? 3 : 2;
    }

    private int resolveViewTableColumnCount(int displayHierarchyColumnCount, String scorecardModel) {
        return displayHierarchyColumnCount + 5 + resolveViewScoreColumnCount(scorecardModel) + 3;
    }

    private int resolveViewSummaryLabelColspan(int displayHierarchyColumnCount) {
        return displayHierarchyColumnCount + 4;
    }

    private Long resolveDefaultReportingDateId(List<ReportingDate> reportingDates) {
        if (reportingDates == null || reportingDates.isEmpty()) {
            return null;
        }
        ReportingDate activeReportingDate = reportingDateService.getActiveReportingDate();
        if (activeReportingDate != null && isReportingDateInList(activeReportingDate, reportingDates)) {
            return activeReportingDate.getId();
        }
        ReportingDate finalReportingDate = resolveFinalReportingDate(reportingDates);
        if (finalReportingDate != null) {
            return finalReportingDate.getId();
        }
        return reportingDates.get(0) == null ? null : reportingDates.get(0).getId();
    }

    private boolean isReportingDateInList(ReportingDate candidate, List<ReportingDate> reportingDates) {
        if (candidate == null || reportingDates == null) {
            return false;
        }
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate != null && reportingDate.getId() == candidate.getId()) {
                return true;
            }
        }
        return false;
    }

    private ReportingDate resolveFinalReportingDate(List<ReportingDate> reportingDates) {
        ReportingDate selected = null;
        LocalDate selectedDate = null;
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate == null) {
                continue;
            }
            LocalDate candidateDate = parseReportingDate(reportingDate.getEndDate());
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

    private LocalDate parseReportingDate(String value) {
        if (!StringUtils.hasText(value)) {
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

    private int resolveDisplayHierarchyColumnCount(String hierarchyModel) {
        if ("programme".equalsIgnoreCase(hierarchyModel)) {
            return 4;
        }
        if ("gear".equalsIgnoreCase(hierarchyModel)) {
            return 2;
        }
        return 1;
    }

    private List<ScorecardDisplaySection> buildDisplaySections(Scorecard scorecard, String hierarchyModel, List<Target> targetsList) {
        List<ScorecardDisplaySection> sections = new ArrayList<ScorecardDisplaySection>();
        if (targetsList == null || targetsList.isEmpty()) {
            return sections;
        }

        if (isLegacyHierarchyModel(hierarchyModel)) {
            List<Gear> selectedGears = gearService.listSelectedGears(scorecard);
            attachLegacyTargets(scorecard, selectedGears);
            if (selectedGears != null) {
                for (Gear gear : selectedGears) {
                    if (gear == null || gear.getTargetsList() == null || gear.getTargetsList().isEmpty()) {
                        continue;
                    }
                    mergeHydratedTargetSnapshots(scorecard.getId(), gear.getTargetsList());
                }
            }
            for (Gear gear : selectedGears) {
                if (gear == null || gear.getTargetsList() == null || gear.getTargetsList().isEmpty()) {
                    continue;
                }
                ScorecardDisplaySection section = new ScorecardDisplaySection();
                section.setSectionName(gear.getName());
                section.setSectionWeight(gear.getTotalAllocatedWeight());
                section.setRows(buildLegacyDisplayRows(gear.getTargetsList(), hierarchyModel));
                sections.add(section);
            }
            return sections;
        }

        int index = 0;
        while (index < targetsList.size()) {
            Target first = targetsList.get(index);
            long currentPerspectiveId = perspectiveId(first);
            int end = index + 1;
            while (end < targetsList.size() && perspectiveId(targetsList.get(end)) == currentPerspectiveId) {
                end++;
            }

            List<Target> sectionTargets = targetsList.subList(index, end);
            ScorecardDisplaySection section = new ScorecardDisplaySection();
            section.setSectionName(first != null && first.getPerspective() != null ? first.getPerspective().getName() : "Unassigned Perspective");
            section.setSectionWeight(sumSectionWeight(sectionTargets));
            section.setRows(buildStandardDisplayRows(sectionTargets));
            sections.add(section);
            index = end;
        }

        return sections;
    }

    private double sumSectionWeight(List<Target> targets) {
        if (targets == null) {
            return 0.0;
        }
        double total = 0.0;
        for (Target target : targets) {
            if (target != null && target.getAllocatedWeight() != null) {
                total += target.getAllocatedWeight();
            }
        }
        return total;
    }

    private List<ScorecardDisplayRow> buildStandardDisplayRows(List<Target> targets) {
        List<ScorecardDisplayRow> rows = new ArrayList<ScorecardDisplayRow>();
        if (targets == null || targets.isEmpty()) {
            return rows;
        }
        for (Target target : targets) {
            ScorecardDisplayRow row = new ScorecardDisplayRow(target);
            row.setStage2Value(target != null && target.getGoal() != null ? target.getGoal().getName() : "");
            row.setShowStage3(false);
            row.setShowOutput(false);
            row.setShowStage4(false);
            rows.add(row);
        }
        applyStandardDisplayRowspans(targets, rows);
        return rows;
    }

    private void applyStandardDisplayRowspans(List<Target> targets, List<ScorecardDisplayRow> rows) {
        int index = 0;
        while (index < targets.size()) {
            long goalId = goalId(targets.get(index));
            int end = index + 1;
            while (end < targets.size() && goalId(targets.get(end)) == goalId) {
                end++;
            }
            ScorecardDisplayRow row = rows.get(index);
            row.setShowStage2(true);
            row.setStage2Rowspan(end - index);
            for (int duplicateIndex = index + 1; duplicateIndex < end; duplicateIndex++) {
                rows.get(duplicateIndex).setShowStage2(false);
            }
            index = end;
        }
    }

    private List<ScorecardDisplayRow> buildLegacyDisplayRows(List<Target> targets, String hierarchyModel) {
        List<ScorecardDisplayRow> rows = new ArrayList<ScorecardDisplayRow>();
        if (targets == null || targets.isEmpty()) {
            return rows;
        }
        for (Target target : targets) {
            ScorecardDisplayRow row = new ScorecardDisplayRow(target);
            Outcome outcome = target == null ? null : target.getOutcome();
            if ("programme".equalsIgnoreCase(hierarchyModel)) {
                String level2 = "";
                String level3 = "";
                String level4 = "";
                if (outcome != null && outcome.getPillar() != null) {
                    Pillar pillar = outcome.getPillar();
                    level2 = pillar.getGoal() != null ? pillar.getGoal().getName() : "";
                    level3 = pillar.getName();
                    level4 = outcome.getName();
                }
                row.setStage2Value(level2);
                row.setStage3Value(level3);
                row.setStage4Value(level4);
                row.setShowStage3(true);
                row.setShowStage4(true);
            } else {
                row.setStage2Value(outcome != null ? outcome.getName() : "");
                row.setShowStage3(false);
                row.setShowStage4(false);
            }
            row.setOutputValue(target != null && target.getOutput() != null ? target.getOutput().getName() : "");
            row.setShowOutput(true);
            rows.add(row);
        }
        applyLegacyDisplayRowspans(targets, rows, hierarchyModel);
        return rows;
    }

    private void applyLegacyDisplayRowspans(List<Target> targets, List<ScorecardDisplayRow> rows, String hierarchyModel) {
        int index = 0;
        while (index < targets.size()) {
            String key = stage2Key(targets.get(index), hierarchyModel);
            int end = index + 1;
            while (end < targets.size() && key.equals(stage2Key(targets.get(end), hierarchyModel))) {
                end++;
            }
            ScorecardDisplayRow row = rows.get(index);
            row.setShowStage2(true);
            row.setStage2Rowspan(end - index);
            for (int duplicateIndex = index + 1; duplicateIndex < end; duplicateIndex++) {
                rows.get(duplicateIndex).setShowStage2(false);
            }
            index = end;
        }

        if ("programme".equalsIgnoreCase(hierarchyModel)) {
            index = 0;
            while (index < targets.size()) {
                String key = stage3Key(targets.get(index));
                int end = index + 1;
                while (end < targets.size() && key.equals(stage3Key(targets.get(end)))) {
                    end++;
                }
                ScorecardDisplayRow row = rows.get(index);
                row.setShowStage3(true);
                row.setStage3Rowspan(end - index);
                for (int duplicateIndex = index + 1; duplicateIndex < end; duplicateIndex++) {
                    rows.get(duplicateIndex).setShowStage3(false);
                }
                index = end;
            }

            index = 0;
            while (index < targets.size()) {
                long currentOutcomeId = outcomeId(targets.get(index));
                int end = index + 1;
                while (end < targets.size() && outcomeId(targets.get(end)) == currentOutcomeId) {
                    end++;
                }
                ScorecardDisplayRow row = rows.get(index);
                row.setShowStage4(true);
                row.setStage4Rowspan(end - index);
                for (int duplicateIndex = index + 1; duplicateIndex < end; duplicateIndex++) {
                    rows.get(duplicateIndex).setShowStage4(false);
                }
                index = end;
            }
        }

        index = 0;
        while (index < targets.size()) {
            long currentOutputId = outputId(targets.get(index));
            long currentOutcomeId = outcomeId(targets.get(index));
            int end = index + 1;
            while (end < targets.size()
                    && outputId(targets.get(end)) == currentOutputId
                    && outcomeId(targets.get(end)) == currentOutcomeId) {
                end++;
            }
            ScorecardDisplayRow row = rows.get(index);
            row.setShowOutput(true);
            row.setOutputRowspan(end - index);
            for (int duplicateIndex = index + 1; duplicateIndex < end; duplicateIndex++) {
                rows.get(duplicateIndex).setShowOutput(false);
            }
            index = end;
        }
    }

    private String stage2Key(Target target, String hierarchyModel) {
        if (target == null) {
            return "-1";
        }
        Outcome outcome = target.getOutcome();
        if ("programme".equalsIgnoreCase(hierarchyModel)) {
            if (outcome != null && outcome.getPillar() != null && outcome.getPillar().getGoal() != null) {
                return String.valueOf(outcome.getPillar().getGoal().getId());
            }
            return "-1";
        }
        return String.valueOf(outcomeId(target));
    }

    private String stage3Key(Target target) {
        if (target == null || target.getOutcome() == null || target.getOutcome().getPillar() == null) {
            return "-1";
        }
        Pillar pillar = target.getOutcome().getPillar();
        Goal goal = pillar.getGoal();
        return (goal == null ? "-1" : goal.getId()) + ":" + pillar.getId();
    }

    private long outputId(Target target) {
        if (target == null || target.getOutput() == null) {
            return -1;
        }
        return target.getOutput().getId();
    }

    private long outcomeId(Target target) {
        if (target == null || target.getOutcome() == null) {
            return -1;
        }
        return target.getOutcome().getId();
    }

    private long pillarId(Target target) {
        if (target == null || target.getOutcome() == null || target.getOutcome().getPillar() == null) {
            return -1;
        }
        return target.getOutcome().getPillar().getId();
    }

    private long strategicGoalId(Target target) {
        if (target == null || target.getOutcome() == null || target.getOutcome().getPillar() == null
                || target.getOutcome().getPillar().getGoal() == null) {
            return -1;
        }
        return target.getOutcome().getPillar().getGoal().getId();
    }

    private void mergeHydratedTargetSnapshots(long scorecardId, List<Target> targetsList) {
        if (scorecardId <= 0 || targetsList == null || targetsList.isEmpty()) {
            return;
        }

        for (Target target : targetsList) {
            if (target == null || target.getId() <= 0) {
                continue;
            }
            hydrateTargetScoreSnapshot(target);
        }
    }

    private void hydrateTargetScoreSnapshot(Target target) {
        if (target == null) {
            return;
        }
        Output output = target.getOutput();

        Object[] standardAggregates = scoreRepository.aggregateStandardTargetScores(target, output);
        double weightedScore = toDouble(aggregateValue(standardAggregates, 0));
        double averageActual = toDouble(aggregateValue(standardAggregates, 1));
        double sumActual = toDouble(aggregateValue(standardAggregates, 2));
        target.setWeightedScore(weightedScore);
        if ("%".equalsIgnoreCase(target.getUnit())) {
            target.setActual(averageActual);
        } else {
            target.setActual(sumActual);
        }

        Object[] valueBasedAggregates = scoreRepository.aggregateValueBasedTargetScores(target, output);
        target.setEmployeeScore(toDouble(aggregateValue(valueBasedAggregates, 1)));
        target.setManagerScore(toDouble(aggregateValue(valueBasedAggregates, 2)));
        target.setAgreedScore(toDouble(aggregateValue(valueBasedAggregates, 3)));
        target.setModeratedScore(toDouble(aggregateValue(valueBasedAggregates, 4)));
    }

    private List<Score> resolveScoreHistory(Target target) {
        if (target == null) {
            return new ArrayList<Score>();
        }
        List<Score> targetHistory = scoreRepository.findScoresByTargetOrderByReportingDate_DateDescIdDesc(target);
        if (targetHistory != null && !targetHistory.isEmpty()) {
            return targetHistory;
        }
        if (target.getOutput() == null) {
            return new ArrayList<Score>();
        }
        List<Score> outputHistory = scoreRepository.findScoresByOutputOrderByReportingDate_DateDescIdDesc(target.getOutput());
        if (outputHistory == null || outputHistory.isEmpty()) {
            return new ArrayList<Score>();
        }
        List<Score> filteredHistory = new ArrayList<Score>();
        for (Score score : outputHistory) {
            if (score == null) {
                continue;
            }
            if (score.getTarget() == null || score.getTarget().getId() == target.getId()) {
                filteredHistory.add(score);
            }
        }
        return filteredHistory.isEmpty() ? outputHistory : filteredHistory;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private Object aggregateValue(Object[] aggregates, int index) {
        Object[] row = unwrapAggregateRow(aggregates);
        if (row == null || index < 0 || index >= row.length) {
            return null;
        }
        return row[index];
    }

    private Object[] unwrapAggregateRow(Object[] aggregates) {
        if (aggregates == null) {
            return null;
        }
        if (aggregates.length == 1 && aggregates[0] instanceof Object[]) {
            return (Object[]) aggregates[0];
        }
        return aggregates;
    }

    @RequestMapping("/capture-scores/{id}")
    public ModelAndView captureScores(@PathVariable("id") long id, HttpServletRequest request, HttpSession session) {

        Scorecard scorecard = scorecardService.getScorecardById(id);
        if (scorecard == null) {
            PortletUtils.addErrorMsg("Scorecard not found.", request);
            return new ModelAndView("redirect:/scorecards");
        }
        String scorecardModel = scorecard.getScorecardModel() != null
                ? scorecard.getScorecardModel().getName()
                : PMConstants.STANDARD_SCORECARD;
        String hierarchyModel = resolveHierarchyModel(scorecard);
        int displayHierarchyColumnCount = resolveDisplayHierarchyColumnCount(hierarchyModel);
        ReportingPeriod reportingPeriod = scorecard.getReportingPeriod();
        ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
        List<ReportingDate> reportingDates = reportingDateService.listAllReportingDates(reportingPeriod);
        double averageEmployeeScore = goalService.getAverageEmployeeScore(id);
        double averageManagerScore = goalService.getAverageManagerScore(id);
        double averageAgreedScore = goalService.getAverageAgreedScore(id);
        double averageModeratedScore = goalService.getAverageModeratorScore(id);
        double totalAllocatedWeight = isLegacyHierarchyModel(hierarchyModel)
                ? outcomeService.getTotalAllocatedWeight(id)
                : goalService.getTotalAllocatedWeight(id);

        double weightedScore;
        try {
            weightedScore = (averageModeratedScore / 5 ) * 100;
        }catch (Exception e){
            weightedScore = 0;
        }

        ModelAndView modelAndView = resolveCaptureScoresView(scorecard, scorecardModel, request);
        String scoreCaptureBlockedMessage = resolveScoreCaptureBlockedMessage(scorecard, reportingDate);
        boolean scoreCaptureAllowed = scoreCaptureBlockedMessage == null;
        if (!scoreCaptureAllowed && !Pages.BLANK_PAGE.equals(modelAndView.getViewName())) {
            PortletUtils.addErrorMsg(scoreCaptureBlockedMessage, request);
        }
        ValueBasedCaptureStage captureStage = resolveValueBasedCaptureStage(scorecard);
        if (captureStage != null) {
            modelAndView.addObject("url", captureStage.getSubmitUrl());
        }
        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        modelAndView.addObject("captureSubmitLabel", resolveCaptureSubmitLabel(captureStage, workflow));

            modelAndView.addObject("pageTitle", "Capture Scores");
            modelAndView.addObject("scorecard", scorecard);
            modelAndView.addObject("comment", new Comment());
            List<Target> targetsList;
            if (isLegacyHierarchyModel(hierarchyModel)) {
                List<Gear> selectedGears = gearService.listSelectedGears(scorecard);
                targetsList = attachLegacyTargets(scorecard, selectedGears);
                mergeHydratedTargetSnapshots(scorecard.getId(), targetsList);
                modelAndView.addObject("selectedGears", selectedGears);
            } else {
                targetsList = targetService.getAllTargetsByScorecard(id);
            }
            modelAndView.addObject("targetsList", targetsList);
            modelAndView.addObject("targetRows", buildTargetCaptureRows(targetsList));
            boolean selfAssessmentPipRequired = captureStage == ValueBasedCaptureStage.EMPLOYEE;
            modelAndView.addObject("selfAssessmentPipRequired", selfAssessmentPipRequired);
            modelAndView.addObject("selfAssessmentPips", selfAssessmentPipRequired
                    ? performanceImprovementPlanService.listSelfAssessmentPerformanceImprovementPlans(scorecard, reportingDate)
                    : Collections.emptyList());
            addScorecardDisplayModel(modelAndView, scorecard, targetsList, reportingDates);
            List<ScorecardDisplaySection> displaySections =
                    (List<ScorecardDisplaySection>) modelAndView.getModel().get("displaySections");
            Map<Long, Double> displayWeightedScoreTotalsByReportingDate =
                    buildDisplayWeightedScoreTotalsByReportingDate(displaySections, reportingDates, hierarchyModel);
            modelAndView.addObject("averageEmployeeScore", averageEmployeeScore);
            modelAndView.addObject("averageManagerScore", averageManagerScore);
            modelAndView.addObject("averageAgreedScore", averageAgreedScore);
            modelAndView.addObject("averageModeratedScore", averageModeratedScore);
            modelAndView.addObject("weightedScore", weightedScore);
            modelAndView.addObject("totalAllocatedWeight", totalAllocatedWeight);
            modelAndView.addObject("totalCaptureWeightedScore", reportingDateWeightedTotal(displayWeightedScoreTotalsByReportingDate, reportingDate));
            modelAndView.addObject("reportingDates", reportingDates);
            modelAndView.addObject("reportingDate", reportingDate);
            modelAndView.addObject("reportingDateLabel", resolveReportingDateLabel(reportingDate));
            modelAndView.addObject("scorecardModel", scorecardModel);
            modelAndView.addObject("reportingPeriod", reportingPeriod);
            modelAndView.addObject("overallScore", overallScoreService.getOverallScoreByScorecardAndReportingDate(scorecard, reportingDate));
            modelAndView.addObject(
                    "overallComment",
                    overallCommentService.getOverallCommentByScorecardAndReportingDate(scorecard, reportingDate)
            );
            modelAndView.addObject("scoreCaptureAllowed", scoreCaptureAllowed);
            modelAndView.addObject("scoreCaptureBlockedMessage", scoreCaptureBlockedMessage);
            modelAndView.addObject("activeReportingDateId", resolveDefaultReportingDateId(reportingDates));
            modelAndView.addObject("overallScoresByReportingDate", buildOverallScoresByReportingDate(scorecard, reportingDates));
            modelAndView.addObject("displayWeightedScoreTotalsByReportingDate", displayWeightedScoreTotalsByReportingDate);
            if (PMConstants.STANDARD_SCORECARD.equalsIgnoreCase(scorecardModel)) {
                modelAndView.addObject("captureViewSummaryLabelColspan", displayHierarchyColumnCount + 4);
                modelAndView.addObject("captureEntrySummaryLabelColspan", displayHierarchyColumnCount + 4);
                modelAndView.addObject("captureViewTableColumnCount", displayHierarchyColumnCount + 10);
                modelAndView.addObject("captureEntryTableColumnCount", displayHierarchyColumnCount + 9);
            } else if (PMConstants.VALUE_BASED.equalsIgnoreCase(scorecardModel) && captureStage != null) {
                modelAndView.addObject("captureViewSummaryLabelColspan", displayHierarchyColumnCount + 4);
                modelAndView.addObject("captureEntrySummaryLabelColspan", displayHierarchyColumnCount + 4);
                modelAndView.addObject("captureViewTableColumnCount", resolveValueBasedViewTableColumnCount(displayHierarchyColumnCount, captureStage));
                modelAndView.addObject("captureEntryTableColumnCount", resolveValueBasedCaptureTableColumnCount(displayHierarchyColumnCount, captureStage));
            }

        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    private ModelAndView resolveCaptureScoresView(Scorecard scorecard, String scorecardModel, HttpServletRequest request) {
        if (PMConstants.STANDARD_SCORECARD.equalsIgnoreCase(scorecardModel)) {
            return new ModelAndView(Pages.CAPTURE_SCORES_STANDARD);
        }
        if (PMConstants.VALUE_BASED.equalsIgnoreCase(scorecardModel)) {
            ValueBasedCaptureStage stage = resolveValueBasedCaptureStage(scorecard);
            if (stage != null) {
                ModelAndView modelAndView = new ModelAndView(stage.getPage());
                modelAndView.addObject("url", stage.getSubmitUrl());
                return modelAndView;
            }
            PortletUtils.addErrorMsg("You are not allowed to capture scores on this scorecard", request);
            return new ModelAndView(Pages.BLANK_PAGE);
        }

        PortletUtils.addErrorMsg("It shows like the scoring model is not defined. Contact the administrator", request);
        return new ModelAndView(Pages.BLANK_PAGE);
    }

    private ValueBasedCaptureStage resolveValueBasedCaptureStage(Scorecard scorecard) {
        for (ValueBasedCaptureStage stage : ValueBasedCaptureStage.values()) {
            if (commonService.isUserAllowed(stage.getRequiredActivity(), scorecard)) {
                return stage;
            }
        }
        return null;
    }

    private ReportingDate resolveActiveReportingDate() {
        ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
        if (reportingDate == null) {
            return null;
        }
        return reportingDate;
    }

    private boolean isScoreCaptureWindowOpen(Scorecard scorecard, ReportingDate reportingDate) {
        return reportingDateService.isReportingDateOpen(reportingDate)
                && isReportingDateForScorecard(scorecard, reportingDate);
    }

    private boolean isReportingDateForScorecard(Scorecard scorecard, ReportingDate reportingDate) {
        if (scorecard == null || scorecard.getReportingPeriod() == null || reportingDate == null
                || reportingDate.getReportingPeriod() == null) {
            return false;
        }
        return scorecard.getReportingPeriod().getId() == reportingDate.getReportingPeriod().getId();
    }

    private boolean isScorecardInActiveReportingPeriod(Scorecard scorecard) {
        return scorecard != null
                && scorecard.getReportingPeriod() != null
                && scorecard.getReportingPeriod().getStatus() != null
                && PMConstants.STATUS_ACTIVE.equalsIgnoreCase(scorecard.getReportingPeriod().getStatus().trim());
    }

    private String resolveActiveReportingDateRole(Scorecard scorecard) {
        if (scorecard == null) {
            return null;
        }
        ReportingDate reportingDate = resolveActiveReportingDate();
        return resolveReportingDateRole(scorecard, reportingDate);
    }

    private String resolveReportingDateRole(Scorecard scorecard, ReportingDate reportingDate) {
        if (scorecard == null) {
            return null;
        }
        if (reportingDate == null) {
            return null;
        }
        if (!isScoreCaptureWindowOpen(scorecard, reportingDate)) {
            return null;
        }
        String roleKey = scorecardReportingDateStageService.getCurrentRoleKey(scorecard, reportingDate);
        if (roleKey != null) {
            return roleKey;
        }
        ScorecardReportingDateStage stage = scorecardReportingDateStageService.getOrCreateStage(scorecard, reportingDate);
        if (stage == null || stage.getApprovalStage() == null) {
            return null;
        }
        return stage.getApprovalStage().getRoleKey();
    }

    private boolean moveActiveReportingDateStage(Scorecard scorecard, String roleKey, HttpServletRequest request) {
        ReportingDate reportingDate = resolveActiveReportingDate();
        if (reportingDate == null) {
            PortletUtils.addErrorMsg("No active reporting date was found.", request);
            return false;
        }
        if (!reportingDateService.isReportingDateOpen(reportingDate)) {
            PortletUtils.addErrorMsg("Scores can only be submitted for an OPEN reporting date.", request);
            return false;
        }
        if (!isReportingDateForScorecard(scorecard, reportingDate)) {
            PortletUtils.addErrorMsg("Scores can only be submitted for the OPEN reporting date in this scorecard's reporting period.", request);
            return false;
        }
        ScorecardReportingDateStage stage = scorecardReportingDateStageService.moveToRole(scorecard, reportingDate, roleKey);
        if (stage == null || stage.getApprovalStage() == null) {
            PortletUtils.addErrorMsg("The score workflow stage could not be updated.", request);
            return false;
        }
        return true;
    }

    private boolean validateValueBasedScoreSubmission(Scorecard scorecard,
                                                      ReportingDate reportingDate,
                                                      ValueBasedCaptureStage stage,
                                                      HttpServletRequest request) {
        String validationMessage = resolveValueBasedScoreSubmissionIssue(scorecard, reportingDate, stage);
        if (validationMessage == null) {
            return true;
        }
        PortletUtils.addErrorMsg(validationMessage, request);
        return false;
    }

    private String resolveValueBasedScoreSubmissionIssue(Scorecard scorecard,
                                                        ReportingDate reportingDate,
                                                        ValueBasedCaptureStage stage) {
        if (scorecard == null) {
            return "Scorecard not found.";
        }
        if (stage == null) {
            return "Score capture stage could not be resolved.";
        }
        if (hasReportingDateConflict(scorecard)) {
            return "Score capture is blocked: multiple OPEN/ACTIVE reporting dates exist. Contact an administrator.";
        }
        if (reportingDate == null) {
            return "No active reporting date was found.";
        }
        if (!reportingDateService.isReportingDateOpen(reportingDate)) {
            return "Scores can only be submitted for an OPEN reporting date.";
        }
        if (!isReportingDateForScorecard(scorecard, reportingDate)) {
            return "Scores can only be submitted for the OPEN reporting date in this scorecard's reporting period.";
        }
        if (!isStageCaptureAllowed(scorecard, stage)) {
            return "You are not allowed to submit " + scoreStageDisplayName(stage) + " scores for this scorecard.";
        }

        List<Target> targetsList = resolveTargetsForScoreSubmission(scorecard);
        if (targetsList.isEmpty()) {
            return "No targets are available for score capture.";
        }
        for (Target target : targetsList) {
            Score score = resolveTargetReportingDateScore(target, reportingDate);
            if (!hasValidCapturedScore(score, stage)) {
                return "Capture a valid " + scoreStageDisplayName(stage) + " score between 1 and 5 for every target before submitting.";
            }
        }
        return null;
    }

    private List<Target> resolveTargetsForScoreSubmission(Scorecard scorecard) {
        if (scorecard == null || scorecard.getId() <= 0) {
            return new ArrayList<Target>();
        }
        String hierarchyModel = resolveHierarchyModel(scorecard);
        if (isLegacyHierarchyModel(hierarchyModel)) {
            return attachLegacyTargets(scorecard, gearService.listSelectedGears(scorecard));
        }
        List<Target> targetsList = targetService.getAllTargetsByScorecard(scorecard.getId());
        return targetsList == null ? new ArrayList<Target>() : targetsList;
    }

    private boolean hasValidCapturedScore(Score score, ValueBasedCaptureStage stage) {
        if (score == null) {
            return false;
        }
        return isValidValueBasedScore(resolveCapturedScoreValue(score, stage));
    }

    private Double resolveCapturedScoreValue(Score score, ValueBasedCaptureStage stage) {
        if (score == null || stage == null) {
            return null;
        }
        switch (stage) {
            case EMPLOYEE:
                return score.getEmployeeScore();
            case MANAGER:
                return score.getManagerScore();
            case AGREED:
                return score.getAgreedScore();
            case MODERATED:
                return score.getModeratedScore();
            default:
                return null;
        }
    }

    private String scoreStageDisplayName(ValueBasedCaptureStage stage) {
        if (stage == null) {
            return "score";
        }
        switch (stage) {
            case EMPLOYEE:
                return "employee";
            case MANAGER:
                return "manager";
            case AGREED:
                return "agreed";
            case MODERATED:
                return "moderated";
            default:
                return "score";
        }
    }

    private String resolveCaptureSubmitLabel(ValueBasedCaptureStage captureStage, ScorecardWorkflowDefinition workflow) {
        if (captureStage == null) {
            return "Validate & Submit";
        }
        switch (captureStage) {
            case EMPLOYEE:
                return resolveRoleActionLabel(workflow, PMConstants.SCORECARD_STAGE_OWNER_SCORING, "Submit Owner Scores");
            case MANAGER:
                return resolveRoleActionLabel(workflow, PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING, "Submit Supervisor Scores");
            case AGREED:
                return resolveRoleActionLabel(workflow, PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING, "Submit Agreed Scores");
            case MODERATED:
                return resolveRoleActionLabel(workflow, PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING, "Submit Moderated Scores");
            default:
                return "Validate & Submit";
        }
    }

    private String resolveRoleActionLabel(ScorecardWorkflowDefinition workflow, String roleKey, String fallback) {
        if (workflow == null || workflow.getRoleActionButtonLabels() == null || roleKey == null) {
            return fallback;
        }
        String configured = workflow.getRoleActionButtonLabels().get(roleKey.toUpperCase(Locale.ENGLISH));
        if (configured == null || configured.trim().isEmpty()) {
            return fallback;
        }
        return configured.trim();
    }

    private double reportingDateWeightedTotal(Map<Long, Double> totalsByReportingDate, ReportingDate reportingDate) {
        if (totalsByReportingDate == null || reportingDate == null) {
            return 0.0;
        }
        Double total = totalsByReportingDate.get(reportingDate.getId());
        return total == null ? 0.0 : total;
    }

    private int resolveValueBasedViewTableColumnCount(int hierarchyColumnCount, ValueBasedCaptureStage captureStage) {
        int columns = hierarchyColumnCount + 9; // measure, unit, base, stretch, weight, employee score, evidence, justification, action
        if (captureStage != ValueBasedCaptureStage.EMPLOYEE) {
            columns += 1; // manager score
        }
        if (captureStage == ValueBasedCaptureStage.AGREED || captureStage == ValueBasedCaptureStage.MODERATED) {
            columns += 1; // agreed score
        }
        return columns;
    }

    private int resolveValueBasedCaptureTableColumnCount(int hierarchyColumnCount, ValueBasedCaptureStage captureStage) {
        int columns = hierarchyColumnCount + 9; // measure, unit, base, stretch, weight, employee, evidence, justification, action
        if (captureStage != ValueBasedCaptureStage.EMPLOYEE) {
            columns += 1; // manager
            if (captureStage != ValueBasedCaptureStage.MODERATED) {
                columns += 1; // weighted
            }
        } else {
            columns += 1; // attachment
        }
        if (captureStage == ValueBasedCaptureStage.AGREED || captureStage == ValueBasedCaptureStage.MODERATED) {
            columns += 1; // agreed
        }
        return columns;
    }

    private boolean equalsStatus(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private boolean canViewScorecardReport(Scorecard scorecard) {
        if (scorecard == null) {
            return false;
        }
        if (scorecard.getApprovalStage() != null
                && scorecard.getApprovalStage().getRoleKey() != null
                && PMConstants.SCORECARD_STAGE_CLOSED.equalsIgnoreCase(scorecard.getApprovalStage().getRoleKey().trim())) {
            return true;
        }
        return equalsStatus(scorecard.getApprovalStatus(), PMConstants.APPROVAL_STATUS_MODERATED_BY_HR)
                || equalsStatus(scorecard.getApprovalStatus(), PMConstants.APPROVAL_STATUS_CLOSED);
    }

    private boolean canViewScorecard(Scorecard scorecard) {
        if (scorecard == null) {
            return false;
        }
        Account loggedUser = commonService.getLoggedUser();
        if (loggedUser == null || !isScorecardInConfiguredClient(scorecard)) {
            return false;
        }
        if (commonService.isAdmin() || commonService.hasSpecialRights()) {
            return true;
        }
        if (accessControlService.hasPermission(loggedUser, AccessPermissions.SCORECARD_APPROVE_TARGETS_HR, scorecard.getOwner())
                || accessControlService.hasPermission(loggedUser, AccessPermissions.SCORECARD_APPROVE_AGREED_SCORES_HR, scorecard.getOwner())
                || accessControlService.hasPermission(loggedUser, AccessPermissions.SCORECARD_MODERATE, scorecard.getOwner())
                || accessControlService.hasPermission(loggedUser, AccessPermissions.SCORECARD_CLOSE, scorecard.getOwner())) {
            return true;
        }

        String accountType = loggedUser.getAccountType();
        if ("EMPLOYEE".equalsIgnoreCase(accountType)) {
            return isScorecardOwner(scorecard, loggedUser);
        }
        if ("SUPERVISOR".equalsIgnoreCase(accountType)) {
            return isScorecardOwner(scorecard, loggedUser) || isScorecardSupervisor(scorecard, loggedUser);
        }
        if ("DEPARTMENT_MANAGER".equalsIgnoreCase(accountType) || "DIVISIONAL_DIRECTOR".equalsIgnoreCase(accountType)) {
            return isSameDepartment(scorecard.getOwner(), loggedUser);
        }
        if ("ACTING_CEO".equalsIgnoreCase(accountType) || "CEO".equalsIgnoreCase(accountType)) {
            return true;
        }
        return false;
    }

    private boolean isScorecardInConfiguredClient(Scorecard scorecard) {
        if (scorecard == null) {
            return false;
        }
        long configuredClientId = commonService.getConfiguredClientId();
        if (configuredClientId <= 0) {
            return true;
        }
        ReportingPeriod reportingPeriod = scorecard.getReportingPeriod();
        if (reportingPeriod != null
                && (reportingPeriod.getClientId() == configuredClientId || reportingPeriod.getClientId() <= 0)) {
            return true;
        }
        long scorecardClientId = scorecard.getClientId();
        return scorecardClientId <= 0 || configuredClientId <= 0 || scorecardClientId == configuredClientId;
    }

    private boolean isScorecardOwner(Scorecard scorecard, Account loggedUser) {
        return scorecard != null
                && scorecard.getOwner() != null
                && loggedUser != null
                && scorecard.getOwner().getId() == loggedUser.getId();
    }

    private boolean isScorecardSupervisor(Scorecard scorecard, Account loggedUser) {
        return scorecard != null
                && scorecard.getOwner() != null
                && scorecard.getOwner().getSupervisor() != null
                && loggedUser != null
                && scorecard.getOwner().getSupervisor().getId() == loggedUser.getId();
    }

    private boolean isSameDepartment(Account owner, Account loggedUser) {
        return owner != null
                && owner.getDepartment() != null
                && loggedUser != null
                && loggedUser.getDepartment() != null
                && owner.getDepartment().getId() == loggedUser.getDepartment().getId();
    }

    private boolean canCaptureTargets(Scorecard scorecard) {
        return canCaptureTargets(scorecard, isTargetCaptureStage(resolveContractStageRole(scorecard)));
    }

    private boolean canCaptureTargets(Scorecard scorecard, boolean targetCaptureStage) {
        return scorecard != null
                && targetCaptureStage
                && PMConstants.STATUS_ACTIVE.equalsIgnoreCase(scorecard.getStatus())
                && isScorecardInActiveReportingPeriod(scorecard)
                && commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_TARGETS, scorecard);
    }

    private ScorecardActionState resolveScorecardActionState(Scorecard scorecard,
                                                             String contractRole,
                                                             String reportingDateRole,
                                                             ScorecardWorkflowDefinition workflow) {
        ScorecardActionState state = new ScorecardActionState();
        Account loggedUser = commonService.getLoggedUser();
        if (scorecard == null || loggedUser == null) {
            return state;
        }

        if (isTargetCaptureStage(contractRole)) {
            if (canCurrentUserCaptureTargetsIgnoringActivity(scorecard, loggedUser)) {
                state.expectedActivity = PMConstants.ACTIVITY_CAPTURE_TARGETS;
                state.canCaptureTargets = canCaptureTargets(scorecard, true);
            }
            return state;
        }

        if (isSupervisorTargetApprovalStage(contractRole)) {
            if (isScorecardSupervisor(scorecard, loggedUser)) {
                state.expectedActivity = PMConstants.ACTIVITY_APPROVE_SCORECARD;
                state.canSupervisorApproveTargets = commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_SCORECARD, scorecard);
            }
            return state;
        }

        if (isHrTargetApprovalStage(contractRole)) {
            if (canCurrentUserApproveTargetsAsHr(scorecard, loggedUser)) {
                state.expectedActivity = PMConstants.ACTIVITY_APPROVE_SCORECARD;
                state.canHrApproveTargets = commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_SCORECARD, scorecard);
            }
            return state;
        }

        if (!isContractReadyForScoring(scorecard, workflow) || !StringUtils.hasText(reportingDateRole)) {
            return state;
        }

        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_OWNER_SCORING)) {
            if (canCurrentUserCaptureEmployeeScoresIgnoringActivity(scorecard, loggedUser)) {
                state.expectedActivity = PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES;
                state.canCaptureEmployeeScore = commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES, scorecard);
            }
            return state;
        }

        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL)) {
            if (isScorecardSupervisor(scorecard, loggedUser)) {
                state.expectedActivity = PMConstants.ACTIVITY_APPROVE_OWNER_SCORES;
                state.canApproveOwnerScores = commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_OWNER_SCORES, scorecard);
            }
            return state;
        }

        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING)) {
            if (isScorecardSupervisor(scorecard, loggedUser)) {
                state.expectedActivity = PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES;
                state.canCaptureManagerScore = commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES, scorecard);
            }
            return state;
        }

        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING)) {
            if (isScorecardSupervisor(scorecard, loggedUser)) {
                state.expectedActivity = PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES;
                state.canCaptureAgreedScore = commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES, scorecard);
            }
            return state;
        }

        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL)) {
            if (canCurrentUserApproveAgreedScoresIgnoringActivity(scorecard, loggedUser)) {
                state.expectedActivity = PMConstants.ACTIVITY_APPROVE_AGREED_SCORES;
                state.canApproveAgreedScores = commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_AGREED_SCORES, scorecard);
            }
            return state;
        }

        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING)) {
            if (canCurrentUserModerateIgnoringActivity(scorecard, loggedUser)) {
                state.expectedActivity = PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES;
                state.canModerate = commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES, scorecard);
            }
            return state;
        }

        if (equalsStatus(scorecard.getApprovalStatus(), workflow.getModeratedByHrStatus())
                && canCurrentUserCloseScorecardIgnoringActivity(scorecard, loggedUser)) {
            state.expectedActivity = PMConstants.ACTIVITY_CLOSE_SCORECARD;
        }
        return state;
    }

    private boolean canCurrentUserCaptureEmployeeScoresIgnoringActivity(Scorecard scorecard, Account loggedUser) {
        return isScorecardOwner(scorecard, loggedUser)
                || (loggedUser != null && PMConstants.HAS_SPECIAL_RIGHTS.equalsIgnoreCase(loggedUser.getSpecial()));
    }

    private boolean canCurrentUserApproveTargetsAsHr(Scorecard scorecard, Account loggedUser) {
        return scorecard != null
                && scorecard.getOwner() != null
                && accessControlService.hasPermission(loggedUser, AccessPermissions.SCORECARD_APPROVE_TARGETS_HR, scorecard.getOwner());
    }

    private boolean canCurrentUserApproveAgreedScoresIgnoringActivity(Scorecard scorecard, Account loggedUser) {
        return scorecard != null
                && scorecard.getOwner() != null
                && accessControlService.hasPermission(loggedUser, AccessPermissions.SCORECARD_APPROVE_AGREED_SCORES_HR, scorecard.getOwner());
    }

    private boolean canCurrentUserModerateIgnoringActivity(Scorecard scorecard, Account loggedUser) {
        return scorecard != null
                && scorecard.getOwner() != null
                && accessControlService.hasPermission(loggedUser, AccessPermissions.SCORECARD_MODERATE, scorecard.getOwner());
    }

    private boolean canCurrentUserCloseScorecardIgnoringActivity(Scorecard scorecard, Account loggedUser) {
        return scorecard != null
                && scorecard.getOwner() != null
                && accessControlService.hasPermission(loggedUser, AccessPermissions.SCORECARD_CLOSE, scorecard.getOwner());
    }

    private String resolveScorecardActivityRestrictionNotice(Scorecard scorecard,
                                                             ReportingDate reportingDate,
                                                             boolean hasAllowedAction,
                                                             String expectedActivity) {
        if (hasAllowedAction) {
            return "";
        }
        if (scorecard == null
                || reportingDate == null
                || !reportingDateService.isReportingDateOpen(reportingDate)
                || !isReportingDateForScorecard(scorecard, reportingDate)
                || !reportingDateActivityPeriodService.hasConfiguredActivityPeriods(reportingDate)) {
            return "";
        }

        if (!StringUtils.hasText(expectedActivity)
                || reportingDateActivityPeriodService.isActivityAllowed(reportingDate, expectedActivity)) {
            return "";
        }

        String activitySummary = reportingDateActivityPeriodService.getCurrentActivitySummary(reportingDate);
        if (StringUtils.hasText(activitySummary)) {
            return "Current reporting-date activity: " + activitySummary;
        }
        return "The active reporting date has activity periods configured, but none is active today.";
    }

    private boolean canCurrentUserCaptureTargetsIgnoringActivity(Scorecard scorecard, Account loggedUser) {
        return scorecard != null
                && PMConstants.STATUS_ACTIVE.equalsIgnoreCase(scorecard.getStatus())
                && isScorecardInActiveReportingPeriod(scorecard)
                && PMConstants.LOCK_STATUS_OPEN.equalsIgnoreCase(scorecard.getLockStatus())
                && (isScorecardOwner(scorecard, loggedUser)
                || PMConstants.HAS_SPECIAL_RIGHTS.equalsIgnoreCase(loggedUser.getSpecial())
                || PMConstants.IS_ADMIN.equalsIgnoreCase(loggedUser.getAdmin()));
    }

    private boolean isTargetCaptureStage(String contractRole) {
        return matchesRole(contractRole, PMConstants.SCORECARD_STAGE_NEW)
                || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_CAPTURE_TARGETS);
    }

    private boolean isSupervisorTargetApprovalStage(String contractRole) {
        return matchesRole(contractRole, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR);
    }

    private boolean isHrTargetApprovalStage(String contractRole) {
        return matchesRole(contractRole, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR);
    }

    private boolean isContractReadyForScoring(Scorecard scorecard, ScorecardWorkflowDefinition workflow) {
        if (scorecard == null || workflow == null) {
            return false;
        }
        String contractRole = resolveContractStageRole(scorecard);
        if (matchesRole(contractRole, PMConstants.SCORECARD_STAGE_OWNER_SCORING)
                || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL)
                || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING)
                || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING)
                || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL)
                || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING)
                || matchesRole(contractRole, PMConstants.SCORECARD_STAGE_CLOSED)) {
            return true;
        }
        return equalsStatus(scorecard.getApprovalStatus(), workflow.getApprovedByHrStatus())
                || equalsStatus(scorecard.getApprovalStatus(), workflow.getScoredByEmployeeStatus())
                || equalsStatus(scorecard.getApprovalStatus(), workflow.getApprovedOwnerScoresStatus())
                || equalsStatus(scorecard.getApprovalStatus(), workflow.getScoredBySupervisorStatus())
                || equalsStatus(scorecard.getApprovalStatus(), workflow.getAgreedByTwoStatus())
                || equalsStatus(scorecard.getApprovalStatus(), workflow.getApprovedAgreedScoresStatus())
                || equalsStatus(scorecard.getApprovalStatus(), workflow.getModeratedByHrStatus())
                || equalsStatus(scorecard.getApprovalStatus(), workflow.getClosedStatus());
    }

    private String resolveContractStageRole(Scorecard scorecard) {
        if (scorecard == null) {
            return null;
        }
        if (scorecard.getApprovalStage() != null && StringUtils.hasText(scorecard.getApprovalStage().getRoleKey())) {
            return scorecard.getApprovalStage().getRoleKey().trim().toUpperCase(Locale.ENGLISH);
        }
        return mapContractStatusToRole(scorecard.getApprovalStatus());
    }

    private String mapContractStatusToRole(String approvalStatus) {
        if (!StringUtils.hasText(approvalStatus)) {
            return PMConstants.SCORECARD_STAGE_NEW;
        }
        switch (approvalStatus.trim().toUpperCase(Locale.ENGLISH)) {
            case PMConstants.APPROVAL_STATUS_NEW:
                return PMConstants.SCORECARD_STAGE_NEW;
            case PMConstants.APPROVAL_STATUS_PENDING_APPROVAL:
            case PMConstants.APPROVAL_STATUS_REJECTED_BY_HR:
                return PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR;
            case PMConstants.APPROVAL_STATUS_APPROVED_BY_SUPERVISOR:
                return PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR;
            case PMConstants.APPROVAL_STATUS_REJECTED_BY_SUPERVISOR:
                return PMConstants.SCORECARD_STAGE_CAPTURE_TARGETS;
            case PMConstants.APPROVAL_STATUS_APPROVED_BY_HR:
                return PMConstants.SCORECARD_STAGE_OWNER_SCORING;
            default:
                return null;
        }
    }

    private boolean matchesRole(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    @RequestMapping(value = "/save-target", method = RequestMethod.POST)
    public String saveTarget(@Valid GoalWrapper goalWrapper, BindingResult bindingResult, HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            log.warn("Target validation failed: {}", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/scorecards/view-scorecards";
        }

        long scorecardId = goalWrapper.getScorecardId();
        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        if (!canCaptureTargets(scorecard)) {
            PortletUtils.addErrorMsg("Targets can only be captured for an active reporting period.", request);
            return scorecard == null ? "redirect:/scorecards" : "redirect:/scorecards/view-scorecard/" + scorecardId;
        }
        Goal goal;
        Target target;

        if(goalWrapper.getGoalId() < 1){
            goal = new Goal();
        }else {
            goal = goalService.getGoalById(goalWrapper.getGoalId());
        }

        goal.setScorecardId(goalWrapper.getScorecardId());
        goal.setPerspective(goalWrapper.getPerspective());
        goal.setStrategicObjective(goalWrapper.getStrategicObjective());
        goal.setName(goalWrapper.getGoalName());
        Goal savedGoal = goalService.saveGoal(goal);

        if(goalWrapper.getTargetId() < 1){
            target = new Target();
        }else{
            target = targetService.getTargetById(goalWrapper.getTargetId());
        }

        target.setGoal(savedGoal);
        target.setPerspective(savedGoal.getPerspective());
        target.setStrategicObjective(savedGoal.getStrategicObjective());
        target.setMeasure(goalWrapper.getMeasure());
        target.setUnit(goalWrapper.getUnit());
        target.setAllocatedWeight(goalWrapper.getAllocatedWeight());
        target.setNormalTarget(goalWrapper.getNormalTarget());
        target.setBaseTarget(goalWrapper.getBaseTarget());
        target.setStretchTarget(goalWrapper.getStretchTarget());
        targetService.saveTarget(target);

        return "redirect:/scorecards/capture-targets/"+ scorecardId;
    }

    @RequestMapping(value = "/save-target-to-existing-goal", method = RequestMethod.POST)
    public String saveTargetToExistingGoal(@Valid Target target, BindingResult bindingResult, HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            log.warn("Target validation failed: {}", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/scorecards/view-scorecards";
        }

        if (target == null || target.getGoal() == null || target.getGoal().getScorecardId() <= 0) {
            PortletUtils.addErrorMsg("Scorecard reference is required.", request);
            return "redirect:/scorecards";
        }
        long scorecardId = target.getGoal().getScorecardId();
        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        if (!canCaptureTargets(scorecard)) {
            PortletUtils.addErrorMsg("Targets can only be captured for an active reporting period.", request);
            return scorecard == null ? "redirect:/scorecards" : "redirect:/scorecards/view-scorecard/" + scorecardId;
        }
        targetService.saveTarget(target);

        return "redirect:/scorecards/capture-targets/"+ scorecardId;
    }

    @RequestMapping(value = "/save-output-target", method = RequestMethod.POST)
    public String saveOutputTarget(OutputWrapper wrapper, HttpServletRequest request) {
        long scorecardId = wrapper.getScorecardId();
        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        if (scorecard == null) {
            PortletUtils.addErrorMsg("Scorecard could not be resolved.", request);
            return "redirect:/scorecards";
        }
        if (!canCaptureTargets(scorecard)) {
            PortletUtils.addErrorMsg("Targets can only be captured for an active reporting period.", request);
            return "redirect:/scorecards/view-scorecard/" + scorecardId;
        }

        Outcome outcome = outcomeService.getOutcomeById(wrapper.getOutcomeId());
        if (outcome == null) {
            PortletUtils.addErrorMsg("Outcome could not be resolved.", request);
            return "redirect:/scorecards/capture-targets/" + scorecardId;
        }

        Goal goal = resolveGoalFromOutcome(outcome);
        if (goal == null) {
            PortletUtils.addErrorMsg("Strategic goal could not be resolved for the selected outcome.", request);
            return "redirect:/scorecards/capture-targets/" + scorecardId;
        }

        Output output;
        if (wrapper.getOutputId() > 0) {
            output = outputService.getOutputById(wrapper.getOutputId());
            if (output == null) {
                PortletUtils.addErrorMsg("Output could not be resolved.", request);
                return "redirect:/scorecards/capture-targets/" + scorecardId;
            }
        } else {
            output = new Output();
            output.setScorecard(scorecard);
        }

        output.setOutcome(outcome);
        output.setName(wrapper.getName());
        output.setAllocatedWeight(wrapper.getAllocatedWeight());
        Output savedOutput = outputService.saveOutput(output);

        Target target;
        if (wrapper.getTargetId() > 0) {
            target = targetService.getTargetById(wrapper.getTargetId());
            if (target == null) {
                PortletUtils.addErrorMsg("Target could not be resolved.", request);
                return "redirect:/scorecards/capture-targets/" + scorecardId;
            }
        } else {
            target = new Target();
        }

        target.setOutput(savedOutput);
        target.setGoal(goal);
        target.setPerspective(goal.getPerspective());
        target.setStrategicObjective(goal.getStrategicObjective());
        target.setMeasure(wrapper.getMeasure());
        target.setUnit(wrapper.getUnit());
        target.setAllocatedWeight(wrapper.getAllocatedWeight());
        target.setNormalTarget(wrapper.getNormalTarget());
        target.setBaseTarget(wrapper.getBaseTarget());
        target.setStretchTarget(wrapper.getStretchTarget());
        targetService.saveTarget(target);

        return "redirect:/scorecards/capture-targets/" + scorecardId;
    }

    @RequestMapping(value = "/save-target-to-existing-output", method = RequestMethod.POST)
    public String saveTargetToExistingOutput(Target target, HttpServletRequest request) {
        if (target == null || target.getOutput() == null || target.getOutput().getId() <= 0) {
            PortletUtils.addErrorMsg("Output reference is required.", request);
            return "redirect:/scorecards";
        }

        Output output = outputService.getOutputById(target.getOutput().getId());
        if (output == null || output.getScorecard() == null) {
            PortletUtils.addErrorMsg("Output could not be resolved.", request);
            return "redirect:/scorecards";
        }
        Scorecard scorecard = output.getScorecard();
        if (!canCaptureTargets(scorecard)) {
            PortletUtils.addErrorMsg("Targets can only be captured for an active reporting period.", request);
            return "redirect:/scorecards/view-scorecard/" + scorecard.getId();
        }

        Goal goal = resolveGoalFromOutcome(output.getOutcome());
        if (goal == null) {
            PortletUtils.addErrorMsg("Strategic goal could not be resolved for this output.", request);
            return "redirect:/scorecards/capture-targets/" + scorecard.getId();
        }

        target.setOutput(output);
        target.setGoal(goal);
        target.setPerspective(goal.getPerspective());
        target.setStrategicObjective(goal.getStrategicObjective());
        if (target.getAllocatedWeight() == null) {
            target.setAllocatedWeight(output.getAllocatedWeight());
        } else {
            output.setAllocatedWeight(target.getAllocatedWeight());
            outputService.saveOutput(output);
        }
        targetService.saveTarget(target);

        return "redirect:/scorecards/capture-targets/" + scorecard.getId();
    }

    @RequestMapping(value = "/select-gear", method = RequestMethod.POST)
    public String selectGear(HttpServletRequest request, long gearId, long scorecardId) {
        Gear gear = gearService.getGearById(gearId);
        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        if (gear == null || scorecard == null) {
            PortletUtils.addErrorMsg("Scorecard hierarchy selection failed. Invalid input.", request);
            return "redirect:/scorecards/capture-targets/" + scorecardId;
        }
        if (!canCaptureTargets(scorecard)) {
            PortletUtils.addErrorMsg("Targets can only be captured for an active reporting period.", request);
            return "redirect:/scorecards/view-scorecard/" + scorecardId;
        }

        if (scorecard.getReportingPeriod() == null || scorecard.getReportingPeriod().getId() <= 0) {
            PortletUtils.addErrorMsg("Scorecard reporting period is missing.", request);
            return "redirect:/scorecards/capture-targets/" + scorecardId;
        }

        if (gear.getReportingPeriod() == null || gear.getReportingPeriod().getId() <= 0) {
            gear.setReportingPeriod(scorecard.getReportingPeriod());
            gearService.addGear(gear);
        } else if (gear.getReportingPeriod().getId() != scorecard.getReportingPeriod().getId()) {
            PortletUtils.addErrorMsg("Selected " + stage1Label(gear) + " belongs to a different reporting period.", request);
            return "redirect:/scorecards/capture-targets/" + scorecardId;
        }

        if (gear.getGoals() == null || gear.getGoals().isEmpty()) {
            PortletUtils.addErrorMsg("Not added. The hierarchy is incomplete for this " + stage1Label(gear) + ".", request);
            return "redirect:/scorecards/capture-targets/" + scorecardId;
        }

        Map<Long, Output> existingOutputsByOutcome = new HashMap<Long, Output>();
        for (Output existingOutput : outputService.listAllOutputs(scorecard)) {
            if (existingOutput != null && existingOutput.getOutcome() != null) {
                existingOutputsByOutcome.put(existingOutput.getOutcome().getId(), existingOutput);
            }
        }

        for (Goal goal : gear.getGoals()) {
            if ("programme".equalsIgnoreCase(gear.getCategory())) {
                if (goal.getPillars() == null || goal.getPillars().isEmpty()) {
                    PortletUtils.addErrorMsg("Not added. The records are not well cascaded. Please add pillars and strategic goals under this programme.", request);
                    continue;
                }
                for (Pillar pillar : goal.getPillars()) {
                    if (pillar == null || pillar.getOutcomes() == null) {
                        continue;
                    }
                    for (Outcome outcome : pillar.getOutcomes()) {
                        ensureLegacyOutput(scorecard, outcome, existingOutputsByOutcome);
                    }
                }
            } else if ("gear".equalsIgnoreCase(gear.getCategory())) {
                if (goal.getOutcomes() == null || goal.getOutcomes().isEmpty()) {
                    PortletUtils.addErrorMsg("Not added. The records are not well cascaded. Please add outcomes to all goals under this gear.", request);
                    continue;
                }
                for (Outcome outcome : goal.getOutcomes()) {
                    ensureLegacyOutput(scorecard, outcome, existingOutputsByOutcome);
                }
            } else {
                PortletUtils.addErrorMsg("Unknown hierarchy category for this strategic item.", request);
            }
        }

        return "redirect:/scorecards/capture-targets/" + scorecardId;
    }

    private void ensureLegacyOutput(Scorecard scorecard, Outcome outcome, Map<Long, Output> existingOutputsByOutcome) {
        if (outcome == null || outcome.getId() <= 0 || existingOutputsByOutcome.containsKey(outcome.getId())) {
            return;
        }
        Output output = new Output();
        output.setOutcome(outcome);
        output.setScorecard(scorecard);
        output.setName(outcome.getName());
        output.setAllocatedWeight(0.0);
        Output savedOutput = outputService.saveOutput(output);
        existingOutputsByOutcome.put(outcome.getId(), savedOutput);
    }

    private String stage1Label(Gear gear) {
        if (gear == null || !StringUtils.hasText(gear.getCategory())) {
            return "item";
        }
        if ("programme".equalsIgnoreCase(gear.getCategory())) {
            return "programme";
        }
        if ("gear".equalsIgnoreCase(gear.getCategory())) {
            return "gear";
        }
        return "item";
    }

    @RequestMapping(value = "/save-overall-comment", method = RequestMethod.POST)
    public void saveComment(HttpServletRequest request, HttpServletResponse response, Long scorecardId, String userType, String comment) throws MalformedURLException {

        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        if (scorecard == null) {
            writeScoreSaveResponse(response, true, "Scorecard could not be resolved");
            return;
        }

        ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
        String captureBlockedMessage = resolveScoreCaptureBlockedMessage(scorecard, reportingDate);
        if (captureBlockedMessage != null) {
            writeScoreSaveResponse(response, true, captureBlockedMessage);
            return;
        }
        if (!isOverallCommentAllowed(scorecard, userType)) {
            writeScoreSaveResponse(response, true, "You are not allowed to save this overall comment at the current stage");
            return;
        }
        if (comment != null && comment.length() > 1000) {
            writeScoreSaveResponse(response, true, "Overall comment cannot exceed 1000 characters");
            return;
        }

        saveReportingDateOverallComment(scorecard, reportingDate, userType, comment);
        if(!PMConstants.USER_TYPE_OWNER.equalsIgnoreCase(userType)){
            URL link = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecardId));

            String recipient = scorecard.getOwner().getEmail();
            String subject = "Scorecard Comment,";
            String template = "Good day, \n\n"
                                + "Please note that"+ commonService.getLoggedUser().getFullName() +" added an overall comment on your scorecard. "
                                + "Message: "+ comment + "\n"
                                + "You can now login and response or action\n"
                                + "Link: "+ link + "\n\n";
            try {
                sendScorecardEmail(request, recipient, subject, template);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
        }

        writeScoreSaveResponse(response);
    }

    private boolean isOverallCommentAllowed(Scorecard scorecard, String userType) {
        if (scorecard == null || !StringUtils.hasText(userType)) {
            return false;
        }
        String reportingDateRole = resolveActiveReportingDateRole(scorecard);
        if (PMConstants.USER_TYPE_OWNER.equalsIgnoreCase(userType)) {
            return matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_OWNER_SCORING)
                    && commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES, scorecard);
        }
        if (PMConstants.USER_TYPE_SUPERVISOR.equalsIgnoreCase(userType)) {
            if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING)) {
                return commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES, scorecard);
            }
            return matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING)
                    && commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES, scorecard);
        }
        if (PMConstants.USER_TYPE_MODERATOR.equalsIgnoreCase(userType)) {
            return matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING)
                    && commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES, scorecard);
        }
        return false;
    }

    private void saveReportingDateOverallComment(Scorecard scorecard,
                                                 ReportingDate reportingDate,
                                                 String userType,
                                                 String comment) {
        OverallComment overallComment = new OverallComment();
        overallComment.setScorecard(scorecard);
        overallComment.setReportingDate(reportingDate);
        if(PMConstants.USER_TYPE_OWNER.equalsIgnoreCase(userType)){
            overallComment.setOwnerComment(comment);
        } else if (PMConstants.USER_TYPE_SUPERVISOR.equalsIgnoreCase(userType)) {
            overallComment.setSupervisorComment(comment);
        } else if (PMConstants.USER_TYPE_MODERATOR.equalsIgnoreCase(userType)) {
            overallComment.setModeratorComment(comment);
        }
        overallCommentService.saveOverallComment(overallComment);
    }

    @RequestMapping(value = "/delete-target", method = RequestMethod.POST)
    public String deleteTarget(HttpServletRequest request, Target targ) {

        Target target = targetService.getTargetById(targ.getId());
        if (target == null) {
            PortletUtils.addErrorMsg("Target wasn't found", request);
            return "redirect:/scorecards";
        }

        long scorecardId = 0;
        if (target.getGoal() != null && target.getGoal().getScorecardId() > 0) {
            scorecardId = target.getGoal().getScorecardId();
        } else if (target.getOutput() != null && target.getOutput().getScorecard() != null) {
            scorecardId = target.getOutput().getScorecard().getId();
        }

        if (scorecardId <= 0) {
            return "redirect:/scorecards";
        }
        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        if (!canCaptureTargets(scorecard)) {
            PortletUtils.addErrorMsg("Targets can only be deleted for an active reporting period.", request);
            return scorecard == null ? "redirect:/scorecards" : "redirect:/scorecards/view-scorecard/" + scorecardId;
        }

        try {
            targetService.deleteTarget(target);
            PortletUtils.addInfoMsg("Target was successfully deleted", request);
            if (target.getOutput() != null) {
                Output output = outputService.getOutputById(target.getOutput().getId());
                if (output != null && !targetService.checkIfOutputHasTargets(output)) {
                    outputService.deleteOutput(output);
                    PortletUtils.addInfoMsg("Output was successfully deleted", request);
                }
            }
        }catch (Exception e){
            PortletUtils.addErrorMsg("Target wasn't deleted", request);
        }

        return "redirect:/scorecards/capture-targets/"+ scorecardId;
    }

    @RequestMapping(value = "/submit-scorecard-for-approval", method = RequestMethod.POST)
    public String submitScorecardForApproval(HttpServletRequest request, Scorecard updatedScorecard) throws UnsupportedEncodingException, MalformedURLException {

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());
        if (scorecard == null) {
            PortletUtils.addErrorMsg("Scorecard not found.", request);
            return "redirect:/scorecards";
        }
        if (!canCaptureTargets(scorecard)) {
            PortletUtils.addErrorMsg("Targets can only be submitted for an active reporting period.", request);
            return "redirect:/scorecards/view-scorecard/" + scorecard.getId();
        }
        Account owner = scorecard.getOwner();
        Account supervisor = owner == null ? null : owner.getSupervisor();
        String supervisorEmail = supervisor == null ? null : supervisor.getEmail();
        if (supervisor == null || !StringUtils.hasText(supervisorEmail)) {
            String ownerName = owner == null ? "this owner" : owner.getFullName();
            PortletUtils.addErrorMsg("Cannot submit scorecard: no supervisor account is configured for " + ownerName + ".", request);
            return "redirect:/scorecards/view-scorecard/" + scorecard.getId();
        }

        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        scorecard.setApprovalStatus(workflow.getPendingApprovalStatus());
        scorecard.setLockStatus(PMConstants.LOCK_STATUS_LOCKED);
        scorecardService.saveScorecard(scorecard);
        URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
        String recipient = supervisorEmail;

        String subject = "Scorecard Approval,";
        String template = "Good day, \n\n"
                + "Please note that "+ scorecard.getOwner().getFullName() +" has submitted his/her scorecard for your approval. "
                + "You can now login and approve\n"
                + "Link: "+ currentURL +"\n\n";

        try {
            sendScorecardEmail(request, recipient, subject, template);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }

        PortletUtils.addInfoMsg("Scorecard successfully submitted for approval. An email notification will be sent to your supervisor.", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping(value = "/submit-employee-scores", method = RequestMethod.POST)
    public String submitEmployeeScore(HttpServletRequest request, Scorecard updatedScorecard) throws MalformedURLException {

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());
        if (scorecard == null) {
            PortletUtils.addErrorMsg("Scorecard not found.", request);
            return "redirect:/scorecards";
        }
        ReportingDate reportingDate = resolveActiveReportingDate();
        if (reportingDate != null
                && isScoreCaptureWindowOpen(scorecard, reportingDate)
                && !performanceImprovementPlanService.hasSelfAssessmentPerformanceImprovementPlan(scorecard, reportingDate)) {
            PortletUtils.addErrorMsg("Add at least one training and development intervention, or save N/A if none is required.", request);
            return "redirect:/scorecards/capture-scores/" + scorecard.getId();
        }
        if (!validateEmployeeScoreEvidenceAndJustificationSubmission(scorecard, reportingDate, request)) {
            return "redirect:/scorecards/capture-scores/" + scorecard.getId();
        }
        if (!moveActiveReportingDateStage(scorecard, PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL, request)) {
            return "redirect:/scorecards/view-scorecard/" + scorecard.getId();
        }
        Account supervisor = scorecard.getOwner().getSupervisor();

        URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
        String recipient = supervisor.getEmail();

        String subject = "Scorecard Scoring,";
        String template = "Good day, \n\n"
                        + "Please note that "+ scorecard.getOwner().getFullName() +" has submitted captured scores for your approval. "
                        + "After approval, you can proceed with your score capture.\n"
                        + "Link: "+ currentURL +"\n\n";
        try {
            sendScorecardEmail(request, recipient, subject, template);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }

        PortletUtils.addInfoMsg("Owner scores submitted successfully for supervisor approval. An email notification will be sent to " + supervisor.getFullName(), request);
        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    private boolean validateEmployeeScoreEvidenceAndJustificationSubmission(Scorecard scorecard,
                                                                            ReportingDate reportingDate,
                                                                            HttpServletRequest request) {
        if (systemSettingService == null || !systemSettingService.isEmployeeScoreEvidenceAndJustificationRequired()) {
            return true;
        }
        String validationMessage = resolveEmployeeScoreEvidenceAndJustificationIssue(scorecard, reportingDate);
        if (validationMessage == null) {
            return true;
        }
        PortletUtils.addErrorMsg(validationMessage, request);
        return false;
    }

    private String resolveEmployeeScoreEvidenceAndJustificationIssue(Scorecard scorecard, ReportingDate reportingDate) {
        if (scorecard == null) {
            return "Scorecard not found.";
        }
        if (reportingDate == null) {
            return "No active reporting date was found.";
        }
        if (!reportingDateService.isReportingDateOpen(reportingDate)) {
            return "Scores can only be submitted for an OPEN reporting date.";
        }
        if (!isReportingDateForScorecard(scorecard, reportingDate)) {
            return "Scores can only be submitted for the OPEN reporting date in this scorecard's reporting period.";
        }

        List<Target> targetsList = resolveTargetsForScoreSubmission(scorecard);
        if (targetsList.isEmpty()) {
            return "No targets are available for score capture.";
        }
        for (Target target : targetsList) {
            Score score = resolveTargetReportingDateScore(target, reportingDate);
            if (!hasEmployeeScoreEvidenceAndJustification(target, score, reportingDate)) {
                return "Capture evidence and justification for " + targetDisplayName(target) + " before submitting employee scores.";
            }
        }
        return null;
    }

    private boolean hasEmployeeScoreEvidenceAndJustification(Target target, Score score, ReportingDate reportingDate) {
        if (target == null || score == null) {
            return false;
        }
        Evidence evidence = resolveLatestEvidence(target, reportingDate);
        String evidenceLink = firstText(evidence == null ? null : evidence.getEvidence(), score.getEvidence());
        String attachmentName = firstText(evidence == null ? null : evidence.getAttachmentName(), score.getAttachmentName());
        String justification = firstText(evidence == null ? null : evidence.getJustification(), score.getJustification());
        return (StringUtils.hasText(evidenceLink) || StringUtils.hasText(attachmentName))
                && StringUtils.hasText(justification);
    }

    private String firstText(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary;
        }
        return fallback;
    }

    private String targetDisplayName(Target target) {
        if (target != null && StringUtils.hasText(target.getMeasure())) {
            return target.getMeasure().trim();
        }
        return "every target";
    }

    @RequestMapping(value = "/save-self-assessment-pip", method = RequestMethod.POST, consumes = {"*/*"})
    public void saveSelfAssessmentPerformanceImprovementPlan(HttpServletResponse response,
                                                             @RequestParam(value = "id", required = false) Long id,
                                                             @RequestParam(value = "scorecardId", required = false) Long scorecardId,
                                                             @RequestParam(value = "targetId", required = false) Long targetId,
                                                             @RequestParam(value = "targetArea", required = false) String targetArea,
                                                             @RequestParam(value = "concern", required = false) String concern,
                                                             @RequestParam(value = "expectedStandard", required = false) String expectedStandard,
                                                             @RequestParam(value = "agreedAction", required = false) String agreedAction,
                                                             @RequestParam(value = "requiredSupport", required = false) String requiredSupport,
                                                             @RequestParam(value = "endDate", required = false) String endDate,
                                                             @RequestParam(value = "notApplicable", required = false) Boolean notApplicable) {
        try {
            Scorecard scorecard = resolveScorecardForSelfAssessmentPip(scorecardId);
            if (scorecard == null) {
                writeSelfAssessmentPipResponse(response, false, "Scorecard could not be resolved", null);
                return;
            }

            ReportingDate reportingDate = resolveActiveReportingDate();
            String captureIssue = resolveSelfAssessmentPipCaptureIssue(scorecard, reportingDate);
            if (captureIssue != null) {
                writeSelfAssessmentPipResponse(response, false, captureIssue, null);
                return;
            }

            PerformanceImprovementPlan plan = resolveEditableSelfAssessmentPip(id, scorecard, reportingDate);
            if (id != null && id > 0 && plan == null) {
                writeSelfAssessmentPipResponse(response, false, "Development intervention could not be resolved", null);
                return;
            }
            if (plan == null) {
                plan = new PerformanceImprovementPlan();
            }

            Target target = resolveOptionalSelfAssessmentPipTarget(targetId, scorecard);
            if (targetId != null && targetId > 0 && target == null) {
                writeSelfAssessmentPipResponse(response, false, "Selected target does not belong to this scorecard", null);
                return;
            }

            boolean isNotApplicable = Boolean.TRUE.equals(notApplicable);
            String conflictMessage = resolveSelfAssessmentPipConflictMessage(scorecard, reportingDate, plan.getId(), isNotApplicable);
            if (conflictMessage != null) {
                writeSelfAssessmentPipResponse(response, false, conflictMessage, null);
                return;
            }

            String validationMessage = validateSelfAssessmentPipFields(
                    target,
                    targetArea,
                    concern,
                    expectedStandard,
                    agreedAction,
                    requiredSupport,
                    endDate,
                    isNotApplicable
            );
            if (validationMessage != null) {
                writeSelfAssessmentPipResponse(response, false, validationMessage, null);
                return;
            }

            applySelfAssessmentPipValues(
                    plan,
                    scorecard,
                    reportingDate,
                    target,
                    targetArea,
                    concern,
                    expectedStandard,
                    agreedAction,
                    requiredSupport,
                    endDate,
                    isNotApplicable
            );

            PerformanceImprovementPlan savedPlan = performanceImprovementPlanService.savePerformanceImprovementPlan(plan);
            writeSelfAssessmentPipResponse(response, true, "Development intervention saved.", savedPlan);
        } catch (Exception exception) {
            log.error("Failed to save self-assessment PIP for scorecardId={}", scorecardId, exception);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeSelfAssessmentPipResponse(response, false, "Development intervention could not be saved. Please retry.", null);
        }
    }

    @RequestMapping(value = "/delete-self-assessment-pip", method = RequestMethod.POST, consumes = {"*/*"})
    public void deleteSelfAssessmentPerformanceImprovementPlan(HttpServletResponse response,
                                                               @RequestParam(value = "id", required = false) Long id,
                                                               @RequestParam(value = "scorecardId", required = false) Long scorecardId) {
        try {
            Scorecard scorecard = resolveScorecardForSelfAssessmentPip(scorecardId);
            if (scorecard == null) {
                writeSelfAssessmentPipResponse(response, false, "Scorecard could not be resolved", null);
                return;
            }

            ReportingDate reportingDate = resolveActiveReportingDate();
            String captureIssue = resolveSelfAssessmentPipCaptureIssue(scorecard, reportingDate);
            if (captureIssue != null) {
                writeSelfAssessmentPipResponse(response, false, captureIssue, null);
                return;
            }

            PerformanceImprovementPlan plan = resolveEditableSelfAssessmentPip(id, scorecard, reportingDate);
            if (plan == null) {
                writeSelfAssessmentPipResponse(response, false, "Development intervention could not be resolved", null);
                return;
            }

            performanceImprovementPlanService.deletePerformanceImprovementPlan(plan);
            writeSelfAssessmentPipResponse(response, true, "Development intervention deleted.", null);
        } catch (Exception exception) {
            log.error("Failed to delete self-assessment PIP id={}", id, exception);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeSelfAssessmentPipResponse(response, false, "Development intervention could not be deleted. Please retry.", null);
        }
    }

    @RequestMapping(value = "/supervisor-approve-owner-scores", method = RequestMethod.POST)
    public String supervisorApproveOwnerScores(HttpServletRequest request, Long id) throws MalformedURLException {
        Scorecard scorecard = scorecardService.getScorecardById(id);
        if (scorecard == null) {
            PortletUtils.addErrorMsg("Scorecard not found.", request);
            return "redirect:/scorecards";
        }
        if (!commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_OWNER_SCORES, scorecard)) {
            PortletUtils.addErrorMsg("You are not allowed to approve owner scores on this scorecard.", request);
            return "redirect:/scorecards/view-scorecard/" + id;
        }

        if (!moveActiveReportingDateStage(scorecard, PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING, request)) {
            return "redirect:/scorecards/view-scorecard/" + scorecard.getId();
        }

        Account owner = scorecard.getOwner();
        Account supervisor = owner == null ? null : owner.getSupervisor();
        URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/" + scorecard.getId()));
        String recipient = owner != null ? owner.getEmail() : null;
        String subject = "Scorecard Scoring,";
        String template = "Good day, \n\n"
                + "Please note that " + (supervisor != null ? supervisor.getFullName() : "your supervisor") + " approved your captured scores. "
                + "Supervisor scoring can now proceed.\n"
                + "Link: " + currentURL + "\n\n";
        try {
            sendScorecardEmail(request, recipient, subject, template);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Email to " + recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }

        PortletUtils.addInfoMsg("Owner scores approved successfully. Supervisor score capture can now proceed.", request);
        return "redirect:/scorecards/view-scorecard/" + scorecard.getId();
    }

    @RequestMapping(value = "/supervisor-reject-owner-scores", method = RequestMethod.POST)
    public String supervisorRejectOwnerScores(HttpServletRequest request, Long id, String message) throws MalformedURLException {
        Scorecard scorecard = scorecardService.getScorecardById(id);
        if (scorecard == null) {
            PortletUtils.addErrorMsg("Scorecard not found.", request);
            return "redirect:/scorecards";
        }
        if (!commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_OWNER_SCORES, scorecard)) {
            PortletUtils.addErrorMsg("You are not allowed to reject owner scores on this scorecard.", request);
            return "redirect:/scorecards/view-scorecard/" + id;
        }

        String rejectionReason = cleanText(message);
        if (!StringUtils.hasText(rejectionReason)) {
            PortletUtils.addErrorMsg("A rejection reason is required.", request);
            return "redirect:/scorecards/view-scorecard/" + scorecard.getId();
        }

        if (!moveActiveReportingDateStage(scorecard, PMConstants.SCORECARD_STAGE_OWNER_SCORING, request)) {
            return "redirect:/scorecards/view-scorecard/" + scorecard.getId();
        }

        Account loggedUser = commonService.getLoggedUser();
        Account owner = scorecard.getOwner();
        String supervisorName = loggedUser != null && StringUtils.hasText(loggedUser.getFullName())
                ? loggedUser.getFullName()
                : "your supervisor";
        String ownerName = owner != null && StringUtils.hasText(owner.getFullName())
                ? owner.getFullName()
                : "there";
        String recipient = owner == null ? null : owner.getEmail();

        try {
            Approval approval = new Approval();
            approval.setScorecard(scorecard);
            approval.setAccount(loggedUser);
            approval.setMessage(rejectionReason);
            approval.setStatus(PMConstants.APPROVAL_STATUS_REJECTED_OWNER_SCORES);
            approvalService.addApproval(approval);
        } catch (Exception exception) {
            log.warn("Failed to persist owner score rejection audit for scorecardId={}", scorecard.getId(), exception);
        }

        URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/" + scorecard.getId()));
        String subject = "Scorecard Scoring,";
        String template = "Good day " + ownerName + ", \n\n"
                + "Please note that " + supervisorName + " rejected your captured scores with the following reason:\n\n"
                + ".........................................................................................\n"
                + rejectionReason + "\n"
                + ".........................................................................................\n\n"
                + "Please log in, review the feedback, and resubmit your scores.\n"
                + "Link: " + currentURL + "\n\n";
        try {
            sendScorecardEmail(request, recipient, subject, template);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Email to " + recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }

        PortletUtils.addInfoMsg("Owner scores rejected successfully. An email notification will be sent to " + ownerName + ".", request);
        return "redirect:/scorecards/view-scorecard/" + scorecard.getId();
    }

    @RequestMapping(value = "/submit-manager-scores", method = RequestMethod.POST)
    public String submitManagerScore(HttpServletRequest request, Scorecard updatedScorecard) throws MalformedURLException {

        long scorecardId = updatedScorecard == null ? 0 : updatedScorecard.getId();
        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        if (scorecard == null) {
            PortletUtils.addErrorMsg("Scorecard not found.", request);
            return "redirect:/scorecards";
        }

        ReportingDate reportingDate = resolveActiveReportingDate();
        if (!validateValueBasedScoreSubmission(scorecard, reportingDate, ValueBasedCaptureStage.MANAGER, request)) {
            return "redirect:/scorecards/capture-scores/" + scorecard.getId();
        }

        if (!moveActiveReportingDateStage(scorecard, PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING, request)) {
            return "redirect:/scorecards/view-scorecard/" + scorecard.getId();
        }
        Account supervisor = scorecard.getOwner().getSupervisor();
        Account owner = scorecard.getOwner();
        URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));

        String recipient = owner.getEmail();
        String subject = "Scorecard Scoring,";
        String template = "Good day, \n\n"
                + "Please note that "+ supervisor.getFullName() +" has captured scores on your scorecard. Be prepared for the session to capture agreed scores. "
                + "You can now login and add your scores\n"
                + "Link: "+ currentURL + "\n\n";
        try {
            sendScorecardEmail(request, recipient, subject, template);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }

        PortletUtils.addInfoMsg("Scorecard scores successfully captured. You can now start to capture the agreed scores", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping(value = "/submit-agreed-scores", method = RequestMethod.POST)
    public String submitAgreedScore(HttpServletRequest request, Scorecard updatedScorecard) throws MalformedURLException {

        long scorecardId = updatedScorecard == null ? 0 : updatedScorecard.getId();
        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        if (scorecard == null) {
            PortletUtils.addErrorMsg("Scorecard not found.", request);
            return "redirect:/scorecards";
        }
        ReportingDate reportingDate = resolveActiveReportingDate();
        if (!validateValueBasedScoreSubmission(scorecard, reportingDate, ValueBasedCaptureStage.AGREED, request)) {
            return "redirect:/scorecards/capture-scores/" + scorecard.getId();
        }
        Account loggedUser = commonService.getLoggedUser();
        Account supervisor = scorecard.getOwner().getSupervisor();
        Account owner = scorecard.getOwner();

        try {
            if (!moveActiveReportingDateStage(scorecard, PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL, request)) {
                return "redirect:/scorecards/view-scorecard/" + scorecard.getId();
            }
            URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
            String recipient = owner.getEmail();
            String subject = "Scorecard Scoring,";
            String template = "Good day, \n\n"
                    + "Please note that " + supervisor.getFullName() + " has submitted the agreed scores for your scorecard. "
                    + "A moderator will approve before moderation capture proceeds.\n"
                    + "You can now log in and view results.\n"
                    + "Link: "+ currentURL +"\n\n";

            String subject2 = "Scorecard Moderation,";
            String template2 = "Good day, \n\n"
                    + "Please note that " + supervisor.getFullName() + " has submitted agreed scores with " + owner.getFullName() + ". "
                    + "Please review and approve before moderation capture starts.\n"
                    + "You can now log in and see results.\n"
                    + "Link: "+ currentURL + "\n\n";

            try {
                sendScorecardEmail(request, recipient, subject, template);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            try {
                sendToHrGroup(request, subject2, template2);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Notification to scorecard moderation approvers failed to send.", request);
            }

            PortletUtils.addInfoMsg("Agreed scores submitted successfully. Email notifications will be sent to " + owner.getFullName() + " and HR for moderation approval.", request);

        }catch (Exception e){

            String recipient = commonService.getAdminEmail();
            String subject = "Scorecard Moderation,";
            String template = "Good day, \n\n"
                    + "Please note that " + loggedUser.getFullName() + " has failed to submit a moderated scorecard for" + owner.getFullName() + ". "
                    + "Kindly assist\n\n";
            try {
                sendScorecardEmail(request, recipient, subject, template);
            }catch (Exception x){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            PortletUtils.addErrorMsg("Scorecard wasn't submitted. A technical failure notification will be sent to the administrator.", request);
        }

        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping(value = "/moderator-approve-agreed-scores", method = RequestMethod.POST)
    public String moderatorApproveAgreedScores(HttpServletRequest request, Long id) throws MalformedURLException {
        Scorecard scorecard = scorecardService.getScorecardById(id);
        if (scorecard == null) {
            PortletUtils.addErrorMsg("Scorecard not found.", request);
            return "redirect:/scorecards";
        }
        if (!commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_AGREED_SCORES, scorecard)) {
            PortletUtils.addErrorMsg("You are not allowed to approve agreed scores on this scorecard.", request);
            return "redirect:/scorecards/view-scorecard/" + id;
        }

        if (!moveActiveReportingDateStage(scorecard, PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING, request)) {
            return "redirect:/scorecards/view-scorecard/" + scorecard.getId();
        }

        Account owner = scorecard.getOwner();
        Account supervisor = owner == null ? null : owner.getSupervisor();
        Account moderator = commonService.getLoggedUser();
        URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/" + scorecard.getId()));

        String ownerRecipient = owner == null ? null : owner.getEmail();
        String supervisorRecipient = supervisor == null ? null : supervisor.getEmail();
        String subject = "Scorecard Moderation,";
        String ownerTemplate = "Good day, \n\n"
                + "Please note that " + (moderator != null ? moderator.getFullName() : "the moderator") + " approved the agreed scores. "
                + "Moderation score capture can now proceed.\n"
                + "Link: " + currentURL + "\n\n";
        String supervisorTemplate = "Good day, \n\n"
                + "Please note that " + (moderator != null ? moderator.getFullName() : "the moderator") + " approved agreed scores for "
                + (owner != null ? owner.getFullName() : "the scorecard owner") + ". "
                + "Moderation score capture can now proceed.\n"
                + "Link: " + currentURL + "\n\n";
        try {
            sendScorecardEmail(request, ownerRecipient, subject, ownerTemplate);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Email to " + ownerRecipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }
        try {
            sendScorecardEmail(request, supervisorRecipient, subject, supervisorTemplate);
        } catch (Exception exception) {
            PortletUtils.addErrorMsg("Email to " + supervisorRecipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }

        PortletUtils.addInfoMsg("Agreed scores approved successfully. Moderator score capture can now proceed.", request);
        return "redirect:/scorecards/view-scorecard/" + scorecard.getId();
    }
 @RequestMapping(value = "/submit-moderated-scores", method = RequestMethod.POST)
 public String submitModeratedScorecard(HttpServletRequest request, Scorecard updatedScorecard) {

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());
        if (scorecard == null || !commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES, scorecard)) {
            PortletUtils.addErrorMsg("You are not allowed to submit moderated scores for this scorecard.", request);
            return "redirect:/scorecards";
        }
        Account loggedUser = commonService.getLoggedUser();
        Account supervisor = scorecard.getOwner().getSupervisor();
        Account owner = scorecard.getOwner();

        try {
            if (!moveActiveReportingDateStage(scorecard, PMConstants.SCORECARD_STAGE_CLOSED, request)) {
                return "redirect:/scorecards/view-scorecard/" + scorecard.getId();
            }

            URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
            String recipient = owner.getEmail();
            String subject = "Scorecard Moderation,";
            String template = "Good day, \n\n"
                    + "Please note that " + loggedUser.getFullName() + " has moderated your scorecard. "
                    + "You can now login and see results\n"
                    + "Link: "+ currentURL + "\n\n";

            String recipient2 = supervisor.getEmail();
            String subject2 = "Scorecard Moderation,";
            String template2 = "Good day, \n\n"
                    + "Please note that " + loggedUser.getFullName() + " has moderated " + owner.getFullName() + "'s scorecard. "
                    + "You can now log in and see results\n\n";

            try {
                sendScorecardEmail(request, recipient, subject, template);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            try {
                sendScorecardEmail(request, recipient2, subject2, template2);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient2 + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }

            PortletUtils.addInfoMsg("Scorecard successfully moderated by HR. Emails were sent to "+ owner.getFullName()+" and "+ supervisor.getFullName(), request);

        }catch (Exception e){

            String recipient = commonService.getAdminEmail();
            String subject = "Scorecard Moderation,";
            String template = "Good day, \n\n"
                    + "Please note that " + loggedUser.getFullName() + " has failed to submit a moderated scorecard for" + owner.getFullName() + ". "
                    + "Kindly assist\n\n";
            try {
                sendScorecardEmail(request, recipient, subject, template);
            }catch (Exception x){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            PortletUtils.addErrorMsg("Scorecard wasn't submitted. A technical failure notification will be sent to the administrator.", request);
        }

        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping(value = "/supervisor-approve", method = RequestMethod.POST)
    public String supervisorApproveScorecard(HttpServletRequest request, Long id) throws UnsupportedEncodingException {

        Scorecard scorecard = scorecardService.getScorecardById(id);
        String supervisor = scorecard.getOwner().getSupervisor().getFullName();
        String owner = scorecard.getOwner().getFullName();
        String recipient = scorecard.getOwner().getEmail();
        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        scorecard.setApprovalStatus(workflow.getApprovedBySupervisorStatus());

        try {
            scorecardService.saveScorecard(scorecard);
            try {
                Account loggedUser = commonService.getLoggedUser();
                Approval approval = new Approval();
                approval.setScorecard(scorecard);
                approval.setAccount(loggedUser);
                approval.setStatus(workflow.getApprovedBySupervisorStatus());
                approvalService.addApproval(approval);
            }catch (Exception e){
                log.warn("Failed to persist supervisor approval audit for scorecardId={}", scorecard.getId(), e);
            }
            URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
            String subject = "Scorecard Approval,";
            String template = "Good day "+ owner + ", \n\n"
                            + "Please note that "+ supervisor +" has approved your scorecard. "
                            + "We are now waiting for HR to approve so that you can proceed with capturing scores. \n"
                            + "Link: "+ currentURL +"\n\n";
            try {
                sendScorecardEmail(request, recipient, subject, template);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }


            String subject2 = "Scorecard Approval,";
            String template2 = "Good day HR, \n\n"
                            + "Please note that "+ supervisor +" has approved "+owner+"'s scorecard. "
                            + "You are now eligible to review and approve so that they can proceed with capturing scores. \n"
                            + "Link: "+ currentURL +"\n\n";
            try {
                sendToHrGroup(request, subject2, template2);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Notification to scorecard HR approvers failed to send.", request);
            }


            PortletUtils.addInfoMsg("Scorecard successfully approved. Email notifications will be sent to HR for further approval and to "+ owner + " as feedback", request);
            return "redirect:/scorecards/view-scorecard/"+ id;
        }catch (Exception e){

            String admin = commonService.getAdminEmail();
            String subject  = "Technical failure,";
            String template = "Good day, \n\n"
                            + "Please note that "+ supervisor +"  failed to approve "+ owner +"'s scorecard. "
                            + "With error :. \n\n"
                            + e.getMessage() +"\n\n";
            try {
                sendScorecardEmail(request, admin, subject, template);
            }catch (Exception x){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            PortletUtils.addInfoMsg("Scorecard approval failed. A technical failure notification will be sent to the administrator with error details. ", request);
            return "redirect:/scorecards/view-scorecard/"+ id;
        }
    }

    @RequestMapping(value = "/supervisor-reject", method = RequestMethod.POST)
    public String supervisorRejectScorecard(HttpServletRequest request, Long id, String message) throws UnsupportedEncodingException {

        Scorecard scorecard = scorecardService.getScorecardById(id);
        String supervisor = scorecard.getOwner().getSupervisor().getFullName();
        String owner = scorecard.getOwner().getFullName();
        String recipient = scorecard.getOwner().getEmail();
        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        scorecard.setApprovalStatus(workflow.getRejectedBySupervisorStatus());
        scorecard.setLockStatus(PMConstants.LOCK_STATUS_OPEN);

        try{
            scorecardService.saveScorecard(scorecard);

            try {
                Account loggedUser = commonService.getLoggedUser();
                Approval approval = new Approval();
                approval.setScorecard(scorecard);
                approval.setAccount(loggedUser);
                approval.setMessage(message);
                approval.setStatus(workflow.getRejectedBySupervisorStatus());
                approvalService.addApproval(approval);
            }catch (Exception e){
                log.warn("Failed to persist supervisor rejection audit for scorecardId={}", scorecard.getId(), e);
            }
            URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
            String subject = "Scorecard Approval,";
            String template = "Good day "+ owner + ", \n\n"
                            + "Please note that "+ supervisor +" has rejected your scorecard with the following message \n\n. "
                            +".........................................................................................\n"
                            + message + "\n"
                            +".........................................................................................\n\n"
                            + "Please log in and make recommended changes. Also look for comments and flags on your scorecard and rectify\n"
                            + "Link: "+currentURL +"\n\n";
            try {
                sendScorecardEmail(request, recipient, subject, template);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }

            PortletUtils.addInfoMsg("Scorecard successfully rejected. An email notification will be sent to "+ owner + " as feedback", request);

        }catch (Exception e){

            String admin = commonService.getAdminEmail();
            String subject  = "Technical failure,";
            String template = "Good day, \n\n"
                    + "Please note that "+ supervisor +"  failed to approve "+ owner +"'s scorecard. "
                    + "With error :. \n\n"
                    + e.getMessage() +"\n\n";
            try {
                sendScorecardEmail(request, recipient, subject, template);
            }catch (Exception x){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            PortletUtils.addInfoMsg("Scorecard approval failed. A technical failure notification will be sent to the administrator with error details. ", request);
        }

        return "redirect:/scorecards/view-scorecard/"+ id;
    }

    @RequestMapping(value = "/hr-approve", method = RequestMethod.POST)
    public String hrApproveScorecard(HttpServletRequest request, Long id) throws UnsupportedEncodingException {

        Scorecard scorecard = scorecardService.getScorecardById(id);
        if (scorecard == null || !commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_SCORECARD, scorecard)) {
            PortletUtils.addErrorMsg("You are not allowed to approve this scorecard at the HR stage.", request);
            return "redirect:/scorecards";
        }
        String supervisor = scorecard.getOwner().getSupervisor().getFullName();
        String owner = scorecard.getOwner().getFullName();
        String recipient = scorecard.getOwner().getSupervisor().getEmail();
        Account loggedUser = commonService.getLoggedUser();
        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        scorecard.setApprovalStatus(workflow.getApprovedByHrStatus());
        scorecard.setLockStatus(PMConstants.LOCK_STATUS_OPEN);

        try {
            scorecardService.saveScorecard(scorecard);
            try {
                Approval approval = new Approval();
                approval.setScorecard(scorecard);
                approval.setAccount(loggedUser);
                approval.setStatus(workflow.getApprovedByHrStatus());
                approvalService.addApproval(approval);
            }catch (Exception e){
                log.warn("Failed to persist HR approval audit for scorecardId={}", scorecard.getId(), e);
            }
            URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
            String subject = "Scorecard Approval,";
            String template = "Good day "+ supervisor + ", \n\n"
                            + "Please note that "+ loggedUser.getFullName() +" has approved "+ owner +" scorecard. "
                            + "The owner is now eligible for capturing scores. \n"
                            + "Link: "+ currentURL +"\n\n";
            try {
                sendScorecardEmail(request, recipient, subject, template);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }


            String recipient2 = scorecard.getOwner().getEmail();
            String subject2 = "Scorecard Approval,";
            String template2 = "Good day "+owner+", \n\n"
                    + "Please note that "+ loggedUser.getFullName() +" has your scorecard. "
                    + "You are now eligible to capture scores. \n"
                    + "Link: "+ currentURL +"\n\n";
            try {
                sendScorecardEmail(request, recipient2, subject2, template2);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }


            PortletUtils.addInfoMsg("Scorecard successfully approved. Email notifications will be sent to "+supervisor+" and to "+ owner + " as feedback", request);
            return "redirect:/scorecards/view-scorecard/"+ id;
        }catch (Exception e){

            String admin = commonService.getAdminEmail();
            String subject  = "Technical failure,";
            String template = "Good day, \n\n"
                    + "Please note that "+ loggedUser.getFullName() +"  failed to approve "+ owner +"'s scorecard. "
                    + "With error :. \n\n"
                    + e.getMessage() +"\n\n";
            try {
                sendScorecardEmail(request, admin, subject, template);
            }catch (Exception x){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            PortletUtils.addInfoMsg("Scorecard approval failed. A technical failure notification will be sent to the administrator with error details. ", request);
            return "redirect:/scorecards/view-scorecard/"+ id;
        }
    }

    @RequestMapping(value = "/hr-reject", method = RequestMethod.POST)
    public String hrRejectScorecard(HttpServletRequest request, Long id, String message) throws UnsupportedEncodingException {

        Scorecard scorecard = scorecardService.getScorecardById(id);
        if (scorecard == null || !commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_SCORECARD, scorecard)) {
            PortletUtils.addErrorMsg("You are not allowed to reject this scorecard at the HR stage.", request);
            return "redirect:/scorecards";
        }
        String supervisor = scorecard.getOwner().getSupervisor().getFullName();
        String owner = scorecard.getOwner().getFullName();
        String recipient = scorecard.getOwner().getSupervisor().getEmail();
        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        scorecard.setApprovalStatus(workflow.getRejectedByHrStatus());
        Account loggedUser = commonService.getLoggedUser();

        try{
            scorecardService.saveScorecard(scorecard);

            try {

                Approval approval = new Approval();
                approval.setScorecard(scorecard);
                approval.setAccount(loggedUser);
                approval.setMessage(message);
                approval.setStatus(workflow.getRejectedByHrStatus());
                approvalService.addApproval(approval);
            }catch (Exception e){
              log.warn("Failed to persist HR rejection audit for scorecardId={}", scorecard.getId(), e);
            }
            URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
            String subject = "Scorecard Approval,";
            String template = "Good day "+ supervisor + ", \n\n"
                            + "Please note that "+ loggedUser.getFullName() +" has rejected "+ owner+"'s scorecard with the following message \n\n. "
                            +".........................................................................................\n"
                            + message + "\n"
                            +".........................................................................................\n\n"
                            + "Please log in and make recommended changes. Also look for comments and flags on your scorecard and rectify\n"
                            + "Link: "+ currentURL + "\n\n";
            try {
                sendScorecardEmail(request, recipient, subject, template);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }

            PortletUtils.addInfoMsg("Scorecard successfully rejected. An email notification will be sent to "+ supervisor + " as feedback", request);

        }catch (Exception e){

            String admin = commonService.getAdminEmail();
            String subject  = "Technical failure,";
            String template = "Good day, \n\n"
                    + "Please note that "+ loggedUser.getFullName() +"  failed to reject "+ owner +"'s scorecard. "
                    + "With error :. \n\n"
                    + e.getMessage() +"\n\n";
            try {
                sendScorecardEmail(request, admin, subject, template);
            }catch (Exception x){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            PortletUtils.addInfoMsg("Scorecard approval failed. A technical failure notification will be sent to the administrator with error details. ", request);
        }

        return "redirect:/scorecards/view-scorecard/"+ id;
    }

    @RequestMapping("/view-scorecard/{id}")
    public ModelAndView viewScorecard(@PathVariable("id") long id, HttpServletRequest request, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_SCORECARD);
        try {
            if (id <= 0) {
                PortletUtils.addErrorMsg("Invalid scorecard reference.", request);
                modelAndView = new ModelAndView(Pages.BLANK_PAGE);
                preparePage(modelAndView, request, session);
                return modelAndView;
            }

            Scorecard scorecard = scorecardService.getScorecardById(id);
            if (scorecard == null) {
                PortletUtils.addErrorMsg("Scorecard #" + id + " cannot be found.", request);
                modelAndView = new ModelAndView(Pages.BLANK_PAGE);
                preparePage(modelAndView, request, session);
                return modelAndView;
            }
            if (!canViewScorecard(scorecard)) {
                PortletUtils.addErrorMsg("You are not allowed to view this scorecard.", request);
                modelAndView = new ModelAndView(Pages.BLANK_PAGE);
                preparePage(modelAndView, request, session);
                return modelAndView;
            }

            String scorecardModel = scorecard.getScorecardModel() != null
                    ? scorecard.getScorecardModel().getName()
                    : PMConstants.STANDARD_SCORECARD;
            String hierarchyModel = resolveHierarchyModel(scorecard);
            int displayHierarchyColumnCount = resolveDisplayHierarchyColumnCount(hierarchyModel);
            ReportingPeriod reportingPeriod = scorecard.getReportingPeriod();
            List<ReportingDate> reportingDates = reportingDateService.listAllReportingDates(reportingPeriod);
            double averageEmployeeScore = goalService.getAverageEmployeeScore(id);
            double averageManagerScore = goalService.getAverageManagerScore(id);
            double averageAgreedScore = goalService.getAverageAgreedScore(id);
            double averageModeratedScore = goalService.getAverageModeratorScore(id);
            double totalAllocatedWeight = isLegacyHierarchyModel(hierarchyModel)
                    ? outcomeService.getTotalAllocatedWeight(id)
                    : goalService.getTotalAllocatedWeight(id);
            List<Target> targetsList;
            if (isLegacyHierarchyModel(hierarchyModel)) {
                List<Gear> selectedGears = gearService.listSelectedGears(scorecard);
                targetsList = attachLegacyTargets(scorecard, selectedGears);
                mergeHydratedTargetSnapshots(scorecard.getId(), targetsList);
                modelAndView.addObject("selectedGears", selectedGears);
            } else {
                targetsList = targetService.getAllTargetsByScorecard(id);
            }
            double totalWeightedScore = 0.0;
            for(Target target: targetsList){
                if(target.getWeightedScore() !=null){
                    totalWeightedScore += target.getWeightedScore();
                }

            }

            modelAndView.addObject("pageTitle", "View Scorecard {"+ scorecard.getOwner().getFullName() +"}");
            modelAndView.addObject("scorecard", scorecard);
            modelAndView.addObject("id", scorecard.getId());
            modelAndView.addObject("scorecardModel", scorecardModel);
            modelAndView.addObject("targetsList", targetsList);
            modelAndView.addObject("targetRows", buildTargetCaptureRows(targetsList));
            addScorecardDisplayModel(modelAndView, scorecard, targetsList, reportingDates);
            List<ScorecardDisplaySection> displaySections =
                    (List<ScorecardDisplaySection>) modelAndView.getModel().get("displaySections");
            modelAndView.addObject("comment", new Comment());
            modelAndView.addObject("reportingDates", reportingDates);
            modelAndView.addObject("activeReportingDateId", resolveDefaultReportingDateId(reportingDates));
            modelAndView.addObject("selfAssessmentPipsByReportingDate", buildSelfAssessmentPipsByReportingDate(scorecard, reportingDates));
            modelAndView.addObject("overallScoresByReportingDate", buildOverallScoresByReportingDate(scorecard, reportingDates));
            modelAndView.addObject("overallCommentsByReportingDate", buildOverallCommentsByReportingDate(scorecard));
            modelAndView.addObject(
                    "displayWeightedScoreTotalsByReportingDate",
                    buildDisplayWeightedScoreTotalsByReportingDate(displaySections, reportingDates, hierarchyModel)
            );
            modelAndView.addObject("averageEmployeeScore", averageEmployeeScore);
            modelAndView.addObject("averageManagerScore", averageManagerScore);
            modelAndView.addObject("averageAgreedScore", averageAgreedScore);
            modelAndView.addObject("averageModeratedScore", averageModeratedScore);
            modelAndView.addObject("totalAllocatedWeight", totalAllocatedWeight);
            modelAndView.addObject("totalWeightedScore", totalWeightedScore);
            modelAndView.addObject("viewScoreColumnCount", resolveViewScoreColumnCount(scorecardModel));
            modelAndView.addObject("viewPreScoreColumnCount", displayHierarchyColumnCount + 5);
            modelAndView.addObject("viewSummaryLabelColspan", resolveViewSummaryLabelColspan(displayHierarchyColumnCount));
            modelAndView.addObject("viewTableColumnCount", resolveViewTableColumnCount(displayHierarchyColumnCount, scorecardModel));
            modelAndView.addObject("isSupervisor", commonService.isSupervisor(scorecard.getOwner()));
            ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
            String contractRole = resolveContractStageRole(scorecard);
            ReportingDate activeReportingDate = resolveActiveReportingDate();
            String reportingDateRole = resolveReportingDateRole(scorecard, activeReportingDate);
            boolean ownerCaptureStage = matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_OWNER_SCORING);
            boolean supervisorCaptureStage = matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING);
            boolean agreedCaptureStage = matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING);
            boolean moderatedCaptureStage = matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING);
            modelAndView.addObject("ownerCaptureStage", ownerCaptureStage);
            modelAndView.addObject("supervisorCaptureStage", supervisorCaptureStage);
            modelAndView.addObject("agreedCaptureStage", agreedCaptureStage);
            modelAndView.addObject("moderatedCaptureStage", moderatedCaptureStage);
            modelAndView.addObject("canViewReport", canViewScorecardReport(scorecard));
            ScorecardActionState actionState = resolveScorecardActionState(scorecard, contractRole, reportingDateRole, workflow);
            modelAndView.addObject("canSupervisorApproveTargets", actionState.canSupervisorApproveTargets);
            modelAndView.addObject("canHrApproveTargets", actionState.canHrApproveTargets);
            modelAndView.addObject("canCaptureTargets", actionState.canCaptureTargets);
            modelAndView.addObject("canCaptureEmployeeScore", actionState.canCaptureEmployeeScore);
            modelAndView.addObject("canCaptureManagerScore", actionState.canCaptureManagerScore);
            modelAndView.addObject("canCaptureAgreedScoreAgreedScore", actionState.canCaptureAgreedScore);
            modelAndView.addObject("canModerate", actionState.canModerate);
            modelAndView.addObject("canApproveOwnerScores", actionState.canApproveOwnerScores);
            modelAndView.addObject("canApproveAgreedScores", actionState.canApproveAgreedScores);
            modelAndView.addObject("scorecardActivityRestrictionNotice",
                    resolveScorecardActivityRestrictionNotice(
                            scorecard,
                            activeReportingDate,
                            actionState.hasAllowedAction(),
                            actionState.expectedActivity
                    ));

        }catch (Exception e){
            log.error("Failed to open scorecard id={}", id, e);
            PortletUtils.addErrorMsg("The scorecard could not be opened. Please check scorecard setup and try again.", request);
            modelAndView = new ModelAndView(Pages.BLANK_PAGE);
        }

        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping(value = "/save-comment", method = RequestMethod.POST)
    public String saveComment(HttpServletRequest request,
                              Comment comment,
                              @RequestParam(value = "target", required = false) Long targetId,
                              @RequestParam(value = "returnUrl", required = false) String returnUrl) throws MalformedURLException {
        Account loggedUser = commonService.getLoggedUser();
        Target target = resolveSubmittedCommentTarget(comment, targetId);
        if (target == null) {
            PortletUtils.addErrorMsg("Target could not be resolved.", request);
            return redirectAfterTargetNote(returnUrl, 0);
        }
        comment.setTarget(target);
        comment.setSender(loggedUser);
        commentService.saveComment(comment);
        Scorecard scorecard = resolveScorecardFromTarget(target);
        long scorecardId = scorecard == null ? 0 : scorecard.getId();
        URL link = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecardId));

        String recipient = scorecard == null || scorecard.getOwner() == null ? null : scorecard.getOwner().getEmail();
        String subject = "Scorecard Comment,";
        String template = "Good day, \n\n"
                + "Please note that"+ loggedUser.getFullName() +" added a comment on your scorecard. "
                + "Goal - measure: [" + resolveTargetGoalName(target) +" - "+ target.getMeasure() +"]\n"
                + "Message: "+ comment.getName() + "\n"
                + "You can now login and response or action\n"
                + "Link: "+ link + "\n\n";
        try {
            sendScorecardEmail(request, recipient, subject, template);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }
        PortletUtils.addInfoMsg("Comment successfully saved", request);
        return redirectAfterTargetNote(returnUrl, scorecardId);
    }

    private Target resolveSubmittedCommentTarget(Comment comment, Long targetId) {
        long resolvedTargetId = targetId == null ? 0 : targetId;
        if (resolvedTargetId <= 0 && comment != null && comment.getTarget() != null) {
            resolvedTargetId = comment.getTarget().getId();
        }
        return resolvedTargetId > 0 ? targetService.getTargetById(resolvedTargetId) : null;
    }

    @RequestMapping(value = "/save-flag", method = RequestMethod.POST)
    public String saveFlag(HttpServletRequest request, Target updatedTarget, @RequestParam(value = "returnUrl", required = false) String returnUrl) throws MalformedURLException {

        Target target = targetService.getTargetById(updatedTarget.getId());
        if (target == null) {
            PortletUtils.addErrorMsg("Target could not be resolved.", request);
            return redirectAfterTargetNote(returnUrl, 0);
        }
        Scorecard scorecard = resolveScorecardFromTarget(target);
        long scorecardId = scorecard == null ? 0 : scorecard.getId();
        target.setFlag(updatedTarget.getFlag());
        targetService.saveTarget(target);
        URL link = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecardId));
        String fullName = commonService.getLoggedUser().getFullName();

        String recipient = scorecard == null || scorecard.getOwner() == null ? null : scorecard.getOwner().getEmail();
        String subject = "Scorecard Comment,";
        String template = "Good day, \n\n"
                + "Please note that"+ fullName +" flagged a goal on your scorecard. "
                + "Goal - measure: [" + resolveTargetGoalName(target) +" - "+ target.getMeasure() +"]\n"
                + "Message: "+ target.getFlag() + "\n"
                + "You can now login and response or action\n"
                + "Link: "+ link + "\n\n";
        try {
            sendScorecardEmail(request, recipient, subject, template);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }
        PortletUtils.addInfoMsg("Measure successfully flagged and the reason was saved", request);
        return redirectAfterTargetNote(returnUrl, scorecardId);
    }

    private String redirectAfterTargetNote(String returnUrl, long scorecardId) {
        if (StringUtils.hasText(returnUrl) && returnUrl.startsWith("/") && !returnUrl.startsWith("//")) {
            return "redirect:" + returnUrl;
        }
        if (scorecardId > 0) {
            return "redirect:/scorecards/view-scorecard/" + scorecardId;
        }
        return "redirect:/scorecards";
    }

    private String resolveTargetGoalName(Target target) {
        if (target != null && target.getGoal() != null && StringUtils.hasText(target.getGoal().getName())) {
            return target.getGoal().getName();
        }
        if (target != null && target.getOutcome() != null && StringUtils.hasText(target.getOutcome().getName())) {
            return target.getOutcome().getName();
        }
        return "Target";
    }

    @RequestMapping(value = "/save-standard-score", method = RequestMethod.POST, consumes = {"*/*"})
    public void saveStandardScore( HttpServletResponse response, Long targetId, Double actual, String evidence, String justification) {
        if (targetId == null) {
            writeScoreSaveResponse(response, true, "Target reference is required");
            return;
        }
        Target target = targetService.getTargetById(targetId);
        if (target == null) {
            writeScoreSaveResponse(response, true, "Target could not be resolved");
            return;
        }
        Scorecard scorecard = resolveScorecardFromTarget(target);
        if (scorecard == null) {
            writeScoreSaveResponse(response, true, "Scorecard could not be resolved");
            return;
        }
        if (hasReportingDateConflict(scorecard)) {
            writeScoreSaveResponse(response, true, "Score capture is blocked: multiple OPEN/ACTIVE reporting dates exist. Contact an administrator.");
            return;
        }
        if (!isAnyScoreCaptureAllowed(scorecard)) {
            writeScoreSaveResponse(response, true, "You are not allowed to capture scores for this scorecard at the current stage");
            return;
        }

        ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
        if (!reportingDateService.isReportingDateOpen(reportingDate)) {
            writeScoreSaveResponse(response, true, "Scores can only be captured for an open reporting date");
            return;
        }
        if (!isReportingDateForScorecard(scorecard, reportingDate)) {
            writeScoreSaveResponse(response, true, "Scores can only be captured for the open reporting date in this scorecard's reporting period");
            return;
        }

            Score score = new Score();
            score.setTarget(target);
            score.setOutput(target.getOutput());
            score.setReportingDate(reportingDate);
            score.setEvidence(evidence);
            score.setJustification(justification);
            score.setActual(actual);

        standardScorecardScoreService.saveScore(score);
        writeScoreSaveResponse(response);

    }

    @RequestMapping(value = "/save-value-based-employee-score", method = RequestMethod.POST)
    public void saveEmployeeScore(HttpServletResponse response, Long targetId, Double employeeScore, String justification) {
        saveValueBasedScore(response, targetId, employeeScore, justification, ValueBasedCaptureStage.EMPLOYEE);
    }

    @RequestMapping(value = "/save-value-based-evidence", method = RequestMethod.POST)
    public String saveValueBasedEvidence(EvidenceWrapper wrapper, HttpServletRequest request){
        Long scorecardId = null;
        try {
            Target target = targetService.getTargetById(wrapper.getTargetId());
            if (target == null) {
                PortletUtils.addErrorMsg("Target could not be resolved for evidence upload.", request);
                return "redirect:/scorecards";
            }

            Scorecard scorecard = resolveScorecardFromTarget(target);
            if (scorecard == null) {
                PortletUtils.addErrorMsg("Scorecard could not be resolved for evidence upload.", request);
                return "redirect:/scorecards";
            }
            scorecardId = scorecard.getId();

            if (hasReportingDateConflict(scorecard)) {
                PortletUtils.addErrorMsg("Score capture is blocked: multiple OPEN/ACTIVE reporting dates exist. Contact an administrator.", request);
                return "redirect:/scorecards/capture-scores/"+ scorecard.getId();
            }

            if(!commonService.isOwner(scorecard)){
                PortletUtils.addErrorMsg("You are not allowed to upload evidence for this scorecard.", request);
                return "redirect:/scorecards/capture-scores/"+ scorecard.getId();
            }

            ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
            if (!reportingDateService.isReportingDateOpen(reportingDate)) {
                PortletUtils.addErrorMsg("Scores can only be captured for an open reporting date", request);
                return "redirect:/scorecards/capture-scores/"+ scorecard.getId();
            }
            if (!isReportingDateForScorecard(scorecard, reportingDate)) {
                PortletUtils.addErrorMsg("Scores can only be captured for the open reporting date in this scorecard's reporting period", request);
                return "redirect:/scorecards/capture-scores/"+ scorecard.getId();
            }

            Evidence evidence = new Evidence();
            evidence.setTarget(target);
            evidence.setReportingDate(reportingDate);
            evidence.setEvidence(wrapper.getEvidence());

            MultipartFile file = wrapper.getAttachment();
            if (file != null && !file.isEmpty()) {
                String storedFileName = storeEvidenceFile(file);
                evidence.setAttachmentName(storedFileName);
            }

            evidenceService.saveEvidence(evidence);
            PortletUtils.addInfoMsg("Evidence saved successfully.", request);
        }catch (IllegalArgumentException exception){
            PortletUtils.addErrorMsg(exception.getMessage(), request, exception);
        }catch (IOException exception) {
            PortletUtils.addErrorMsg("Evidence upload failed. Please retry with a supported file.", request, exception);
        }catch (Exception exception){
            PortletUtils.addErrorMsg("Evidence could not be saved. Please try again.", request, exception);
        }
        if (scorecardId == null) {
            return "redirect:/scorecards";
        }
        return "redirect:/scorecards/capture-scores/"+ scorecardId;
    }

    @RequestMapping(value = "/save-value-based-manager-score", method = RequestMethod.POST, consumes = {"*/*"})
    public void saveManagerScore(HttpServletResponse response, Long targetId, Double managerScore) {
        saveValueBasedScore(response, targetId, managerScore, null, ValueBasedCaptureStage.MANAGER);
    }

    @RequestMapping(value = "/save-overall-score", method = RequestMethod.POST, consumes = {"*/*"})
    public void saveOverallScore(HttpServletResponse response, Long scorecardId, String userType, Double score) {
        try {
            if (scorecardId == null || scorecardId <= 0) {
                writeScoreSaveResponse(response, true, "Scorecard reference is required");
                return;
            }
            if (score == null || score < 1.0 || score > 5.0) {
                writeScoreSaveResponse(response, true, "Score must be between 1 and 5");
                return;
            }
            Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
            if (scorecard == null) {
                writeScoreSaveResponse(response, true, "Scorecard could not be resolved");
                return;
            }
            if (!PMConstants.USER_TYPE_MODERATOR.equalsIgnoreCase(userType)
                    || !isStageCaptureAllowed(scorecard, ValueBasedCaptureStage.MODERATED)) {
                writeScoreSaveResponse(response, true, "You are not allowed to capture the moderated score for this scorecard");
                return;
            }

            ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
            if (!reportingDateService.isReportingDateOpen(reportingDate)) {
                writeScoreSaveResponse(response, true, "Scores can only be captured for an open reporting date");
                return;
            }
            if (!isReportingDateForScorecard(scorecard, reportingDate)) {
                writeScoreSaveResponse(response, true, "Scores can only be captured for the open reporting date in this scorecard's reporting period");
                return;
            }

            OverallScore overallScore = overallScoreService.getOverallScoreByScorecardAndReportingDate(scorecard, reportingDate);
            overallScore.setModeratedOverall(score);
            overallScore = overallScoreService.saveOverallScore(overallScore);
            writeScoreSaveResponse(response, overallScore);
        } catch (Exception exception) {
            log.error("Failed to save overall score for scorecardId={}", scorecardId, exception);
            writeScoreSaveResponse(response, true, "Score could not be saved");
        }
    }

    @RequestMapping(value = "/save-value-based-agreed-score", method = RequestMethod.POST, consumes = {"*/*"})
    public void saveAgreedScore(HttpServletResponse response, Long targetId, Double agreedScore) {
        saveValueBasedScore(response, targetId, agreedScore, null, ValueBasedCaptureStage.AGREED);
    }

    @RequestMapping(value = "/save-value-based-moderated-score", method = RequestMethod.POST, consumes = {"*/*"})
    public void saveModeratedScore(HttpServletResponse response, Long targetId, Double moderatedScore) {
        saveValueBasedScore(response, targetId, moderatedScore, null, ValueBasedCaptureStage.MODERATED);
    }

    private void saveValueBasedScore(HttpServletResponse response, Long targetId, Double scoreValue, String justification, ValueBasedCaptureStage stage) {
        try {
            if (targetId == null) {
                writeScoreSaveResponse(response, true, "Target reference is required");
                return;
            }

            Target target = targetService.getTargetById(targetId);
            if (target == null) {
                writeScoreSaveResponse(response, true, "Target could not be resolved");
                return;
            }
            Scorecard scorecard = resolveScorecardFromTarget(target);
            if (scorecard == null) {
                writeScoreSaveResponse(response, true, "Scorecard could not be resolved");
                return;
            }
            if (hasReportingDateConflict(scorecard)) {
                writeScoreSaveResponse(response, true, "Score capture is blocked: multiple OPEN/ACTIVE reporting dates exist. Contact an administrator.");
                return;
            }
            ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
            if (reportingDate == null) {
                writeScoreSaveResponse(response, true, "No active reporting date is configured");
                return;
            }

            if (!reportingDateService.isReportingDateOpen(reportingDate)) {
                writeScoreSaveResponse(response, true, "Scores can only be captured for an open reporting date");
                return;
            }
            if (!isReportingDateForScorecard(scorecard, reportingDate)) {
                writeScoreSaveResponse(response, true, "Scores can only be captured for the open reporting date in this scorecard's reporting period");
                return;
            }
            if (!isValidValueBasedScore(scoreValue)) {
                writeScoreSaveResponse(response, true, "Score must be between 1 and 5");
                return;
            }

            if(isStageCaptureAllowed(scorecard, stage)){
                Score score = new Score();
                score.setTarget(target);
                score.setOutput(target.getOutput());
                score.setReportingDate(reportingDate);
                score.setJustification(justification);
                persistScoreByStage(score, scoreValue, stage);
                OverallScore overallScore = recalculateAndSaveValueBasedOverallScore(scorecard, reportingDate);
                writeScoreSaveResponse(response, overallScore);
                return;
            }
            writeScoreSaveResponse(response, true, "You are not allowed to capture this score at the current stage");
        }catch (Exception exception){
            log.error("Failed to save score for targetId={} stage={}", targetId, stage, exception);
            writeScoreSaveResponse(response, true, "Score could not be saved. Please retry.");
            return;
        }
    }

    private boolean isAnyScoreCaptureAllowed(Scorecard scorecard) {
        return isAnyScoreCaptureAllowed(scorecard, resolveActiveReportingDateRole(scorecard));
    }

    private boolean isAnyScoreCaptureAllowed(Scorecard scorecard, String reportingDateRole) {
        if (scorecard == null || !StringUtils.hasText(reportingDateRole)) {
            return false;
        }
        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_OWNER_SCORING)) {
            return commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES, scorecard);
        }
        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING)) {
            return commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES, scorecard);
        }
        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING)) {
            return commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES, scorecard);
        }
        if (matchesRole(reportingDateRole, PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING)) {
            return commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES, scorecard);
        }
        return false;
    }

    private String resolveScoreCaptureBlockedMessage(Scorecard scorecard, ReportingDate reportingDate) {
        if (hasReportingDateConflict(scorecard)) {
            return "Score capture is blocked because more than one reporting date is currently OPEN/ACTIVE. Contact an administrator.";
        }
        if (!isAnyScoreCaptureAllowed(scorecard)) {
            return "You are not allowed to capture scores for this scorecard at the current approval stage.";
        }
        if (reportingDate == null) {
            return "Score capture is unavailable because no active reporting date is configured.";
        }
        if (!reportingDateService.isReportingDateOpen(reportingDate)) {
            return "Score capture is unavailable because there is no OPEN reporting date.";
        }
        if (!isReportingDateForScorecard(scorecard, reportingDate)) {
            return "Score capture is unavailable because the OPEN reporting date is not in this scorecard's reporting period.";
        }
        return null;
    }

    private boolean hasReportingDateConflict(Scorecard scorecard) {
        if (scorecard == null) {
            return false;
        }
        long clientId = scorecard.getClientId();
        if (clientId <= 0 && scorecard.getOwner() != null) {
            clientId = scorecard.getOwner().getClientId();
        }
        return clientId > 0 && reportingDateService.hasMultipleOpenOrActiveReportingDates(clientId);
    }

    private Scorecard resolveScorecardForSelfAssessmentPip(Long scorecardId) {
        if (scorecardId == null || scorecardId <= 0) {
            return null;
        }
        return scorecardService.getScorecardById(scorecardId);
    }

    private Map<Long, List<PerformanceImprovementPlan>> buildSelfAssessmentPipsByReportingDate(Scorecard scorecard,
                                                                                               List<ReportingDate> reportingDates) {
        Map<Long, List<PerformanceImprovementPlan>> pipsByReportingDate = new HashMap<>();
        if (scorecard == null || reportingDates == null || reportingDates.isEmpty()) {
            return pipsByReportingDate;
        }
        for (ReportingDate reportingDate : reportingDates) {
            if (reportingDate == null) {
                continue;
            }
            List<PerformanceImprovementPlan> plans =
                    performanceImprovementPlanService.listSelfAssessmentPerformanceImprovementPlans(scorecard, reportingDate);
            pipsByReportingDate.put(reportingDate.getId(), plans == null ? Collections.emptyList() : plans);
        }
        return pipsByReportingDate;
    }

    private String resolveSelfAssessmentPipCaptureIssue(Scorecard scorecard, ReportingDate reportingDate) {
        if (scorecard == null) {
            return "Scorecard could not be resolved";
        }
        if (hasReportingDateConflict(scorecard)) {
            return "Score capture is blocked: multiple OPEN/ACTIVE reporting dates exist. Contact an administrator.";
        }
        if (reportingDate == null) {
            return "No active reporting date is configured";
        }
        if (!reportingDateService.isReportingDateOpen(reportingDate)) {
            return "Development interventions can only be captured for an open reporting date";
        }
        if (!isReportingDateForScorecard(scorecard, reportingDate)) {
            return "Development interventions can only be captured for the open reporting date in this scorecard's reporting period";
        }
        if (!commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES, scorecard)) {
            return "You are not allowed to capture development interventions for this scorecard at the current stage";
        }
        return null;
    }

    private PerformanceImprovementPlan resolveEditableSelfAssessmentPip(Long id, Scorecard scorecard, ReportingDate reportingDate) {
        if (id == null || id <= 0) {
            return null;
        }
        PerformanceImprovementPlan plan = performanceImprovementPlanService.getPerformanceImprovementPlanById(id);
        if (plan == null || !isSelfAssessmentPip(plan)) {
            return null;
        }
        if (plan.getScorecard() == null || scorecard == null || plan.getScorecard().getId() != scorecard.getId()) {
            return null;
        }
        return plan.getReportingDate() != null
                && reportingDate != null
                && plan.getReportingDate().getId() == reportingDate.getId()
                ? plan
                : null;
    }

    private Target resolveOptionalSelfAssessmentPipTarget(Long targetId, Scorecard scorecard) {
        if (targetId == null || targetId <= 0) {
            return null;
        }
        Target target = targetService.getTargetById(targetId);
        if (target == null || !isTargetOwnedByScorecard(target, scorecard)) {
            return null;
        }
        return target;
    }

    private boolean isTargetOwnedByScorecard(Target target, Scorecard scorecard) {
        Scorecard targetScorecard = resolveScorecardFromTarget(target);
        return targetScorecard != null && scorecard != null && targetScorecard.getId() == scorecard.getId();
    }

    private boolean isSelfAssessmentPip(PerformanceImprovementPlan plan) {
        return plan != null
                && PMConstants.PIP_SOURCE_SELF_ASSESSMENT.equalsIgnoreCase(defaultString(plan.getSource()));
    }

    private String resolveSelfAssessmentPipConflictMessage(Scorecard scorecard,
                                                           ReportingDate reportingDate,
                                                           long currentPlanId,
                                                           boolean savingNotApplicable) {
        List<PerformanceImprovementPlan> existingPlans =
                performanceImprovementPlanService.listSelfAssessmentPerformanceImprovementPlans(scorecard, reportingDate);
        for (PerformanceImprovementPlan existingPlan : existingPlans) {
            if (existingPlan == null || existingPlan.getId() == currentPlanId) {
                continue;
            }
            if (savingNotApplicable && !existingPlan.isNotApplicable()) {
                return "Remove existing development interventions before saving N/A.";
            }
            if (!savingNotApplicable && existingPlan.isNotApplicable()) {
                return "Remove the N/A row before adding development interventions.";
            }
        }
        return null;
    }

    private String validateSelfAssessmentPipFields(Target target,
                                                   String targetArea,
                                                   String concern,
                                                   String expectedStandard,
                                                   String agreedAction,
                                                   String requiredSupport,
                                                   String endDate,
                                                   boolean notApplicable) {
        if (notApplicable) {
            return null;
        }
        if (!StringUtils.hasText(targetArea) && target == null) {
            return "Development area or linked target is required.";
        }
        if (!StringUtils.hasText(concern)) {
            return "Identified gap is required.";
        }
        if (!StringUtils.hasText(expectedStandard)) {
            return "Expected outcome is required.";
        }
        if (!StringUtils.hasText(agreedAction)) {
            return "Training/development intervention is required.";
        }
        if (!StringUtils.hasText(requiredSupport)) {
            return "Required support is required.";
        }
        if (!StringUtils.hasText(endDate)) {
            return "Target date is required.";
        }
        try {
            LocalDate.parse(endDate.trim());
        } catch (DateTimeParseException exception) {
            return "Target date must be a valid date.";
        }
        return null;
    }

    private void applySelfAssessmentPipValues(PerformanceImprovementPlan plan,
                                              Scorecard scorecard,
                                              ReportingDate reportingDate,
                                              Target target,
                                              String targetArea,
                                              String concern,
                                              String expectedStandard,
                                              String agreedAction,
                                              String requiredSupport,
                                              String endDate,
                                              boolean notApplicable) {
        Account owner = scorecard.getOwner();
        plan.setClientId(resolveScorecardClientId(scorecard));
        plan.setEmployee(owner);
        plan.setReportingPeriod(scorecard.getReportingPeriod());
        plan.setScorecard(scorecard);
        plan.setReportingDate(reportingDate);
        plan.setTarget(notApplicable ? null : target);
        plan.setSource(PMConstants.PIP_SOURCE_SELF_ASSESSMENT);
        plan.setNotApplicable(notApplicable);

        if (notApplicable) {
            plan.setTargetArea("N/A");
            plan.setConcern("N/A");
            plan.setExpectedStandard("N/A");
            plan.setAgreedAction("N/A");
            plan.setRequiredSupport("N/A");
            plan.setEndDate(null);
            plan.setStatus(PMConstants.PIP_STATUS_NOT_APPLICABLE);
            plan.setProgress(100.0);
            return;
        }

        plan.setTargetArea(resolveSelfAssessmentTargetArea(target, targetArea));
        plan.setConcern(cleanText(concern));
        plan.setExpectedStandard(cleanText(expectedStandard));
        plan.setAgreedAction(cleanText(agreedAction));
        plan.setRequiredSupport(cleanText(requiredSupport));
        plan.setEndDate(cleanText(endDate));
        if (!StringUtils.hasText(plan.getStatus()) || PMConstants.PIP_STATUS_NOT_APPLICABLE.equalsIgnoreCase(plan.getStatus())) {
            plan.setStatus(PMConstants.PIP_STATUS_TODO);
            plan.setProgress(0.0);
        }
    }

    private long resolveScorecardClientId(Scorecard scorecard) {
        if (scorecard == null) {
            return 0L;
        }
        long clientId = scorecard.getClientId();
        if (clientId <= 0 && scorecard.getOwner() != null) {
            clientId = scorecard.getOwner().getClientId();
        }
        return clientId;
    }

    private String resolveSelfAssessmentTargetArea(Target target, String targetArea) {
        if (StringUtils.hasText(targetArea)) {
            return cleanText(targetArea);
        }
        if (target != null && StringUtils.hasText(target.getMeasure())) {
            return target.getMeasure().trim();
        }
        return "";
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultString(String value) {
        return value == null ? "" : value.trim();
    }

    private void writeSelfAssessmentPipResponse(HttpServletResponse response,
                                                boolean saved,
                                                String message,
                                                PerformanceImprovementPlan plan) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("saved", saved);
        if (message != null && !message.trim().isEmpty()) {
            jsonObject.put("message", message);
        }
        if (plan != null) {
            jsonObject.put("id", plan.getId());
            jsonObject.put("targetArea", defaultString(plan.getTargetArea()));
            jsonObject.put("concern", defaultString(plan.getConcern()));
            jsonObject.put("expectedStandard", defaultString(plan.getExpectedStandard()));
            jsonObject.put("agreedAction", defaultString(plan.getAgreedAction()));
            jsonObject.put("requiredSupport", defaultString(plan.getRequiredSupport()));
            jsonObject.put("endDate", defaultString(plan.getEndDate()));
            jsonObject.put("notApplicable", plan.isNotApplicable());
            jsonObject.put("targetId", plan.getTarget() == null ? "" : String.valueOf(plan.getTarget().getId()));
        }
        try(OutputStream outputStream = response.getOutputStream()){
            response.setContentType("application/json; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            outputStream.write(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
        }catch (IOException exception){
            log.error("Failed to write self-assessment PIP response", exception);
        }
    }

    private Scorecard resolveScorecardFromTarget(Target target) {
        if (target == null) {
            return null;
        }
        if (target.getOutput() != null && target.getOutput().getScorecard() != null) {
            long scorecardId = target.getOutput().getScorecard().getId();
            if (scorecardId > 0) {
                return scorecardService.getScorecardById(scorecardId);
            }
        }
        if (target.getGoal() != null && target.getGoal().getScorecardId() > 0) {
            return scorecardService.getScorecardById(target.getGoal().getScorecardId());
        }
        return null;
    }

    private String resolveReportingDateLabel(ReportingDate reportingDate) {
        if (reportingDate != null && StringUtils.hasText(reportingDate.getEndDate())) {
            return reportingDate.getEndDate();
        }
        return "N/A";
    }

    private boolean isStageCaptureAllowed(Scorecard scorecard, ValueBasedCaptureStage stage) {
        String reportingDateRole = resolveActiveReportingDateRole(scorecard);
        if (!matchesRole(reportingDateRole, roleKeyForCaptureStage(stage))) {
            return false;
        }
        switch (stage) {
            case EMPLOYEE:
                return commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES, scorecard);
            case MANAGER:
                return commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES, scorecard);
            case AGREED:
                return commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES, scorecard);
            case MODERATED:
                return commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES, scorecard);
            default:
                return false;
        }
    }

    private String roleKeyForCaptureStage(ValueBasedCaptureStage stage) {
        if (stage == null) {
            return null;
        }
        switch (stage) {
            case EMPLOYEE:
                return PMConstants.SCORECARD_STAGE_OWNER_SCORING;
            case MANAGER:
                return PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING;
            case AGREED:
                return PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING;
            case MODERATED:
                return PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING;
            default:
                return null;
        }
    }

    private boolean isValidValueBasedScore(Double scoreValue) {
        return scoreValue != null && scoreValue >= 1.0 && scoreValue <= 5.0;
    }

    private void persistScoreByStage(Score score, Double scoreValue, ValueBasedCaptureStage stage) {
        switch (stage) {
            case EMPLOYEE:
                score.setEmployeeScore(scoreValue);
                valueBasedScoreService.saveEmployeeScore(score);
                break;
            case MANAGER:
                score.setManagerScore(scoreValue);
                valueBasedScoreService.saveManagerScore(score);
                break;
            case AGREED:
                score.setAgreedScore(scoreValue);
                valueBasedScoreService.saveAgreedScore(score);
                break;
            case MODERATED:
                score.setModeratedScore(scoreValue);
                valueBasedScoreService.saveModeratedScore(score);
                break;
            default:
                break;
        }
    }

    private OverallScore recalculateAndSaveValueBasedOverallScore(Scorecard scorecard, ReportingDate reportingDate) {
        OverallScore overallScore = overallScoreService.getOverallScoreByScorecardAndReportingDate(scorecard, reportingDate);
        if (scorecard == null || reportingDate == null || reportingDate.getId() <= 0) {
            return overallScore;
        }

        String hierarchyModel = resolveHierarchyModel(scorecard);
        List<Target> targetsList = isLegacyHierarchyModel(hierarchyModel)
                ? attachLegacyTargets(scorecard, gearService.listSelectedGears(scorecard))
                : targetService.getAllTargetsByScorecard(scorecard.getId());
        List<ScorecardDisplaySection> displaySections = buildDisplaySections(scorecard, hierarchyModel, targetsList);
        hydrateDisplayScores(displaySections, Collections.singletonList(reportingDate), hierarchyModel);

        double employeeOverall = 0.0;
        double managerOverall = 0.0;
        double agreedOverall = 0.0;
        boolean legacyHierarchy = isLegacyHierarchyModel(hierarchyModel);
        for (ScorecardDisplaySection section : displaySections) {
            if (section == null || section.getRows() == null) {
                continue;
            }
            for (ScorecardDisplayRow row : section.getRows()) {
                if (row == null || row.getTarget() == null || (legacyHierarchy && !row.isShowOutput())) {
                    continue;
                }
                Score score = row.getScoresByReportingDate().get(reportingDate.getId());
                if (score == null) {
                    continue;
                }
                double weight = resolveValueBasedOverallWeight(row.getTarget(), hierarchyModel);
                employeeOverall += (score.getEmployeeScore() * weight) / 100.0;
                managerOverall += (score.getManagerScore() * weight) / 100.0;
                agreedOverall += (score.getAgreedScore() * weight) / 100.0;
            }
        }

        overallScore.setEmployeeOverall(roundTwoDecimals(employeeOverall));
        overallScore.setManagerOverall(roundTwoDecimals(managerOverall));
        overallScore.setAgreedOverall(roundTwoDecimals(agreedOverall));
        return overallScoreService.saveOverallScore(overallScore);
    }

    private double resolveValueBasedOverallWeight(Target target, String hierarchyModel) {
        if (target == null) {
            return 0.0;
        }
        if (isLegacyHierarchyModel(hierarchyModel)
                && target.getOutput() != null
                && target.getOutput().getAllocatedWeight() != null) {
            return target.getOutput().getAllocatedWeight();
        }
        if (target.getAllocatedWeight() != null) {
            return target.getAllocatedWeight();
        }
        if (target.getOutput() != null && target.getOutput().getAllocatedWeight() != null) {
            return target.getOutput().getAllocatedWeight();
        }
        return 0.0;
    }

    private void writeScoreSaveResponse(HttpServletResponse response) {
        writeScoreSaveResponse(response, false, null);
    }

    private void writeScoreSaveResponse(HttpServletResponse response, OverallScore overallScore) {
        writeScoreSaveResponse(response, false, null, overallScore);
    }

    private void writeScoreSaveResponse(HttpServletResponse response, boolean captureBlocked, String message) {
        writeScoreSaveResponse(response, captureBlocked, message, null);
    }

    private void writeScoreSaveResponse(HttpServletResponse response, boolean captureBlocked, String message, OverallScore overallScore) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("alreadyExists", false);
        jsonObject.put("captureBlocked", captureBlocked);
        if (message != null && !message.trim().isEmpty()) {
            jsonObject.put("message", message);
        }
        putOverallScoreValues(jsonObject, overallScore);
        String jsonString = jsonObject.toString();

        try(OutputStream outputStream = response.getOutputStream()){
            response.setContentType("application/json; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        }catch (IOException exception){
            log.error("Failed to write score save response", exception);
        }
    }

    private void putOverallScoreValues(JSONObject jsonObject, OverallScore overallScore) {
        if (jsonObject == null || overallScore == null) {
            return;
        }
        jsonObject.put("employeeOverall", formatScoreValue(overallScore.getEmployeeOverall()));
        jsonObject.put("managerOverall", formatScoreValue(overallScore.getManagerOverall()));
        jsonObject.put("agreedOverall", formatScoreValue(overallScore.getAgreedOverall()));
        jsonObject.put("moderatedOverall", formatScoreValue(overallScore.getModeratedOverall()));
    }

    private String formatScoreValue(Double value) {
        if (value == null) {
            return "";
        }
        return String.format(Locale.ENGLISH, "%.2f", value);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void sendToHrGroup(HttpServletRequest request,
                               String subject,
                               String body) throws UnsupportedEncodingException, MalformedURLException {
        String hrEmail = commonService.getHREmail();
        if (!StringUtils.hasText(hrEmail)) {
            throw new IllegalStateException("email.hr is not configured in System Settings.");
        }
        sendScorecardEmail(request, hrEmail.trim(), subject, body);
    }

    private void sendScorecardEmail(HttpServletRequest request, String recipient, String subject, String body) throws UnsupportedEncodingException, MalformedURLException {
        if (!shouldSendScorecardEmail(recipient)) {
            log.info("Skipping scorecard notification email to acting user: {}", recipient);
            return;
        }

        String normalizedSubject = normalizeScorecardEmailSubject(subject);
        String normalizedBody = normalizeScorecardEmailBody(body);

        String platformUrl = normalizeBaseUrl(commonService.getCurrentUrl(request));
        if (!platformUrl.isEmpty() && (!hasHttpLink(normalizedBody) || !normalizedBody.contains("Open Platform: "))) {
            normalizedBody = appendParagraph(normalizedBody, "Open Platform: " + platformUrl);
        }

        notificationService.sendUserMessageAsync(recipient.trim(), null, normalizedSubject, normalizedBody);
    }

    private boolean shouldSendScorecardEmail(String recipient) {
        if (!StringUtils.hasText(recipient)) {
            return false;
        }
        Account loggedUser = commonService.getLoggedUser();
        if (loggedUser == null || !StringUtils.hasText(loggedUser.getEmail())) {
            return true;
        }
        return !recipient.trim().equalsIgnoreCase(loggedUser.getEmail().trim());
    }

    private String normalizeScorecardEmailSubject(String subject) {
        String cleaned = subject == null ? "" : subject.trim();
        while (cleaned.endsWith(",")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        if (cleaned.isEmpty()) {
            return "Scorecard Activity";
        }
        if ("Technical failure".equalsIgnoreCase(cleaned)) {
            return "Scorecard Activity - Technical Failure";
        }
        String lower = cleaned.toLowerCase();
        if (lower.startsWith("scorecard ") && !lower.startsWith("scorecard activity")) {
            return "Scorecard Activity - " + cleaned.substring("scorecard ".length()).trim();
        }
        return cleaned;
    }

    private String normalizeScorecardEmailBody(String body) {
        String normalized = body == null ? "" : body.replace("\r\n", "\n").replace('\r', '\n').trim();
        normalized = normalized.replaceFirst("(?is)^good day[^\\n]*\\n+", "");
        normalized = normalized.replaceFirst("(?is)\\n*best regards,?\\s*\\n\\s*the\\s+zimtrade\\s+team\\s*$", "");
        normalized = normalized.replaceFirst("(?is)\\n*best regards,?\\s*\\n[^\\n]+\\s*$", "");
        normalized = normalized.replace(" login ", " log in ");
        normalized = normalized.replace("Login ", "Log in ");
        normalized = normalized.replace("response or action", "respond or take action");
        normalized = normalized.replace("You can now login", "You can now log in");
        return normalized.trim();
    }

    private String appendParagraph(String body, String line) {
        if (line == null || line.trim().isEmpty()) {
            return body;
        }
        String normalizedBody = body == null ? "" : body.trim();
        if (normalizedBody.isEmpty()) {
            return line;
        }
        return normalizedBody + "\n\n" + line;
    }

    private boolean hasHttpLink(String content) {
        return content != null && (content.contains("http://") || content.contains("https://"));
    }

    private String normalizeBaseUrl(String url) {
        if (url == null) {
            return "";
        }
        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @RequestMapping(value = "/fake-save-scorecard", method = RequestMethod.POST)
    public String fakeSaveScorecard(HttpServletRequest request, Scorecard scorecard) {
        PortletUtils.addInfoMsg("Scorecard successfully saved for later. You can update it anytime before submitting for approval", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping("/clone-scorecard/{id}")
    public ModelAndView cloneScorecard(@PathVariable("id") long id, HttpServletRequest request, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.CLONE_SCORECARD);
        modelAndView.addObject("pageTitle", "Clone Scorecard");
        Scorecard scorecard = scorecardService.getScorecardById(id);
        modelAndView.addObject("scorecard", scorecard);
        modelAndView.addObject("owner", scorecard.getOwner());
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping("/clone-scorecard-select-owner")
    public ModelAndView cloneScorecardSelect(HttpServletRequest request, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.CLONE_SCORECARD_SELECT_OWNER);
        modelAndView.addObject("pageTitle", "Clone Scorecard");
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping(value = "/clone-scorecard-save-owner", method = RequestMethod.POST)
    public String cloneScorecardSaveOwner(HttpServletRequest request, Long id) {
        Account owner = accountService.getAccountById(id);
        try {
            Scorecard scorecard = scorecardService.getActiveEmployeeScorecardByOwner(owner);
            PortletUtils.addInfoMsg("You have selected "+ owner.getFullName()+ "'s scorecard", request);
            return "redirect:/scorecards/clone-scorecard/"+ scorecard.getId();
        }catch (Exception e){
            PortletUtils.addErrorMsg(owner.getFullName()+ " Does not have an active scorecard. Please try another employee or You may go to view scorecards and select the scorecard you want from the archives", request);
            return "redirect:/scorecards/clone-scorecard-select-owner";
        }

    }


    @RequestMapping(value = "/copy-scorecard", method = RequestMethod.POST)
    public String copyScorecard(HttpServletRequest request,Scorecard imaginaryScorecard) {

        if (imaginaryScorecard == null || imaginaryScorecard.getId() <= 0) {
            PortletUtils.addErrorMsg("Validation failed: Source scorecard is required.", request);
            return "redirect:/scorecards/clone-scorecard-select-owner";
        }
        if (imaginaryScorecard.getOwner() == null || imaginaryScorecard.getOwner().getId() <= 0) {
            PortletUtils.addErrorMsg("Validation failed: New scorecard owner is required.", request);
            return "redirect:/scorecards/clone-scorecard/" + imaginaryScorecard.getId();
        }
        if (imaginaryScorecard.getReportingPeriod() == null || imaginaryScorecard.getReportingPeriod().getId() <= 0) {
            PortletUtils.addErrorMsg("Validation failed: Reporting period is required.", request);
            return "redirect:/scorecards/clone-scorecard/" + imaginaryScorecard.getId();
        }

        Account owner = accountService.getAccountById(imaginaryScorecard.getOwner().getId());
        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(imaginaryScorecard.getReportingPeriod().getId());
        if (owner == null) {
            PortletUtils.addErrorMsg("Validation failed: Selected owner could not be found.", request);
            return "redirect:/scorecards/clone-scorecard/" + imaginaryScorecard.getId();
        }
        if (reportingPeriod == null) {
            PortletUtils.addErrorMsg("Validation failed: Selected reporting period could not be found.", request);
            return "redirect:/scorecards/clone-scorecard/" + imaginaryScorecard.getId();
        }

        if (scorecardService.countActiveScorecards(owner, reportingPeriod) >= 1) {
            PortletUtils.addErrorMsg(owner.getFullName() + " already has an active scorecard for the selected reporting period (" + reportingPeriod.getStartDate() + " - " + reportingPeriod.getEndDate() + ")", request);
            return "redirect:/scorecards/clone-scorecard/"+ imaginaryScorecard.getId();
        } else {

            Scorecard scorecard = scorecardService.getScorecardById(imaginaryScorecard.getId());
            if (scorecard == null) {
                PortletUtils.addErrorMsg("Validation failed: Source scorecard could not be found.", request);
                return "redirect:/scorecards/clone-scorecard-select-owner";
            }
            Scorecard newScorecard = new Scorecard();

            if (scorecard.getClient() != null) {
                newScorecard.setClient(scorecard.getClient());
            } else {
                newScorecard.setClient(commonService.getConfiguredClient());
            }
            newScorecard.setOwner(owner);
            newScorecard.setReportingPeriod(reportingPeriod);
            newScorecard.setScorecardModel(scorecardModelService.getActiveScorecardModel());
            newScorecard.setStatus(PMConstants.STATUS_ACTIVE);
            ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
            newScorecard.setApprovalStatus(workflow.getNewStatus());
            newScorecard.setLockStatus(PMConstants.LOCK_STATUS_OPEN);

            try {
                scorecardService.saveScorecard(newScorecard);
            } catch (IllegalArgumentException | hr.performancemanagement.exception.custom.InvalidWorkflowStateException exception) {
                PortletUtils.addErrorMsg(exception.getMessage(), request);
                return "redirect:/scorecards/clone-scorecard/" + imaginaryScorecard.getId();
            }
            long id = newScorecard.getId();
            List<Goal> goalList = goalService.listAllGoals(scorecard.getId());

            for (Goal goal : goalList) {
                Goal newGoal = new Goal();

                newGoal.setScorecardId(id);
                newGoal.setPerspective(goal.getPerspective());
                newGoal.setStrategicObjective(goal.getStrategicObjective());
                newGoal.setName(goal.getName());

                Goal savedGoal = goalService.saveGoal(newGoal);

                List<Target> targetList = targetService.getAllTargetsByGoal(goal);
                for(Target target: targetList){
                    Target newTarget = new Target();
                    newTarget.setGoal(savedGoal);
                    newTarget.setPerspective(target.getPerspective());
                    newTarget.setStrategicObjective(target.getStrategicObjective());
                    newTarget.setMeasure(target.getMeasure());
                    newTarget.setUnit(target.getUnit());
                    newTarget.setAllocatedWeight(target.getAllocatedWeight());
                    newTarget.setNormalTarget(target.getNormalTarget());
                    newTarget.setBaseTarget(target.getBaseTarget());
                    newTarget.setStretchTarget(target.getStretchTarget());
                    targetService.saveTarget(newTarget);
                }


            }
            String recipient = imaginaryScorecard.getOwner().getEmail();
            String subject = "Scorecard Creation,";
            String template = "Good day, \n\n"
                    + "We are pleased to notify you that your scorecard has been cloned and is already populated with targets. "
                    + "You can now login and modify targets to match your performance goals\n\n";
            try {
                sendScorecardEmail(request, recipient, subject, template);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            return "redirect:/scorecards/view-scorecard/" + id;
        }
    }

    @RequestMapping("/view-evidence/{fileName}")
    public void viewEvidence(@PathVariable("fileName") String fileName, HttpServletResponse response) throws IOException {
        try {
            String safeFileName = sanitizeFileName(fileName);
            Path evidenceDirectory = resolveEvidenceDirectory();
            Path filePath = evidenceDirectory.resolve(safeFileName).normalize();

            if (!filePath.startsWith(evidenceDirectory)) {
                throw new IllegalArgumentException("Invalid evidence path");
            }

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return;
            }

            response.setContentType(resolveEvidenceContentType(safeFileName, filePath));
            response.setHeader("Content-Disposition", "inline;filename=\"" + safeFileName + "\"");
            response.setContentLengthLong(Files.size(filePath));

            try (InputStream inputStream = Files.newInputStream(filePath);
                 OutputStream outputStream = response.getOutputStream()) {
                FileCopyUtils.copy(inputStream, outputStream);
            }
        } catch (IllegalArgumentException exception) {
            log.warn("Rejected evidence file request for fileName={}", fileName, exception);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        } catch (IOException exception) {
            log.error("Failed to stream evidence file fileName={}", fileName, exception);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private String storeEvidenceFile(MultipartFile file) throws IOException {
        String originalName = file == null ? null : file.getOriginalFilename();
        String safeFileName = sanitizeFileName(originalName);
        String extension = commonService.getFileExtention(safeFileName);
        String generatedName = UUID.randomUUID().toString();
        if (extension != null && !extension.trim().isEmpty()) {
            generatedName = generatedName + "." + extension.toLowerCase(Locale.ENGLISH);
        }

        Path evidenceDirectory = resolveEvidenceDirectory();
        Files.createDirectories(evidenceDirectory);
        Path destination = evidenceDirectory.resolve(generatedName).normalize();
        if (!destination.startsWith(evidenceDirectory)) {
            throw new IllegalArgumentException("Invalid evidence destination path");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return generatedName;
    }

    private Path resolveEvidenceDirectory() {
        String location = environment.getProperty("spring.servlet.multipart.location");
        if (!StringUtils.hasText(location)) {
            location = System.getProperty("java.io.tmpdir");
        }
        return Paths.get(location).toAbsolutePath().normalize();
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("Attachment name is required");
        }
        String cleaned = StringUtils.cleanPath(fileName).trim();
        String simpleName;
        try {
            simpleName = Paths.get(cleaned).getFileName().toString();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid attachment name");
        }
        if (!StringUtils.hasText(simpleName)) {
            throw new IllegalArgumentException("Invalid attachment name");
        }
        simpleName = simpleName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (".".equals(simpleName) || "..".equals(simpleName)) {
            throw new IllegalArgumentException("Invalid attachment name");
        }
        return simpleName;
    }

    private String resolveEvidenceContentType(String fileName, Path filePath) throws IOException {
        String detected = Files.probeContentType(filePath);
        if (StringUtils.hasText(detected)) {
            return detected;
        }

        String ext = commonService.getFileExtention(fileName);
        if (ext == null) {
            return "application/octet-stream";
        }
        String lowerExt = ext.toLowerCase(Locale.ENGLISH);
        if ("pdf".equals(lowerExt)) {
            return "application/pdf";
        }
        if ("xls".equals(lowerExt) || "xlsx".equals(lowerExt) || "csv".equals(lowerExt)) {
            return "application/vnd.ms-excel";
        }
        if ("docx".equals(lowerExt)) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if ("doc".equals(lowerExt)) {
            return "application/msword";
        }
        if ("png".equals(lowerExt)) {
            return "image/png";
        }
        if ("jpg".equals(lowerExt) || "jpeg".equals(lowerExt)) {
            return "image/jpeg";
        }
        if ("pptx".equals(lowerExt)) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        }
        if ("ppt".equals(lowerExt)) {
            return "application/vnd.ms-powerpoint";
        }
        return "application/octet-stream";
    }

    public ModelAndView addTerminology(ModelAndView modelAndView) {
        if(modelAndView.getModel().containsKey("scorecard")){
            String stage1, stage2,stage3,stage4, model;
            Scorecard scorecard = (Scorecard) modelAndView.getModel().get("scorecard");

            // Get model from ReportingPeriod
            String hierarchyModel = "standard"; // default
            if(scorecard.getReportingPeriod() != null && scorecard.getReportingPeriod().getModel() != null) {
                hierarchyModel = scorecard.getReportingPeriod().getModel();
            }

            if("programme".equalsIgnoreCase(hierarchyModel)){
                stage1 = "Programme";
                stage2 = "Strategic Goal";
                stage3 = "Pillar";
                stage4 = "Outcome";
                model = "programme";
            } else if("gear".equalsIgnoreCase(hierarchyModel)){
                stage1 = "Gear";
                stage2 = "Goal";
                stage3 = "Goal";
                stage4 = "Outcome";
                model = "gear";
            } else {
                // Default to standard model
                stage1 = "Perspective";
                stage2 = "Goal";
                stage3 = "Goal";
                stage4 = "Goal";
                model = "standard";
            }
            modelAndView.addObject("stage1", stage1);
            modelAndView.addObject("stage2", stage2);
            modelAndView.addObject("stage3", stage3);
            modelAndView.addObject("stage4", stage4);
            modelAndView.addObject("model", model);
        }
        return modelAndView;
    }

}
