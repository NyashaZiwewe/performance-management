package hr.performancemanagement.controllers.scorecard;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.service.GearService;
import hr.performancemanagement.service.OutcomeService;
import hr.performancemanagement.service.OutputService;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.service.api.ScoreService.StandardScorecardScoreService;
import hr.performancemanagement.service.api.ScoreService.ValueBasedScoreService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.PMConstants;
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

    @Autowired
    private final ReportingPeriodService reportingPeriodService;
    @Autowired
    private final AccountService accountService;
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
    private final ScorecardWorkflowService scorecardWorkflowService;

    private final Environment environment;


    public ScorecardController(ReportingPeriodService reportingPeriodService, AccountService accountService, ScorecardService scorecardService, PerspectiveService perspectiveService, GoalService goalService, TargetService targetService, GearService gearService, OutcomeService outcomeService, OutputService outputService, StrategicObjectiveService strategicObjectiveService, CommentService commentService, NotificationService notificationService, ApprovalService approvalService, ReportingDateService reportingDateService, StandardScorecardScoreService standardScorecardScoreService, ValueBasedScoreService valueBasedScoreService, ScorecardModelService scorecardModelService, CommonService commonService, ScorecardWorkflowService scorecardWorkflowService, Environment environment) {
        this.reportingPeriodService = reportingPeriodService;
        this.accountService = accountService;
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
        this.scorecardWorkflowService = scorecardWorkflowService;
        this.environment = environment;
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request, HttpSession session) {

        List<ReportingPeriod> REPORTING_PERIODS_LIST = reportingPeriodService.listAllReportingPeriods();
        List<Account> ACCOUNTS_LIST = accountService.listAllAccounts();
        List<Perspective> PERSPECTIVES_LIST = perspectiveService.listAllPerspectives(commonService.getLoggedUser().getClientId());
        Account loggedUser = commonService.getLoggedUser();
        long loggedUserId = loggedUser.getId();
        String role = loggedUser.getRole();
        ReportingDate activeReportingDate = reportingDateService.getActiveReportingDate();
        boolean captureWindowOpen = reportingDateService.isReportingDateOpen(activeReportingDate);
        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();

        modelAndView.addObject("pageDomain", "Performance");
        modelAndView.addObject("pageName", "Scorecards");
        modelAndView.addObject("reportingPeriodsList", REPORTING_PERIODS_LIST);
        modelAndView.addObject("accountsList", ACCOUNTS_LIST);
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
        addTerminology(modelAndView);
        PortletUtils.addMessagesToPage(modelAndView, request);

    }

    @RequestMapping
    public ModelAndView viewScorecards(@RequestParam(value = "reportingPeriodId", required = false) Long reportingPeriodId,
                                       @RequestParam(value = "ownerName", required = false) String ownerName,
                                       @RequestParam(value = "approvalStatus", required = false) String approvalStatus,
                                       HttpServletRequest request,
                                       HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_SCORECARDS);
        modelAndView.addObject("pageTitle", "View Scorecards");
        ReportingPeriod reportingPeriod = reportingPeriodService.getActiveReportingPeriod();
        if (reportingPeriodId != null) {
            ReportingPeriod selectedPeriod = reportingPeriodService.getReportingPeriodById(reportingPeriodId);
            Account loggedUser = commonService.getLoggedUser();
            if (selectedPeriod != null && loggedUser != null && selectedPeriod.getClientId() == loggedUser.getClientId()) {
                reportingPeriod = selectedPeriod;
            }
        }
        List<Scorecard> scorecards = reportingPeriod == null
                ? Collections.emptyList()
                : scorecardService.getScorecardsByReportingPeriodId(reportingPeriod);
        scorecards = filterScorecards(scorecards, ownerName, approvalStatus);

        modelAndView.addObject("scorecards", scorecards);
        modelAndView.addObject("selectedReportingPeriodId", reportingPeriod != null ? reportingPeriod.getId() : null);
        modelAndView.addObject("ownerName", ownerName == null ? "" : ownerName.trim());
        modelAndView.addObject("approvalStatus", approvalStatus == null ? "" : approvalStatus.trim());
        preparePage(modelAndView, request, session);
        return modelAndView;
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

    @RequestMapping(value = "/view-user-scorecards/{id}")
    public ModelAndView viewUserScorecards(@PathVariable("id") Long userId, HttpServletRequest request, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_USER_SCORECARDS);
        Account owner = accountService.getAccountById(userId);
        modelAndView.addObject("pageTitle", "View " + owner.getFullName() + "'s Scorecards");
        List<Scorecard> scorecards = scorecardService.getScorecardsByOwner(owner);

        modelAndView.addObject("scorecards", scorecards);
        preparePage(modelAndView, request, session);
        return modelAndView;
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

        if(scorecardService.countActiveScorecards(newScorecard.getOwner(), newScorecard.getReportingPeriod()) >= 1){
            PortletUtils.addErrorMsg(newScorecard.getOwner().getFullName() + " already has an active scorecard for the selected reporting period (" + newScorecard.getReportingPeriod().getStartDate() +" - "+ newScorecard.getReportingPeriod().getEndDate() +")", request);
            return "redirect:/scorecards/add-scorecard";
        }else {
            ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
            newScorecard.setClientId(loggedUser.getClientId());
            newScorecard.setLockStatus(PMConstants.LOCK_STATUS_OPEN);
            newScorecard.setStatus(PMConstants.STATUS_ACTIVE);
            newScorecard.setApprovalStatus(workflow.getNewStatus());
            scorecardService.addScorecard(newScorecard);

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
        String scorecardModel = scorecard.getScorecardModel().getName();
        String hierarchyModel = resolveHierarchyModel(scorecard);
        long reportingPeriodId = scorecard.getReportingPeriod().getId();
        List<Goal> GOALS_LIST = goalService.listAllGoals(id);
        List<StrategicObjective> STRATEGIC_OBJECTIVES_LIST = strategicObjectiveService.listAllStrategicObjectives(reportingPeriodId);
        ModelAndView modelAndView;

        if(commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_TARGETS,scorecard)){

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
                List<Gear> gears = gearService.listAllGears(commonService.getLoggedUser().getClientId());
                List<Gear> selectedGears = gearService.listSelectedGears(scorecard);
                List<Gear> remainingGears = gearService.listRemainingGears(scorecard);
                targetsList = attachLegacyTargets(scorecard, selectedGears);
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
            PortletUtils.addErrorMsg("You are not allowed to capture targets on this scorecard at this moment", request);
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
                if (target.getGoal() == null && goal != null) {
                    target.setGoal(goal);
                }
                if (target.getAllocatedWeight() == null && output.getAllocatedWeight() != null) {
                    target.setAllocatedWeight(output.getAllocatedWeight());
                }
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
                gear.getTargetsList().sort(Comparator.<Target>comparingLong(target -> target.getOutcome() == null ? Long.MAX_VALUE : target.getOutcome().getId())
                        .thenComparingLong(target -> target.getOutput() == null ? Long.MAX_VALUE : target.getOutput().getId())
                        .thenComparingLong(Target::getId));
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
        String hierarchyModel = resolveHierarchyModel(scorecard);
        List<ScorecardDisplaySection> displaySections = buildDisplaySections(scorecard, hierarchyModel, targetsList);
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
        modelAndView.addObject("captureSubmitLabel", resolveCaptureSubmitLabel(captureStage));

            modelAndView.addObject("pageTitle", "Capture Scores");
            modelAndView.addObject("scorecard", scorecard);
            List<Target> targetsList;
            if (isLegacyHierarchyModel(hierarchyModel)) {
                List<Gear> selectedGears = gearService.listSelectedGears(scorecard);
                targetsList = attachLegacyTargets(scorecard, selectedGears);
                modelAndView.addObject("selectedGears", selectedGears);
            } else {
                targetsList = targetService.getAllTargetsByScorecard(id);
            }
            modelAndView.addObject("targetsList", targetsList);
            modelAndView.addObject("targetRows", buildTargetCaptureRows(targetsList));
            addScorecardDisplayModel(modelAndView, scorecard, targetsList);
            modelAndView.addObject("averageEmployeeScore", averageEmployeeScore);
            modelAndView.addObject("averageManagerScore", averageManagerScore);
            modelAndView.addObject("averageAgreedScore", averageAgreedScore);
            modelAndView.addObject("averageModeratedScore", averageModeratedScore);
            modelAndView.addObject("weightedScore", weightedScore);
            modelAndView.addObject("totalAllocatedWeight", totalAllocatedWeight);
            modelAndView.addObject("totalCaptureWeightedScore", sumCurrentWeightedScores(targetsList));
            modelAndView.addObject("reportingDates", reportingDates);
            modelAndView.addObject("reportingDate", reportingDate);
            modelAndView.addObject("reportingDateLabel", resolveReportingDateLabel(reportingDate));
            modelAndView.addObject("scorecardModel", scorecardModel);
            modelAndView.addObject("reportingPeriod", reportingPeriod);
            modelAndView.addObject("scoreCaptureAllowed", scoreCaptureAllowed);
            modelAndView.addObject("scoreCaptureBlockedMessage", scoreCaptureBlockedMessage);
            if (PMConstants.STANDARD_SCORECARD.equalsIgnoreCase(scorecardModel)) {
                modelAndView.addObject("captureViewSummaryLabelColspan", displayHierarchyColumnCount + 4);
                modelAndView.addObject("captureEntrySummaryLabelColspan", displayHierarchyColumnCount + 4);
                modelAndView.addObject("captureViewTableColumnCount", displayHierarchyColumnCount + 8);
                modelAndView.addObject("captureEntryTableColumnCount", displayHierarchyColumnCount + 9);
            } else if (PMConstants.VALUE_BASED.equalsIgnoreCase(scorecardModel) && captureStage != null) {
                modelAndView.addObject("captureViewSummaryLabelColspan", displayHierarchyColumnCount + 3);
                modelAndView.addObject("captureEntrySummaryLabelColspan", displayHierarchyColumnCount + 3);
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

    private String resolveCaptureSubmitLabel(ValueBasedCaptureStage captureStage) {
        if (captureStage == null) {
            return "Validate & Submit";
        }
        switch (captureStage) {
            case EMPLOYEE:
                return "Submit Owner Scores";
            case MANAGER:
                return "Submit Supervisor Scores";
            case AGREED:
                return "Submit Agreed Scores";
            case MODERATED:
                return "Submit Moderated Scores";
            default:
                return "Validate & Submit";
        }
    }

    private double sumCurrentWeightedScores(List<Target> targets) {
        if (targets == null || targets.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (Target target : targets) {
            if (target != null && target.getCurrentWeightedScore() != null) {
                total += target.getCurrentWeightedScore();
            }
        }
        return total;
    }

    private int resolveValueBasedViewTableColumnCount(int hierarchyColumnCount, ValueBasedCaptureStage captureStage) {
        int columns = hierarchyColumnCount + 6; // measure, unit, target, weight, employee score, action
        if (captureStage != ValueBasedCaptureStage.EMPLOYEE) {
            columns += 1; // manager score
        }
        if (captureStage == ValueBasedCaptureStage.AGREED || captureStage == ValueBasedCaptureStage.MODERATED) {
            columns += 1; // agreed score
        }
        if (captureStage == ValueBasedCaptureStage.MODERATED) {
            columns += 2; // moderated score + weighted score
        }
        return columns;
    }

    private int resolveValueBasedCaptureTableColumnCount(int hierarchyColumnCount, ValueBasedCaptureStage captureStage) {
        int columns = hierarchyColumnCount + 8; // measure, unit, target, weight, employee, evidence, justification, action
        if (captureStage != ValueBasedCaptureStage.EMPLOYEE) {
            columns += 2; // manager + weighted
        } else {
            columns += 1; // attachment
        }
        if (captureStage == ValueBasedCaptureStage.AGREED || captureStage == ValueBasedCaptureStage.MODERATED) {
            columns += 1; // agreed
        }
        if (captureStage == ValueBasedCaptureStage.MODERATED) {
            columns += 1; // moderated
        }
        return columns;
    }

    private boolean equalsStatus(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    @RequestMapping(value = "/save-target", method = RequestMethod.POST)
    public String saveTarget(@Valid GoalWrapper goalWrapper, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            log.warn("Target validation failed: {}", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/scorecards/view-scorecards";
        }

        long scorecardId = goalWrapper.getScorecardId();
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
    public String saveTargetToExistingGoal(@Valid Target target, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            log.warn("Target validation failed: {}", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/scorecards/view-scorecards";
        }

        long scorecardId = target.getGoal().getScorecardId();
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

        Goal goal = resolveGoalFromOutcome(output.getOutcome());
        if (goal == null) {
            PortletUtils.addErrorMsg("Strategic goal could not be resolved for this output.", request);
            return "redirect:/scorecards/capture-targets/" + output.getScorecard().getId();
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

        return "redirect:/scorecards/capture-targets/" + output.getScorecard().getId();
    }

    @RequestMapping(value = "/select-gear", method = RequestMethod.POST)
    public String selectGear(HttpServletRequest request, long gearId, long scorecardId) {
        Gear gear = gearService.getGearById(gearId);
        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        if (gear == null || scorecard == null) {
            PortletUtils.addErrorMsg("Scorecard hierarchy selection failed. Invalid input.", request);
            return "redirect:/scorecards/capture-targets/" + scorecardId;
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

        if(PMConstants.USER_TYPE_OWNER.equalsIgnoreCase(userType)){
            scorecard.setOwnerComment(comment);
        } else if (PMConstants.USER_TYPE_SUPERVISOR.equalsIgnoreCase(userType)) {
            scorecard.setSupervisorComment(comment);
        } else if (PMConstants.USER_TYPE_MODERATOR.equalsIgnoreCase(userType)) {
            scorecard.setModeratorComment(comment);
        }
        scorecardService.saveScorecard(scorecard);

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

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("alreadyExists", false);

        String jsonString = jsonObject.toString();

        try(OutputStream outputStream = response.getOutputStream()){
            response.setContentType("application/json; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));

        }catch (IOException exception){
            log.error("Failed to write overall comment response for scorecardId={}", scorecardId, exception);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
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

        if (scorecardId <= 0) {
            return "redirect:/scorecards";
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

        PortletUtils.addInfoMsg("Scorecard successfully submitted for approval. An email was sent to your supervisor", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping(value = "/submit-employee-scores", method = RequestMethod.POST)
    public String submitEmployeeScore(HttpServletRequest request, Scorecard updatedScorecard) throws MalformedURLException {

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());

        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        scorecard.setApprovalStatus(workflow.getScoredByEmployeeStatus());
        scorecardService.saveScorecard(scorecard);
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

        PortletUtils.addInfoMsg("Owner scores submitted successfully for supervisor approval. An email was sent to " + supervisor.getFullName(), request);
        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
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

        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        scorecard.setApprovalStatus(workflow.getApprovedOwnerScoresStatus());
        scorecardService.saveScorecard(scorecard);

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

    @RequestMapping(value = "/submit-manager-scores", method = RequestMethod.POST)
    public String submitManagerScore(HttpServletRequest request, Scorecard updatedScorecard) throws MalformedURLException {

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());

        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        scorecard.setApprovalStatus(workflow.getScoredBySupervisorStatus());
        scorecardService.saveScorecard(scorecard);
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

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());
        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        scorecard.setApprovalStatus(workflow.getAgreedByTwoStatus());
        Account loggedUser = commonService.getLoggedUser();
        Account supervisor = scorecard.getOwner().getSupervisor();
        Account owner = scorecard.getOwner();

        try {
            scorecardService.saveScorecard(scorecard);
            URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
            String recipient = owner.getEmail();
            String subject = "Scorecard Scoring,";
            String template = "Good day, \n\n"
                    + "Please note that " + supervisor.getFullName() + " has submitted the agreed scores for your scorecard. "
                    + "A moderator will approve before moderation capture proceeds.\n"
                    + "You can now log in and view results.\n"
                    + "Link: "+ currentURL +"\n\n";

            String recipient2 = commonService.getHREmail();

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
                sendScorecardEmail(request, recipient2, subject2, template2);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient2 + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }

            PortletUtils.addInfoMsg("Agreed scores submitted successfully. An email was sent to " + owner.getFullName() + " and HR for moderation approval.", request);

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
            PortletUtils.addErrorMsg("Scorecard wasn't submitted. An Email was sent to the administrator", request);
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

        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        scorecard.setApprovalStatus(workflow.getApprovedAgreedScoresStatus());
        scorecardService.saveScorecard(scorecard);

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
        ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
        scorecard.setApprovalStatus(workflow.getModeratedByHrStatus());
        Account loggedUser = commonService.getLoggedUser();
        Account supervisor = scorecard.getOwner().getSupervisor();
        Account owner = scorecard.getOwner();

        try {
            scorecardService.saveScorecard(scorecard);

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
            PortletUtils.addErrorMsg("Scorecard wasn't submitted. An Email was sent to the administrator", request);
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


            String recipient2 = commonService.getHREmail();
            String subject2 = "Scorecard Approval,";
            String template2 = "Good day HR, \n\n"
                            + "Please note that "+ supervisor +" has approved "+owner+"'s scorecard. "
                            + "You are now eligible to review and approve so that they can proceed with capturing scores. \n"
                            + "Link: "+ currentURL +"\n\n";
            try {
                sendScorecardEmail(request, recipient2, subject2, template2);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }


            PortletUtils.addInfoMsg("Scorecard successfully approved. An email was sent to HR for further approval and to "+ owner + " as feedback", request);
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
            PortletUtils.addInfoMsg("Scorecard approval failed. An email was sent to the administrator with error details. ", request);
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

            PortletUtils.addInfoMsg("Scorecard successfully rejected. An email was sent to "+ owner + " as feedback", request);

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
            PortletUtils.addInfoMsg("Scorecard approval failed. An email was sent to the administrator with error details. ", request);
        }

        return "redirect:/scorecards/view-scorecard/"+ id;
    }

    @RequestMapping(value = "/hr-approve", method = RequestMethod.POST)
    public String hrApproveScorecard(HttpServletRequest request, Long id) throws UnsupportedEncodingException {

        Scorecard scorecard = scorecardService.getScorecardById(id);
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


            PortletUtils.addInfoMsg("Scorecard successfully approved. An email was sent to "+supervisor+" and to "+ owner + " as feedback", request);
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
            PortletUtils.addInfoMsg("Scorecard approval failed. An email was sent to the administrator with error details. ", request);
            return "redirect:/scorecards/view-scorecard/"+ id;
        }
    }

    @RequestMapping(value = "/hr-reject", method = RequestMethod.POST)
    public String hrRejectScorecard(HttpServletRequest request, Long id, String message) throws UnsupportedEncodingException {

        Scorecard scorecard = scorecardService.getScorecardById(id);
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

            PortletUtils.addInfoMsg("Scorecard successfully rejected. An email was sent to "+ supervisor + " as feedback", request);

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
            PortletUtils.addInfoMsg("Scorecard approval failed. An email was sent to the administrator with error details. ", request);
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

            String scorecardModel = scorecard.getScorecardModel() != null
                    ? scorecard.getScorecardModel().getName()
                    : PMConstants.STANDARD_SCORECARD;
            String hierarchyModel = resolveHierarchyModel(scorecard);
            int displayHierarchyColumnCount = resolveDisplayHierarchyColumnCount(hierarchyModel);
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
            modelAndView.addObject("scorecardModel", scorecardModel);
            modelAndView.addObject("targetsList", targetsList);
            modelAndView.addObject("targetRows", buildTargetCaptureRows(targetsList));
            addScorecardDisplayModel(modelAndView, scorecard, targetsList);
            modelAndView.addObject("comment", new Comment());
            modelAndView.addObject("averageEmployeeScore", averageEmployeeScore);
            modelAndView.addObject("averageManagerScore", averageManagerScore);
            modelAndView.addObject("averageAgreedScore", averageAgreedScore);
            modelAndView.addObject("averageModeratedScore", averageModeratedScore);
            modelAndView.addObject("totalAllocatedWeight", totalAllocatedWeight);
            modelAndView.addObject("totalWeightedScore", totalWeightedScore);
            if (PMConstants.VALUE_BASED.equalsIgnoreCase(scorecardModel)) {
                modelAndView.addObject("viewSummaryLabelColspan", displayHierarchyColumnCount + 3);
                modelAndView.addObject("viewTableColumnCount", displayHierarchyColumnCount + 10);
            } else {
                modelAndView.addObject("viewSummaryLabelColspan", displayHierarchyColumnCount + 4);
                modelAndView.addObject("viewTableColumnCount", displayHierarchyColumnCount + 8);
            }
            modelAndView.addObject("isSupervisor", commonService.isSupervisor(scorecard.getOwner()));
            ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
            modelAndView.addObject("ownerCaptureStage", equalsStatus(scorecard.getApprovalStatus(), workflow.getApprovedByHrStatus()));
            modelAndView.addObject("supervisorCaptureStage",
                    equalsStatus(scorecard.getApprovalStatus(), workflow.getApprovedOwnerScoresStatus())
                            || equalsStatus(scorecard.getApprovalStatus(), workflow.getScoredBySupervisorStatus()));
            modelAndView.addObject("agreedCaptureStage", equalsStatus(scorecard.getApprovalStatus(), workflow.getScoredBySupervisorStatus()));
            modelAndView.addObject("moderatedCaptureStage", equalsStatus(scorecard.getApprovalStatus(), workflow.getApprovedAgreedScoresStatus()));
            modelAndView.addObject("canApprove", commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_SCORECARD, scorecard));
            modelAndView.addObject("canCaptureTargets", commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_TARGETS, scorecard));
            modelAndView.addObject("canCaptureEmployeeScore", commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES, scorecard));
            modelAndView.addObject("canCaptureManagerScore", commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES, scorecard));
            modelAndView.addObject("canCaptureAgreedScoreAgreedScore", commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES, scorecard));
            modelAndView.addObject("canModerate", commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES, scorecard));
            modelAndView.addObject("canApproveOwnerScores", commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_OWNER_SCORES, scorecard));
            modelAndView.addObject("canApproveAgreedScores", commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_AGREED_SCORES, scorecard));

        }catch (Exception e){
            PortletUtils.addErrorMsg("The scorecard could not be opened. Please check scorecard setup and try again.", request);
            modelAndView = new ModelAndView(Pages.BLANK_PAGE);
        }

        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping(value = "/save-comment", method = RequestMethod.POST)
    public String saveComment(HttpServletRequest request, Comment comment) throws MalformedURLException {
        Account loggedUser = commonService.getLoggedUser();
        comment.setSender(loggedUser);
        commentService.saveComment(comment);
        Long scorecardId = comment.getTarget().getGoal().getScorecardId();
        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        URL link = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecardId));

        String recipient = scorecard.getOwner().getEmail();
        String subject = "Scorecard Comment,";
        String template = "Good day, \n\n"
                + "Please note that"+ loggedUser.getFullName() +" added a comment on your scorecard. "
                + "Goal - measure: [" + comment.getTarget().getGoal().getName() +" - "+ comment.getTarget().getMeasure() +"]\n"
                + "Message: "+ comment.getName() + "\n"
                + "You can now login and response or action\n"
                + "Link: "+ link + "\n\n";
        try {
            sendScorecardEmail(request, recipient, subject, template);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }
        PortletUtils.addInfoMsg("Comment successfully saved", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecardId;
    }

    @RequestMapping(value = "/save-flag", method = RequestMethod.POST)
    public String saveFlag(HttpServletRequest request, Target updatedTarget) throws MalformedURLException {

        Target target = targetService.getTargetById(updatedTarget.getId());
        long scorecardId = target.getGoal().getScorecardId();
        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        target.setFlag(updatedTarget.getFlag());
        targetService.saveTarget(target);
        URL link = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecardId));
        String fullName = commonService.getLoggedUser().getFullName();

        String recipient = scorecard.getOwner().getEmail();
        String subject = "Scorecard Comment,";
        String template = "Good day, \n\n"
                + "Please note that"+ fullName +" flagged a goal on your scorecard. "
                + "Goal - measure: [" + target.getGoal().getName() +" - "+ target.getMeasure() +"]\n"
                + "Message: "+ target.getFlag() + "\n"
                + "You can now login and response or action\n"
                + "Link: "+ link + "\n\n";
        try {
            sendScorecardEmail(request, recipient, subject, template);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }
        PortletUtils.addInfoMsg("Measure successfully flagged and the reason was saved", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecardId;
    }

    @RequestMapping(value = "/save-standard-score", method = RequestMethod.POST, consumes = {"*/*"})
    public void saveStandardScore( HttpServletResponse response, Long targetId, Double actual, String evidence, String justification) {
        if (targetId == null) {
            writeScoreSaveResponse(response, true, "Target reference is required");
            return;
        }
        Target target = targetService.getTargetById(targetId);
        if (target == null || target.getGoal() == null) {
            writeScoreSaveResponse(response, true, "Target could not be resolved");
            return;
        }
        Scorecard scorecard = scorecardService.getScorecardById(target.getGoal().getScorecardId());
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

        Score score = new Score();
        score.setTarget(target);
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
            if (target == null || target.getGoal() == null) {
                PortletUtils.addErrorMsg("Target could not be resolved for evidence upload.", request);
                return "redirect:/scorecards";
            }

            Scorecard scorecard = scorecardService.getScorecardById(target.getGoal().getScorecardId());
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

            Score score = new Score();
            score.setTarget(target);
            score.setReportingDate(reportingDate);
            score.setEvidence(wrapper.getEvidence());

            MultipartFile file = wrapper.getAttachment();
            if (file != null && !file.isEmpty()) {
                String storedFileName = storeEvidenceFile(file);
                score.setAttachmentName(storedFileName);
            }

            valueBasedScoreService.saveEvidence(score);
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
            if (target == null || target.getGoal() == null) {
                writeScoreSaveResponse(response, true, "Target could not be resolved");
                return;
            }
            Scorecard scorecard = scorecardService.getScorecardById(target.getGoal().getScorecardId());
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

            if(isStageCaptureAllowed(scorecard, stage)){
                Score score = new Score();
                score.setTarget(target);
                score.setReportingDate(reportingDate);
                score.setJustification(justification);
                persistScoreByStage(score, scoreValue, stage);
                writeScoreSaveResponse(response);
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
        return scorecard != null && (
                commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES, scorecard)
                        || commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES, scorecard)
                        || commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES, scorecard)
                        || commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES, scorecard)
        );
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

    private String resolveReportingDateLabel(ReportingDate reportingDate) {
        if (reportingDate != null && StringUtils.hasText(reportingDate.getEndDate())) {
            return reportingDate.getEndDate();
        }
        return "N/A";
    }

    private boolean isStageCaptureAllowed(Scorecard scorecard, ValueBasedCaptureStage stage) {
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

    private void writeScoreSaveResponse(HttpServletResponse response) {
        writeScoreSaveResponse(response, false, null);
    }

    private void writeScoreSaveResponse(HttpServletResponse response, boolean captureBlocked, String message) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("alreadyExists", false);
        jsonObject.put("captureBlocked", captureBlocked);
        if (message != null && !message.trim().isEmpty()) {
            jsonObject.put("message", message);
        }
        String jsonString = jsonObject.toString();

        try(OutputStream outputStream = response.getOutputStream()){
            response.setContentType("application/json; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        }catch (IOException exception){
            log.error("Failed to write score save response", exception);
        }
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

        if (scorecardService.countActiveScorecards(imaginaryScorecard.getOwner(), imaginaryScorecard.getReportingPeriod()) >= 1) {
            PortletUtils.addErrorMsg(imaginaryScorecard.getOwner().getFullName() + " already has an active scorecard for the selected reporting period (" + imaginaryScorecard.getReportingPeriod().getStartDate() + " - " + imaginaryScorecard.getReportingPeriod().getEndDate() + ")", request);
            return "redirect:/scorecards/clone-scorecard/"+ imaginaryScorecard.getId();
        } else {

            Scorecard scorecard = scorecardService.getScorecardById(imaginaryScorecard.getId());
            Scorecard newScorecard = new Scorecard();

            newScorecard.setClientId(scorecard.getClientId());
            newScorecard.setOwner(imaginaryScorecard.getOwner());
            newScorecard.setReportingPeriod(imaginaryScorecard.getReportingPeriod());
            newScorecard.setScorecardModel(scorecardModelService.getActiveScorecardModel());
            newScorecard.setStatus(PMConstants.STATUS_ACTIVE);
            ScorecardWorkflowDefinition workflow = scorecardWorkflowService.getWorkflowDefinition();
            newScorecard.setApprovalStatus(workflow.getNewStatus());
            newScorecard.setLockStatus(PMConstants.LOCK_STATUS_OPEN);

            scorecardService.saveScorecard(newScorecard);
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
                stage2 = "Outcome";
                stage3 = "Pillar";
                stage4 = "Strategic goal";
                model = "programme";
            } else if("gear".equalsIgnoreCase(hierarchyModel)){
                stage1 = "Gear";
                stage2 = "Goal";
                stage3 = "goal";
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
