package hr.performancemanagement.controllers.scorecard;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.service.api.*;
import hr.performancemanagement.service.api.ScoreService.StandardScorecardScoreService;
import hr.performancemanagement.service.api.ScoreService.ValueBasedScoreService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.constants.Pages;
import hr.performancemanagement.utils.wrappers.EvidenceWrapper;
import hr.performancemanagement.utils.wrappers.GoalWrapper;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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

    private final Environment environment;


    public ScorecardController(ReportingPeriodService reportingPeriodService, AccountService accountService, ScorecardService scorecardService, PerspectiveService perspectiveService, GoalService goalService, TargetService targetService, StrategicObjectiveService strategicObjectiveService, CommentService commentService, NotificationService notificationService, ApprovalService approvalService, ReportingDateService reportingDateService, StandardScorecardScoreService standardScorecardScoreService, ValueBasedScoreService valueBasedScoreService, ScorecardModelService scorecardModelService, CommonService commonService, Environment environment) {
        this.reportingPeriodService = reportingPeriodService;
        this.accountService = accountService;
        this.scorecardService = scorecardService;
        this.perspectiveService = perspectiveService;
        this.goalService = goalService;
        this.targetService = targetService;
        this.strategicObjectiveService = strategicObjectiveService;
        this.commentService = commentService;
        this.notificationService = notificationService;
        this.approvalService = approvalService;
        this.reportingDateService = reportingDateService;
        this.standardScorecardScoreService = standardScorecardScoreService;
        this.valueBasedScoreService = valueBasedScoreService;
        this.scorecardModelService = scorecardModelService;
        this.commonService = commonService;
        this.environment = environment;
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request, HttpSession session) {

        List<ReportingPeriod> REPORTING_PERIODS_LIST = reportingPeriodService.listAllReportingPeriods();
        List<Account> ACCOUNTS_LIST = accountService.listAllAccounts();
        List<Perspective> PERSPECTIVES_LIST = perspectiveService.listAllPerspectives(commonService.getLoggedUser().getClientId());
        Account loggedUser = commonService.getLoggedUser();
        long loggedUserId = loggedUser.getId();
        String role = loggedUser.getRole();

        modelAndView.addObject("pageDomain", "Performance");
        modelAndView.addObject("pageName", "Scorecards");
        modelAndView.addObject("reportingPeriodsList", REPORTING_PERIODS_LIST);
        modelAndView.addObject("accountsList", ACCOUNTS_LIST);
        modelAndView.addObject("perspectivesList", PERSPECTIVES_LIST);
        modelAndView.addObject("loggedUserId", loggedUserId);
        modelAndView.addObject("loggedUser", loggedUser);
        modelAndView.addObject("role", role);
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
    public String saveScorecard(HttpServletRequest request, @Valid Scorecard newScorecard, BindingResult bindingResult) throws UnsupportedEncodingException {
        if (bindingResult.hasErrors()) {
            PortletUtils.addErrorMsg("Validation failed: " + bindingResult.getFieldError().getDefaultMessage(), request);
            return "redirect:/scorecards/add-scorecard";
        }

         Account loggedUser = commonService.getLoggedUser();

        if(scorecardService.countActiveScorecards(newScorecard.getOwner(), newScorecard.getReportingPeriod()) >= 1){
            PortletUtils.addErrorMsg(newScorecard.getOwner().getFullName() + " already has an active scorecard for the selected reporting period (" + newScorecard.getReportingPeriod().getStartDate() +" - "+ newScorecard.getReportingPeriod().getEndDate() +")", request);
            return "redirect:/scorecards/add-scorecard";
        }else {
            newScorecard.setClientId(commonService.getLoggedUser().getClientId());
            newScorecard.setLockStatus("OPEN");
            newScorecard.setStatus("ACTIVE");
            newScorecard.setApprovalStatus("NEW");
            scorecardService.addScorecard(newScorecard);

            String recipient = newScorecard.getOwner().getEmail();
            String subject = "Scorecard Approval,";
            String template = "Good day, \n\n"
                    + "Please note that your scorecard has been successfully created. "
                    + "You can now login and approve\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
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
        long reportingPeriodId = scorecard.getReportingPeriod().getId();
        List<Goal> GOALS_LIST = goalService.listAllGoals(id);
        List<StrategicObjective> STRATEGIC_OBJECTIVES_LIST = strategicObjectiveService.listAllStrategicObjectives(reportingPeriodId);
        ModelAndView modelAndView;

        if(commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_TARGETS,scorecard)){

            modelAndView = new ModelAndView(Pages.CAPTURE_TARGETS);
            modelAndView.addObject("pageTitle", "Capture Targets {"+ scorecard.getOwner().getFullName() +"}");
            modelAndView.addObject("scorecard", scorecard);
            modelAndView.addObject("goalsList", GOALS_LIST);
            modelAndView.addObject("strategicObjectivesList", STRATEGIC_OBJECTIVES_LIST);
            modelAndView.addObject("totalAllocatedWeight", goalService.getTotalAllocatedWeight(id));
            modelAndView.addObject("scorecardModel", scorecardModel);
            List<Target> targetsList = targetService.getAllTargetsByScorecard(id);
            modelAndView.addObject("targetsList", targetsList);
            modelAndView.addObject("targetRows", buildTargetCaptureRows(targetsList));

        } else {

            modelAndView = new ModelAndView(Pages.BLANK_PAGE);
            PortletUtils.addErrorMsg("You are not allowed to capture targets on this scorecard at this moment", request);
        }
        preparePage(modelAndView, request, session);
        return modelAndView;
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
        applyStrategicObjectiveRowspans(targetsList, rows);
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

    @RequestMapping("/capture-scores/{id}")
    public ModelAndView captureScores(@PathVariable("id") long id, HttpServletRequest request, HttpSession session) {

        Scorecard scorecard = scorecardService.getScorecardById(id);
        String scorecardModel = scorecard.getScorecardModel().getName();
        ReportingPeriod reportingPeriod = scorecard.getReportingPeriod();
        ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
        List<ReportingDate> reportingDates = reportingDateService.listAllReportingDates(reportingPeriod);
        double averageEmployeeScore = goalService.getAverageEmployeeScore(id);
        double averageManagerScore = goalService.getAverageManagerScore(id);
        double averageAgreedScore = goalService.getAverageAgreedScore(id);
        double averageModeratedScore = goalService.getAverageModeratorScore(id);
        double totalAllocatedWeight = goalService.getTotalAllocatedWeight(id);

        double weightedScore;
        try {
            weightedScore = (averageModeratedScore / 5 ) * 100;
        }catch (Exception e){
            weightedScore = 0;
        }

        ModelAndView modelAndView = resolveCaptureScoresView(scorecard, scorecardModel, request);

            modelAndView.addObject("pageTitle", "Capture Scores");
            modelAndView.addObject("scorecard", scorecard);
            List<Target> targetsList = targetService.getAllTargetsByScorecard(id);
            modelAndView.addObject("targetsList", targetsList);
            modelAndView.addObject("targetRows", buildTargetCaptureRows(targetsList));
            modelAndView.addObject("averageEmployeeScore", averageEmployeeScore);
            modelAndView.addObject("averageManagerScore", averageManagerScore);
            modelAndView.addObject("averageAgreedScore", averageAgreedScore);
            modelAndView.addObject("averageModeratedScore", averageModeratedScore);
            modelAndView.addObject("weightedScore", weightedScore);
            modelAndView.addObject("totalAllocatedWeight", totalAllocatedWeight);
            modelAndView.addObject("reportingDates", reportingDates);
            modelAndView.addObject("reportingDate", reportingDate);
            modelAndView.addObject("scorecardModel", scorecardModel);
            modelAndView.addObject("reportingPeriod", reportingPeriod);

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
                                + "Link: "+ link + "\n\n"
                                + "Best regards,\n"
                                + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
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
        Goal goal = goalService.getGoalById(target.getGoal().getId());
        boolean hasTargets = targetService.checkIfGoalHasTargets(goal);
        long scorecardId = goal.getScorecardId();

        try {
            targetService.deleteTarget(target);
            PortletUtils.addInfoMsg("Target was successfully deleted", request);
            if(!hasTargets){
                goalService.deleteGoal(goal);
                PortletUtils.addInfoMsg("Goal was successfully deleted", request);
              }
        }catch (Exception e){
            PortletUtils.addErrorMsg("Target wasn't deleted", request);
        }

        return "redirect:/scorecards/capture-targets/"+ scorecardId;
    }

    @RequestMapping(value = "/submit-scorecard-for-approval", method = RequestMethod.POST)
    public String submitScorecardForApproval(HttpServletRequest request, Scorecard updatedScorecard) throws UnsupportedEncodingException, MalformedURLException {

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());

        scorecard.setApprovalStatus(PMConstants.APPROVAL_STATUS_PENDING_APPROVAL);
        scorecard.setLockStatus(PMConstants.LOCK_STATUS_LOCKED);
        scorecardService.saveScorecard(scorecard);
        URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
        String recipient = scorecard.getOwner().getSupervisor().getEmail();

        String subject = "Scorecard Approval,";
        String template = "Good day, \n\n"
                + "Please note that "+ scorecard.getOwner().getFullName() +" has submitted his/her scorecard for your approval. "
                + "You can now login and approve\n"
                + "Link: "+ currentURL +"\n\n"
                + "Best regards,\n"
                + "The ZimTrade Team";

        try {
            sendScorecardEmail(request, recipient, subject, template);
            PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }

        PortletUtils.addInfoMsg("Scorecard successfully submitted for approval. An email was sent to your supervisor", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping(value = "/submit-employee-scores", method = RequestMethod.POST)
    public String submitEmployeeScore(HttpServletRequest request, Scorecard updatedScorecard) throws MalformedURLException {

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());

        scorecard.setApprovalStatus(PMConstants.APPROVAL_STATUS_SCORED_BY_EMPLOYEE);
        scorecardService.saveScorecard(scorecard);
        Account supervisor = scorecard.getOwner().getSupervisor();

        URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
        String recipient = supervisor.getEmail();

        String subject = "Scorecard Scoring,";
        String template = "Good day, \n\n"
                        + "Please note that "+ scorecard.getOwner().getFullName() +" has submitted his/her scorecard for scoring by supervisor. "
                        + "You can now login and add your scores\n"
                        + "Link: "+ currentURL +"\n\n"
                        + "Best regards,\n"
                        + "The ZimTrade Team";
        try {
            sendScorecardEmail(request, recipient, subject, template);
            PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }

        PortletUtils.addInfoMsg("Scorecard successfully submitted for scoring by supervisor. An email was sent to "+ supervisor.getFullName(), request);
        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping(value = "/submit-manager-scores", method = RequestMethod.POST)
    public String submitManagerScore(HttpServletRequest request, Scorecard updatedScorecard) throws MalformedURLException {

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());

        scorecard.setApprovalStatus(PMConstants.APPROVAL_STATUS_SCORED_BY_SUPERVISOR);
        scorecardService.saveScorecard(scorecard);
        Account supervisor = scorecard.getOwner().getSupervisor();
        Account owner = scorecard.getOwner();
        URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));

        String recipient = owner.getEmail();
        String subject = "Scorecard Scoring,";
        String template = "Good day, \n\n"
                + "Please note that "+ supervisor.getFullName() +" has captured scores on your scorecard. Be prepared for the session to capture agreed scores. "
                + "You can now login and add your scores\n"
                + "Link: "+ currentURL + "\n\n"
                + "Best regards,\n"
                + "The ZimTrade Team";
        try {
            sendScorecardEmail(request, recipient, subject, template);
            PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }

        PortletUtils.addInfoMsg("Scorecard scores successfully captured. You can now start to capture the agreed scores", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping(value = "/submit-agreed-scores", method = RequestMethod.POST)
    public String submitAgreedScore(HttpServletRequest request, Scorecard updatedScorecard) throws MalformedURLException {

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());
        scorecard.setApprovalStatus(PMConstants.APPROVAL_STATUS_AGREED_BY_TWO);
        Account loggedUser = commonService.getLoggedUser();
        Account supervisor = scorecard.getOwner().getSupervisor();
        Account owner = scorecard.getOwner();

        try {
            scorecardService.saveScorecard(scorecard);
            URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
            String recipient = owner.getEmail();
            String subject = "Scorecard Scoring,";
            String template = "Good day, \n\n"
                    + "Please note that " + supervisor.getFullName() + " has submitted the agreed scores of your scorecard. HR Moderators will proceed with capturing their input"
                    + "You can now login and see results\n"
                    + "Link: "+ currentURL +"\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";

            String recipient2 = commonService.getHREmail();

            String subject2 = "Scorecard Moderation,";
            String template2 = "Good day, \n\n"
                    + "Please note that " + supervisor.getFullName() + " has submitted their agreed scores with " + owner.getFullName() + ". You can now login and start the moderation process."
                    + "You can now login and see results\n"
                    + "Link: "+ currentURL + "\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";

            try {
                sendScorecardEmail(request, recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            try {
                sendScorecardEmail(request, recipient2, subject2, template2);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient2, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient2 + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }

            PortletUtils.addInfoMsg("Scorecard successfully moderated by HR. Emails were sent to "+ owner.getFullName()+" and "+ supervisor.getFullName(), request);

        }catch (Exception e){

            String recipient = commonService.getAdminEmail();
            String subject = "Scorecard Moderation,";
            String template = "Good day, \n\n"
                    + "Please note that " + loggedUser.getFullName() + " has failed to submit a moderated scorecard for" + owner.getFullName() + ". "
                    + "Kindly assist\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception x){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            PortletUtils.addErrorMsg("Scorecard wasn't submitted. An Email was sent to the administrator", request);
        }

        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }
 @RequestMapping(value = "/submit-moderated-scores", method = RequestMethod.POST)
    public String submitModeratedScorecard(HttpServletRequest request, Scorecard updatedScorecard) {

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());
        scorecard.setApprovalStatus(PMConstants.APPROVAL_STATUS_MODERATED_BY_HR);
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
                    + "Link: "+ currentURL + "\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";

            String recipient2 = supervisor.getEmail();
            String subject2 = "Scorecard Moderation,";
            String template2 = "Good day, \n\n"
                    + "Please note that " + supervisor.getFullName() + " has moderated " + owner.getFullName() + "'s scorecard. "
                    + "You can now login and see results\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";

            try {
                sendScorecardEmail(request, recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            try {
                sendScorecardEmail(request, recipient2, subject2, template2);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient2, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient2 + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }

            PortletUtils.addInfoMsg("Scorecard successfully moderated by HR. Emails were sent to "+ owner.getFullName()+" and "+ supervisor.getFullName(), request);

        }catch (Exception e){

            String recipient = commonService.getAdminEmail();
            String subject = "Scorecard Moderation,";
            String template = "Good day, \n\n"
                    + "Please note that " + loggedUser.getFullName() + " has failed to submit a moderated scorecard for" + owner.getFullName() + ". "
                    + "Kindly assist\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
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
        scorecard.setApprovalStatus(PMConstants.APPROVAL_STATUS_APPROVED_BY_SUPERVISOR);

        try {
            scorecardService.saveScorecard(scorecard);
            try {
                Account loggedUser = commonService.getLoggedUser();
                Approval approval = new Approval();
                approval.setScorecard(scorecard);
                approval.setAccount(loggedUser);
                approval.setStatus(PMConstants.APPROVAL_STATUS_APPROVED_BY_SUPERVISOR);
                approvalService.addApproval(approval);
            }catch (Exception e){
                log.warn("Failed to persist supervisor approval audit for scorecardId={}", scorecard.getId(), e);
            }
            URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
            String subject = "Scorecard Approval,";
            String template = "Good day "+ owner + ", \n\n"
                            + "Please note that "+ supervisor +" has approved your scorecard. "
                            + "We are now waiting for HR to approve so that you can proceed with capturing scores. \n"
                            + "Link: "+ currentURL +"\n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }


            String recipient2 = commonService.getHREmail();
            String subject2 = "Scorecard Approval,";
            String template2 = "Good day HR, \n\n"
                            + "Please note that "+ supervisor +" has approved "+owner+"'s scorecard. "
                            + "You are now eligible to review and approve so that they can proceed with capturing scores. \n"
                            + "Link: "+ currentURL +"\n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, recipient2, subject2, template2);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
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
                            + e.getMessage() +"\n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, admin, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
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
        scorecard.setApprovalStatus(PMConstants.APPROVAL_STATUS_REJECTED_BY_SUPERVISOR);
        scorecard.setLockStatus(PMConstants.LOCK_STATUS_OPEN);

        try{
            scorecardService.saveScorecard(scorecard);

            try {
                Account loggedUser = commonService.getLoggedUser();
                Approval approval = new Approval();
                approval.setScorecard(scorecard);
                approval.setAccount(loggedUser);
                approval.setMessage(message);
                approval.setStatus(PMConstants.APPROVAL_STATUS_REJECTED_BY_SUPERVISOR);
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
                            + "Link: "+currentURL +"\n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
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
                    + e.getMessage() +"\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
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
        scorecard.setApprovalStatus(PMConstants.APPROVAL_STATUS_APPROVED_BY_HR);
        scorecard.setLockStatus(PMConstants.LOCK_STATUS_OPEN);

        try {
            scorecardService.saveScorecard(scorecard);
            try {
                Approval approval = new Approval();
                approval.setScorecard(scorecard);
                approval.setAccount(loggedUser);
                approval.setStatus(PMConstants.APPROVAL_STATUS_APPROVED_BY_HR);
                approvalService.addApproval(approval);
            }catch (Exception e){
                log.warn("Failed to persist HR approval audit for scorecardId={}", scorecard.getId(), e);
            }
            URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
            String subject = "Scorecard Approval,";
            String template = "Good day "+ supervisor + ", \n\n"
                            + "Please note that "+ loggedUser.getFullName() +" has approved "+ owner +" scorecard. "
                            + "The owner is now eligible for capturing scores. \n"
                            + "Link: "+ currentURL +"\n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }


            String recipient2 = scorecard.getOwner().getEmail();
            String subject2 = "Scorecard Approval,";
            String template2 = "Good day "+owner+", \n\n"
                    + "Please note that "+ loggedUser.getFullName() +" has your scorecard. "
                    + "You are now eligible to capture scores. \n"
                    + "Link: "+ currentURL +"\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, recipient2, subject2, template2);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient2, request);
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
                    + e.getMessage() +"\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, admin, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
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
        scorecard.setApprovalStatus(PMConstants.APPROVAL_STATUS_REJECTED_BY_HR);
        Account loggedUser = commonService.getLoggedUser();

        try{
            scorecardService.saveScorecard(scorecard);

            try {

                Approval approval = new Approval();
                approval.setScorecard(scorecard);
                approval.setAccount(loggedUser);
                approval.setMessage(message);
                approval.setStatus(PMConstants.APPROVAL_STATUS_REJECTED_BY_HR);
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
                            + "Link: "+ currentURL + "\n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
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
                    + e.getMessage() +"\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, admin, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
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
            double averageEmployeeScore = goalService.getAverageEmployeeScore(id);
            double averageManagerScore = goalService.getAverageManagerScore(id);
            double averageAgreedScore = goalService.getAverageAgreedScore(id);
            double averageModeratedScore = goalService.getAverageModeratorScore(id);
            double totalAllocatedWeight = goalService.getTotalAllocatedWeight(id);
            List<Target> targetsList = targetService.getAllTargetsByScorecard(id);
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
            modelAndView.addObject("comment", new Comment());
            modelAndView.addObject("averageEmployeeScore", averageEmployeeScore);
            modelAndView.addObject("averageManagerScore", averageManagerScore);
            modelAndView.addObject("averageAgreedScore", averageAgreedScore);
            modelAndView.addObject("averageModeratedScore", averageModeratedScore);
            modelAndView.addObject("totalAllocatedWeight", totalAllocatedWeight);
            modelAndView.addObject("totalWeightedScore", totalWeightedScore);
            modelAndView.addObject("isSupervisor", commonService.isSupervisor(commonService.getLoggedUser()));
            modelAndView.addObject("canApprove", commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_SCORECARD, scorecard));
            modelAndView.addObject("canCaptureTargets", commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_TARGETS, scorecard));
            modelAndView.addObject("canCaptureEmployeeScore", commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES, scorecard));
            modelAndView.addObject("canCaptureManagerScore", commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES, scorecard));
            modelAndView.addObject("canCaptureAgreedScoreAgreedScore", commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES, scorecard));
            modelAndView.addObject("canModerate", commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES, scorecard));

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
                + "Link: "+ link + "\n\n"
                + "Best regards,\n"
                + "The ZimTrade Team";
        try {
            sendScorecardEmail(request, recipient, subject, template);
            PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
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
                + "Link: "+ link + "\n\n"
                + "Best regards,\n"
                + "The ZimTrade Team";
        try {
            sendScorecardEmail(request, recipient, subject, template);
            PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }
        PortletUtils.addInfoMsg("Measure successfully flagged and the reason was saved", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecardId;
    }

    @RequestMapping(value = "/save-standard-score", method = RequestMethod.POST, consumes = {"*/*"})
    public void saveStandardScore( HttpServletResponse response, Long targetId, Double actual, String evidence, String justification) {

        ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
        if (!reportingDateService.isReportingDateOpen(reportingDate)) {
            writeScoreSaveResponse(response, true, "Scores can only be captured for an open reporting date");
            return;
        }
        Target target = targetService.getTargetById(targetId);

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

    private boolean isStageCaptureAllowed(Scorecard scorecard, ValueBasedCaptureStage stage) {
        switch (stage) {
            case EMPLOYEE:
                return commonService.isOwner(scorecard);
            case MANAGER:
            case AGREED:
                return commonService.isSupervisor(scorecard.getOwner());
            case MODERATED:
                return commonService.isModerator() && PMConstants.APPROVAL_STATUS_AGREED_BY_TWO.equalsIgnoreCase(scorecard.getApprovalStatus());
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
        String normalizedSubject = normalizeScorecardEmailSubject(subject);
        String normalizedBody = normalizeScorecardEmailBody(body);

        String platformUrl = normalizeBaseUrl(commonService.getCurrentUrl(request));
        if (!platformUrl.isEmpty() && (!hasHttpLink(normalizedBody) || !normalizedBody.contains("Open Platform: "))) {
            normalizedBody = appendParagraph(normalizedBody, "Open Platform: " + platformUrl);
        }

        boolean sent = notificationService.sendUserMessage(recipient, null, normalizedSubject, normalizedBody);
        if (!sent) {
            throw new RuntimeException("Notification delivery failed");
        }
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
        String normalized = body == null ? "" : body.trim();
        normalized = normalized.replace(" login ", " log in ");
        normalized = normalized.replace("Login ", "Log in ");
        normalized = normalized.replace("response or action", "respond or take action");
        normalized = normalized.replace("You can now login", "You can now log in");
        return normalized;
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
            newScorecard.setApprovalStatus(PMConstants.APPROVAL_STATUS_NEW);
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
                    + "You can now login and modify targets to match your performance goals\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                sendScorecardEmail(request, recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
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
                stage2 = "Strategic Objective";
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
