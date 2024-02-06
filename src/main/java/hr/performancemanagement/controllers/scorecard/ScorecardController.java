package hr.performancemanagement.controllers.scorecard;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.service.*;
import hr.performancemanagement.service.ScoreService.StandardScorecardScoreService;
import hr.performancemanagement.service.ScoreService.ValueBasedScoreService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.constants.Pages;
import hr.performancemanagement.utils.wrappers.EvidenceWrapper;
import hr.performancemanagement.utils.wrappers.OutputWrapper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value="/scorecards")
public class ScorecardController {

    @Autowired
    ReportingPeriodService reportingPeriodService;
    @Autowired
    AccountService accountService;
    @Autowired
    ScorecardService scorecardService;
    @Autowired
    GearService gearService;
    @Autowired
    OutcomeService outcomeService;
    @Autowired
    TargetService targetService;
    @Autowired
    GoalService goalService;
    @Autowired
    CommentService commentService;
    @Autowired
    Mailservice mailservice;
    @Autowired
    ApprovalService approvalService;
    @Autowired
    ReportingDateService reportingDateService;
    @Autowired
    StandardScorecardScoreService standardScorecardScoreService;
    @Autowired
    ValueBasedScoreService valueBasedScoreService;
    @Autowired
    ScorecardModelService scorecardModelService;
    @Autowired
    CommonService commonService;
    @Autowired
    OutputService outputService;

    Environment environment;

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request, HttpSession session) {

        List<ReportingPeriod> REPORTING_PERIODS_LIST = reportingPeriodService.listAllReportingPeriods();
        List<Account> ACCOUNTS_LIST = accountService.listAllAccounts();
        List<Gear> gears = gearService.listAllGears(commonService.getLoggedUser().getClientId());
        Account loggedUser = commonService.getLoggedUser();
        long loggedUserId = loggedUser.getId();
        String role = loggedUser.getRole();

        modelAndView.addObject("pageDomain", "Performance");
        modelAndView.addObject("pageName", "Contracts");
        modelAndView.addObject("reportingPeriodsList", REPORTING_PERIODS_LIST);
        modelAndView.addObject("accountsList", ACCOUNTS_LIST);
        modelAndView.addObject("gears", gears);
        modelAndView.addObject("loggedUserId", loggedUserId);
        modelAndView.addObject("loggedUser", loggedUser);
        modelAndView.addObject("role", role);
        PortletUtils.addMessagesToPage(modelAndView, request);

    }

    @RequestMapping
    public ModelAndView viewScorecards(HttpServletRequest request, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_SCORECARDS);
        modelAndView.addObject("pageTitle", "View Contracts");
        ReportingPeriod reportingPeriod = reportingPeriodService.getActiveReportingPeriod();
        List<Scorecard> scorecards = scorecardService.getScorecardsByReportingPeriodId(reportingPeriod);

        modelAndView.addObject("scorecards", scorecards);
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping(value = "/view-user-scorecards/{id}")
    public ModelAndView viewUserScorecards(@PathVariable("id") Long userId, HttpServletRequest request, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_USER_SCORECARDS);
        Account owner = accountService.getAccountById(userId);
        modelAndView.addObject("pageTitle", "View " + owner.getFullName() + "'s Contracts");
        List<Scorecard> scorecards = scorecardService.getScorecardsByOwner(owner);

        modelAndView.addObject("scorecards", scorecards);
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping("/add-scorecard")
    public ModelAndView addAScorecard(HttpServletRequest request, HttpSession session) {
        ScorecardModel scorecardModel = scorecardModelService.getActiveScorecardModel();
        ModelAndView modelAndView = new ModelAndView(Pages.ADD_SCORECARD);
        modelAndView.addObject("pageTitle", "New Contract");
        modelAndView.addObject("scorecard", new Scorecard());
        modelAndView.addObject("scorecardModel", scorecardModel);
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping(value = "/save-scorecard", method = RequestMethod.POST)
    public String saveScorecard(HttpServletRequest request, Scorecard newScorecard) throws UnsupportedEncodingException {

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
                mailservice.sendEmail(recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }


            if(newScorecard.getOwner().getId() == loggedUser.getId()){
                PortletUtils.addInfoMsg("Scorecard successfully created. You can proceed with capturing targets", request);
                return "redirect:/scorecards/capture-targets/"+ newScorecard.getId();
            }else {
                PortletUtils.addInfoMsg("Scorecard successfully created. You can proceed with creating other contracts", request);
                return "redirect:/scorecards/add-scorecard";
            }
        }
    }

    @RequestMapping("/capture-targets/{id}")
    public ModelAndView captureTargets(@PathVariable("id") long id, HttpServletRequest request, HttpSession session) {

        Scorecard scorecard = scorecardService.getScorecardById(id);
        String scorecardModel = scorecard.getScorecardModel().getName();
        List<Gear> gears = gearService.listAllGears(commonService.getLoggedUser().getClientId());
        List<Gear> selectedGears = gearService.listSelectedGears(scorecard);
        List<Gear> remainingGears = gearService.listRemainingGears(scorecard);
        ModelAndView modelAndView;

        if(commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_TARGETS,scorecard)){

            modelAndView = new ModelAndView(Pages.CAPTURE_TARGETS);
            modelAndView.addObject("pageTitle", "Capture Targets {"+ scorecard.getOwner().getFullName() +"}");
            modelAndView.addObject("scorecard", scorecard);
            modelAndView.addObject("selectedGears", selectedGears);
            modelAndView.addObject("remainingGears", remainingGears);
            modelAndView.addObject("gears", gears);
            modelAndView.addObject("totalAllocatedWeight", outcomeService.getTotalAllocatedWeight(id));
            modelAndView.addObject("scorecardModel", scorecardModel);
            List<Target> targetsList = targetService.getAllTargetsByScorecard(scorecard);
            modelAndView.addObject("targetsList", targetsList);

        } else {

            modelAndView = new ModelAndView(Pages.BLANK_PAGE);
            PortletUtils.addErrorMsg("You are not allowed to capture targets on this scorecard at this moment", request);
        }
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping("/capture-scores/{id}")
    public ModelAndView captureScores(@PathVariable("id") long id, HttpServletRequest request, HttpSession session) {

        Scorecard scorecard = scorecardService.getScorecardById(id);
        String scorecardModel = scorecard.getScorecardModel().getName();
        ReportingPeriod reportingPeriod = scorecard.getReportingPeriod();
        ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
        List<ReportingDate> reportingDates = reportingDateService.listAllReportingDates(reportingPeriod);
        String url = "";

        double averageEmployeeScore = outcomeService.getAverageEmployeeScore(id);
        double averageManagerScore = outcomeService.getAverageManagerScore(id);
        double averageAgreedScore = outcomeService.getAverageAgreedScore(id);
        double averageModeratedScore = outcomeService.getAverageModeratorScore(id);
        double totalAllocatedWeight = outcomeService.getTotalAllocatedWeight(id);

        double weightedScore;
        try {
            weightedScore = (averageModeratedScore / 5 ) * 100;
        }catch (Exception e){
            weightedScore = 0;
        }

        ModelAndView modelAndView;

//            if(PMConstants.STANDARD_SCORECARD.equalsIgnoreCase(scorecardModel)){
//                modelAndView = new ModelAndView(Pages.CAPTURE_SCORES_STANDARD);
//            }else if(PMConstants.VALUE_BASED.equalsIgnoreCase(scorecardModel)){
//                if(commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES, scorecard)){
//                    modelAndView = new ModelAndView(Pages.CAPTURE_EMPLOYEE_SCORE);
//                    url = "submit-employee-scores";
//                } else if (commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MANAGER_SCORES, scorecard)) {
//                    modelAndView = new ModelAndView(Pages.CAPTURE_MANAGER_SCORE);
//                    url = "submit-manager-scores";
//                } else if (commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES, scorecard)) {
//                    modelAndView = new ModelAndView(Pages.CAPTURE_AGREED_SCORE);
//                    url = "submit-agreed-scores";
//                } else if (commonService.isUserAllowed(PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES, scorecard)) {
//                    modelAndView = new ModelAndView(Pages.CAPTURE_MODERATED_SCORE);
//                    url = "submit-moderated-scores";
//                }else {
                    modelAndView = new ModelAndView(Pages.BLANK_PAGE);
                    PortletUtils.addErrorMsg("You are not allowed to capture scores on this scorecard", request);
//                }
//            }else{
//                modelAndView = new ModelAndView(Pages.BLANK_PAGE);
//                PortletUtils.addErrorMsg("It shows like the scoring model is not defined. Contact the administrator", request);
//            }

            modelAndView.addObject("pageTitle", "Capture Scores");
            modelAndView.addObject("scorecard", scorecard);
            List<Target> targetsList = targetService.getAllTargetsByScorecard(scorecard);
            modelAndView.addObject("targetsList", targetsList);
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

    @RequestMapping(value = "/save-target", method = RequestMethod.POST)
    public String saveTarget(OutputWrapper wrapper) {

        long scorecardId = wrapper.getScorecardId();
        Output output;
        Target target;

        if(wrapper.getOutputId() > 0){
            output = outputService.getOutputById(wrapper.getOutputId());
            output.setName(wrapper.getName());
        }
        else if(outputService.outputExists(wrapper.getName())){
            output = outputService.getOutputByName(wrapper.getName());
        }else {
            output = new Output();
            output.setName(wrapper.getName());
            output.setScorecard(scorecardService.getScorecardById(scorecardId));
        }

        output.setOutcome(outcomeService.getOutcomeById(wrapper.getOutcomeId()));
        output.setWeight(wrapper.getAllocatedWeight());

        Output savedOutput = outputService.saveOutput(output);

        if(wrapper.getTargetId() < 1){
            target = new Target();
        }else{
            target = targetService.getTargetById(wrapper.getTargetId());
        }

        target.setOutput(savedOutput);
        target.setMeasure(wrapper.getMeasure());
        target.setUnit(wrapper.getUnit());
        target.setAllocatedWeight(wrapper.getAllocatedWeight());
        target.setNormalTarget(wrapper.getNormalTarget());
        target.setBaseTarget(wrapper.getBaseTarget());
        target.setStretchTarget(wrapper.getStretchTarget());
        targetService.saveTarget(target);

        return "redirect:/scorecards/capture-targets/"+ scorecardId;
    }

    @RequestMapping(value = "/save-target-to-existing-output", method = RequestMethod.POST)
    public String saveTargetToExistingGoal(Target target) {

        long scorecardId = target.getOutput().getScorecard().getId();
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
                                + "Please note that"+ commonService.getLoggedUser().getFullName() +" added an overall comment on your contract. "
                                + "Message: "+ comment + "\n"
                                + "You can now login and response or action\n"
                                + "Link: "+ link + "\n\n"
                                + "Best regards,\n"
                                + "The ZimTrade Team";
            try {
                mailservice.sendEmail(recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
        }

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("alreadyExists", false);

        String jsonString = jsonObject.toString();

        try(OutputStream outputStream = response.getOutputStream()){
            outputStream.write(jsonString.getBytes());

        }catch (IOException e){
            throw  new RuntimeException();
        }
    }

    @RequestMapping(value = "/delete-target", method = RequestMethod.POST)
    public String deleteTarget(HttpServletRequest request, Target targ) {

        Target target = targetService.getTargetById(targ.getId());
        Output output = outputService.getOutputById(target.getOutput().getId());
        boolean hasTargets = targetService.checkIfOutputHasTargets(output);
        long scorecardId = output.getScorecard().getId();

        try {
            targetService.deleteTarget(target);
            PortletUtils.addInfoMsg("Target was successfully deleted", request);
            if(!hasTargets){
                outputService.deleteOutput(output);
                PortletUtils.addInfoMsg("Output was successfully deleted", request);
              }
        }catch (Exception e){
            PortletUtils.addErrorMsg("Output wasn't deleted", request);
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
                + "Please note that "+ scorecard.getOwner().getFullName() +" has submitted his/her contract for your approval. "
                + "You can now login and approve\n"
                + "Link: "+ currentURL +"\n\n"
                + "Best regards,\n"
                + "The ZimTrade Team";

        try {
            mailservice.sendEmail(recipient, subject, template);
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
                        + "Please note that "+ scorecard.getOwner().getFullName() +" has submitted his/her contract for scoring by supervisor. "
                        + "You can now login and add your scores\n"
                        + "Link: "+ currentURL +"\n\n"
                        + "Best regards,\n"
                        + "The ZimTrade Team";
        try {
            mailservice.sendEmail(recipient, subject, template);
            PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }

        PortletUtils.addInfoMsg("Contract successfully submitted for scoring by supervisor. An email was sent to "+ supervisor.getFullName(), request);
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
                + "Please note that "+ supervisor.getFullName() +" has captured scores on your contract. Be prepared for the session to capture agreed scores. "
                + "You can now login and add your scores\n"
                + "Link: "+ currentURL + "\n\n"
                + "Best regards,\n"
                + "The ZimTrade Team";
        try {
            mailservice.sendEmail(recipient, subject, template);
            PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }

        PortletUtils.addInfoMsg("Contract scores successfully captured. You can now start to capture the agreed scores", request);
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
            String subject = "Contract Scoring,";
            String template = "Good day, \n\n"
                    + "Please note that " + supervisor.getFullName() + " has submitted the agreed scores of your scorecard. HR Moderators will proceed with capturing their input"
                    + "You can now login and see results\n"
                    + "Link: "+ currentURL +"\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";

            String recipient2 = commonService.getHREmail();

            String subject2 = "Contract Moderation,";
            String template2 = "Good day, \n\n"
                    + "Please note that " + supervisor.getFullName() + " has submitted their agreed scores with " + owner.getFullName() + ". You can now login and start the moderation process."
                    + "You can now login and see results\n"
                    + "Link: "+ currentURL + "\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";

            try {
                mailservice.sendEmail(recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            try {
                mailservice.sendEmail(recipient2, subject2, template2);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient2, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient2 + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }

            PortletUtils.addInfoMsg("Contract successfully moderated by HR. Emails were sent to "+ owner.getFullName()+" and "+ supervisor.getFullName(), request);

        }catch (Exception e){

            String recipient = commonService.getAdminEmail();
            String subject = "Contract Moderation,";
            String template = "Good day, \n\n"
                    + "Please note that " + loggedUser.getFullName() + " has failed to submit a moderated scorecard for" + owner.getFullName() + ". "
                    + "Kindly assist\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                mailservice.sendEmail(recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception x){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            PortletUtils.addErrorMsg("Contract wasn't submitted. An Email was sent to the administrator", request);
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
            String subject = "Contract Moderation,";
            String template = "Good day, \n\n"
                    + "Please note that " + loggedUser.getFullName() + " has moderated your contract. "
                    + "You can now login and see results\n"
                    + "Link: "+ currentURL + "\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";

            String recipient2 = supervisor.getEmail();
            String subject2 = "Contract Moderation,";
            String template2 = "Good day, \n\n"
                    + "Please note that " + supervisor.getFullName() + " has moderated " + owner.getFullName() + "'s contract. "
                    + "You can now login and see results\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";

            try {
                mailservice.sendEmail(recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            try {
                mailservice.sendEmail(recipient2, subject2, template2);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient2, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient2 + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }

            PortletUtils.addInfoMsg("Contract successfully moderated by HR. Emails were sent to "+ owner.getFullName()+" and "+ supervisor.getFullName(), request);

        }catch (Exception e){

            String recipient = commonService.getAdminEmail();
            String subject = "Contract Moderation,";
            String template = "Good day, \n\n"
                    + "Please note that " + loggedUser.getFullName() + " has failed to submit a moderated scorecard for" + owner.getFullName() + ". "
                    + "Kindly assist\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                mailservice.sendEmail(recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception x){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            PortletUtils.addErrorMsg("Contract wasn't submitted. An Email was sent to the administrator", request);
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
                e.printStackTrace();
            }
            URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
            String subject = "Contract Approval,";
            String template = "Good day "+ owner + ", \n\n"
                            + "Please note that "+ supervisor +" has approved your contract. "
                            + "We are now waiting for HR to approve so that you can proceed with capturing scores. \n"
                            + "Link: "+ currentURL +"\n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            try {
                mailservice.sendEmail(recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }


            String recipient2 = commonService.getHREmail();
            String subject2 = "Contract Approval,";
            String template2 = "Good day HR, \n\n"
                            + "Please note that "+ supervisor +" has approved "+owner+"'s contract. "
                            + "You are now eligible to review and approve so that they can proceed with capturing scores. \n"
                            + "Link: "+ currentURL +"\n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            try {
                mailservice.sendEmail(recipient2, subject2, template2);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }


            PortletUtils.addInfoMsg("Contract successfully approved. An email was sent to HR for further approval and to "+ owner + " as feedback", request);
            return "redirect:/scorecards/view-scorecard/"+ id;
        }catch (Exception e){

            String admin = commonService.getAdminEmail();
            String subject  = "Technical failure,";
            String template = "Good day, \n\n"
                            + "Please note that "+ supervisor +"  failed to approve "+ owner +"'s contract. "
                            + "With error :. \n\n"
                            + e.getMessage() +"\n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            try {
                mailservice.sendEmail(admin, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception x){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            PortletUtils.addInfoMsg("Contract approval failed. An email was sent to the administrator with error details. ", request);
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
                e.printStackTrace();
            }
            URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
            String subject = "Scorecard Approval,";
            String template = "Good day "+ owner + ", \n\n"
                            + "Please note that "+ supervisor +" has rejected your scorecard with the following message \n\n. "
                            +".........................................................................................\n"
                            + message + "\n"
                            +".........................................................................................\n\n"
                            + "Please log in and make recommended changes. Also look for comments and flags on your contract and rectify\n"
                            + "Link: "+currentURL +"\n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            try {
                mailservice.sendEmail(recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }

            PortletUtils.addInfoMsg("Contract successfully rejected. An email was sent to "+ owner + " as feedback", request);

        }catch (Exception e){

            String admin = commonService.getAdminEmail();
            String subject  = "Technical failure,";
            String template = "Good day, \n\n"
                    + "Please note that "+ supervisor +"  failed to approve "+ owner +"'s contract. "
                    + "With error :. \n\n"
                    + e.getMessage() +"\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                mailservice.sendEmail(recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception x){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            PortletUtils.addInfoMsg("Contract approval failed. An email was sent to the administrator with error details. ", request);
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
                e.printStackTrace();
            }
            URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
            String subject = "Contract Approval,";
            String template = "Good day "+ supervisor + ", \n\n"
                            + "Please note that "+ loggedUser.getFullName() +" has approved "+ owner +" contract. "
                            + "The owner is now eligible for capturing scores. \n"
                            + "Link: "+ currentURL +"\n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            try {
                mailservice.sendEmail(recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }


            String recipient2 = scorecard.getOwner().getEmail();
            String subject2 = "Contract Approval,";
            String template2 = "Good day "+owner+", \n\n"
                    + "Please note that "+ loggedUser.getFullName() +" has your contract. "
                    + "You are now eligible to capture scores. \n"
                    + "Link: "+ currentURL +"\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                mailservice.sendEmail(recipient2, subject2, template2);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient2, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }


            PortletUtils.addInfoMsg("Contract successfully approved. An email was sent to "+supervisor+" and to "+ owner + " as feedback", request);
            return "redirect:/scorecards/view-scorecard/"+ id;
        }catch (Exception e){

            String admin = commonService.getAdminEmail();
            String subject  = "Technical failure,";
            String template = "Good day, \n\n"
                    + "Please note that "+ loggedUser.getFullName() +"  failed to approve "+ owner +"'s contract. "
                    + "With error :. \n\n"
                    + e.getMessage() +"\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                mailservice.sendEmail(admin, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception x){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            PortletUtils.addInfoMsg("Contract approval failed. An email was sent to the administrator with error details. ", request);
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
              e.printStackTrace();
            }
            URL currentURL = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecard.getId()));
            String subject = "Contract Approval,";
            String template = "Good day "+ supervisor + ", \n\n"
                            + "Please note that "+ loggedUser.getFullName() +" has rejected "+ owner+"'s scorecard with the following message \n\n. "
                            +".........................................................................................\n"
                            + message + "\n"
                            +".........................................................................................\n\n"
                            + "Please log in and make recommended changes. Also look for comments and flags on your contract and rectify\n"
                            + "Link: "+ currentURL + "\n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            try {
                mailservice.sendEmail(recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }

            PortletUtils.addInfoMsg("Contract successfully rejected. An email was sent to "+ supervisor + " as feedback", request);

        }catch (Exception e){

            String admin = commonService.getAdminEmail();
            String subject  = "Technical failure,";
            String template = "Good day, \n\n"
                    + "Please note that "+ loggedUser.getFullName() +"  failed to reject "+ owner +"'s contract. "
                    + "With error :. \n\n"
                    + e.getMessage() +"\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                mailservice.sendEmail(admin, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception x){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            PortletUtils.addInfoMsg("Contract approval failed. An email was sent to the administrator with error details. ", request);
        }

        return "redirect:/scorecards/view-scorecard/"+ id;
    }

    @RequestMapping("/view-scorecard/{id}")
    public ModelAndView viewScorecard(@PathVariable("id") long id, HttpServletRequest request, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_SCORECARD);
        try {
            Scorecard scorecard = scorecardService.getScorecardById(id);
            String scorecardModel = scorecard.getScorecardModel().getName();
            double averageEmployeeScore = outcomeService.getAverageEmployeeScore(id);
            double averageManagerScore = outcomeService.getAverageManagerScore(id);
            double averageAgreedScore = outcomeService.getAverageAgreedScore(id);
            double averageModeratedScore = outcomeService.getAverageModeratorScore(id);
            double totalAllocatedWeight = outcomeService.getTotalAllocatedWeight(id);
            List<Target> targetsList = targetService.getAllTargetsByScorecard(scorecard);
            List<Gear> selectedGears = gearService.listSelectedGears(scorecard);
            double totalWeightedScore = 0.0;
            for(Target target: targetsList){
                if(target.getWeightedScore() !=null){
                    totalWeightedScore += target.getWeightedScore();
                }

            }

            modelAndView.addObject("pageTitle", "View Contract {"+ scorecard.getOwner().getFullName() +"}");
            modelAndView.addObject("scorecard", scorecard);
            modelAndView.addObject("scorecardModel", scorecardModel);
            modelAndView.addObject("targetsList", targetsList);
            modelAndView.addObject("selectedGears", selectedGears);
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
            PortletUtils.addErrorMsg("That scorecard cannot be found", request);
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
        Long scorecardId = comment.getTarget().getOutput().getScorecard().getId();
        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        URL link = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecardId));

        String recipient = scorecard.getOwner().getEmail();
        String subject = "Scorecard Comment,";
        String template = "Good day, \n\n"
                + "Please note that"+ loggedUser.getFullName() +" added a comment on your contract. "
                + "Goal - measure: [" + comment.getTarget().getOutput().getName() +" - "+ comment.getTarget().getMeasure() +"]\n"
                + "Message: "+ comment.getName() + "\n"
                + "You can now login and response or action\n"
                + "Link: "+ link + "\n\n"
                + "Best regards,\n"
                + "The ZimTrade Team";
        try {
            mailservice.sendEmail(recipient, subject, template);
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
        long scorecardId = target.getOutput().getScorecard().getId();
        Scorecard scorecard = scorecardService.getScorecardById(scorecardId);
        target.setFlag(updatedTarget.getFlag());
        targetService.saveTarget(target);
        URL link = new URL(commonService.getCurrentUrl(request).concat("/scorecards/view-scorecard/"+ scorecardId));
        String fullName = commonService.getLoggedUser().getFullName();

        String recipient = scorecard.getOwner().getEmail();
        String subject = "Scorecard Comment,";
        String template = "Good day, \n\n"
                + "Please note that"+ fullName +" flagged a goal on your contract. "
                + "Goal - measure: [" + target.getOutput().getName() +" - "+ target.getMeasure() +"]\n"
                + "Message: "+ target.getFlag() + "\n"
                + "You can now login and response or action\n"
                + "Link: "+ link + "\n\n"
                + "Best regards,\n"
                + "The ZimTrade Team";
        try {
            mailservice.sendEmail(recipient, subject, template);
            PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
        }catch (Exception e){
            PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
        }
        PortletUtils.addInfoMsg("Target successfully flagged and the reason was saved", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecardId;
    }

    @RequestMapping(value = "/save-standard-score", method = RequestMethod.POST, consumes = {"*/*"})
    public void saveStandardScore( HttpServletResponse response, Long targetId, Double actual, String evidence, String justification) {

        ReportingDate reportingDate = reportingDateService.getActiveReportingDate();
        Target target = targetService.getTargetById(targetId);

        Score score = new Score();
        score.setTarget(target);
        score.setReportingDate(reportingDate);
        score.setEvidence(evidence);
        score.setJustification(justification);
        score.setActual(actual);

        JSONObject jsonObject = new JSONObject();

            standardScorecardScoreService.saveScore(score);
            jsonObject.put("alreadyExists", false);


        String jsonString = jsonObject.toString();

        try(OutputStream outputStream = response.getOutputStream()){
            outputStream.write(jsonString.getBytes());

        }catch (IOException e){
            throw  new RuntimeException();
        }

    }

    @RequestMapping(value = "/save-value-based-employee-score", method = RequestMethod.POST)
    public void saveEmployeeScore(HttpServletRequest request, HttpServletResponse response, Long targetId, Double employeeScore, String justification) {

        try {

            Target target = targetService.getTargetById(targetId);
            Scorecard scorecard = scorecardService.getScorecardById(target.getOutput().getScorecard().getId());

            if(commonService.isOwner(scorecard)){
                Score score = new Score();
                score.setTarget(target);
                score.setReportingDate(commonService.getActiveReportingDate(request));
                score.setEmployeeScore(employeeScore);
                score.setJustification(justification);
                valueBasedScoreService.saveEmployeeScore(score);
            }
        }catch (Exception ignored){

        }

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("alreadyExists", false);
        String jsonString = jsonObject.toString();

        try(OutputStream outputStream = response.getOutputStream()){
            outputStream.write(jsonString.getBytes());

        }catch (IOException e){
            throw  new RuntimeException();
        }
    }

    @RequestMapping(value = "/save-value-based-evidence", method = RequestMethod.POST)
    public String saveValueBasedEvidence(EvidenceWrapper wrapper, HttpServletRequest request){

        Target target = targetService.getTargetById(wrapper.getTargetId());
        Scorecard scorecard = scorecardService.getScorecardById(target.getOutput().getScorecard().getId());

        MultipartFile file = wrapper.getAttachment();
        String fileName = file.getOriginalFilename();
        try {
            if(fileName != ""){
                file.transferTo( new File(fileName));
            }

        } catch (Exception e) {

        }
        try {
            if(commonService.isOwner(scorecard)){
                Score score = new Score();
                score.setTarget(target);
                score.setReportingDate(commonService.getActiveReportingDate(request));
                score.setEvidence(wrapper.getEvidence());
                score.setAttachmentName(fileName);
                valueBasedScoreService.saveEvidence(score);
            }
        }catch (Exception ignored){

        }
        return "redirect:/scorecards/capture-scores/"+ scorecard.getId();
    }

    @RequestMapping(value = "/save-value-based-manager-score", method = RequestMethod.POST, consumes = {"*/*"})
    public void saveManagerScore(HttpServletRequest request, HttpServletResponse response, Long targetId, Double managerScore) {

        try {
            Target target = targetService.getTargetById(targetId);
            Scorecard scorecard = scorecardService.getScorecardById(target.getOutput().getScorecard().getId());

            if(commonService.isSupervisor(scorecard.getOwner())){
                Score score = new Score();
                score.setTarget(target);
                score.setReportingDate(commonService.getActiveReportingDate(request));
                score.setManagerScore(managerScore);
                valueBasedScoreService.saveManagerScore(score);
            }
        }catch (Exception ignored){

        }

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("alreadyExists", false);
        String jsonString = jsonObject.toString();

        try(OutputStream outputStream = response.getOutputStream()){
            outputStream.write(jsonString.getBytes());
        }catch (IOException e){
            throw  new RuntimeException();
        }
    }

    @RequestMapping(value = "/save-value-based-agreed-score", method = RequestMethod.POST, consumes = {"*/*"})
    public void saveAgreedScore(HttpServletRequest request, HttpServletResponse response, Long targetId, Double agreedScore) {

        try {
            Target target = targetService.getTargetById(targetId);
            Scorecard scorecard = scorecardService.getScorecardById(target.getOutput().getScorecard().getId());

            if(commonService.isSupervisor(scorecard.getOwner())){
                Score score = new Score();
                score.setTarget(target);
                score.setReportingDate(commonService.getActiveReportingDate(request));
                score.setAgreedScore(agreedScore);
                valueBasedScoreService.saveAgreedScore(score);
            }
        }catch (Exception ignored){

        }

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("alreadyExists", false);
        String jsonString = jsonObject.toString();

        try(OutputStream outputStream = response.getOutputStream()){
            outputStream.write(jsonString.getBytes());

        }catch (IOException e){
            throw  new RuntimeException();
        }
    }

    @RequestMapping(value = "/save-value-based-moderated-score", method = RequestMethod.POST, consumes = {"*/*"})
    public void saveModeratedScore(HttpServletRequest request, HttpServletResponse response, Long targetId, Double moderatedScore) {

        try {
            Target target = targetService.getTargetById(targetId);
            Scorecard scorecard = scorecardService.getScorecardById(target.getOutput().getScorecard().getId());

            if(commonService.isModerator() && PMConstants.APPROVAL_STATUS_AGREED_BY_TWO.equalsIgnoreCase(scorecard.getApprovalStatus())){
                Score score = new Score();
                score.setTarget(target);
                score.setReportingDate(commonService.getActiveReportingDate(request));
                score.setModeratedScore(moderatedScore);
                valueBasedScoreService.saveModeratedScore(score);
            }
        }catch (Exception ignored){

        }

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("alreadyExists", false);
        String jsonString = jsonObject.toString();

        try(OutputStream outputStream = response.getOutputStream()){
            outputStream.write(jsonString.getBytes());

        }catch (IOException e){
            throw  new RuntimeException();
        }
    }

    @RequestMapping(value = "/fake-save-scorecard", method = RequestMethod.POST)
    public String fakeSaveScorecard(HttpServletRequest request, Scorecard scorecard) {
        PortletUtils.addInfoMsg("Contract successfully saved for later. You can update it anytime before submitting for approval", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping("/clone-scorecard/{id}")
    public ModelAndView cloneScorecard(@PathVariable("id") long id, HttpServletRequest request, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.CLONE_SCORECARD);
        modelAndView.addObject("pageTitle", "Clone Contract");
        Scorecard scorecard = scorecardService.getScorecardById(id);
        modelAndView.addObject("scorecard", scorecard);
        modelAndView.addObject("owner", scorecard.getOwner());
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping("/clone-scorecard-select-owner")
    public ModelAndView cloneScorecardSelect(HttpServletRequest request, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.CLONE_SCORECARD_SELECT_OWNER);
        modelAndView.addObject("pageTitle", "Clone Contract");
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping(value = "/clone-scorecard-save-owner", method = RequestMethod.POST)
    public String cloneScorecardSaveOwner(HttpServletRequest request, Long id) {
        Account owner = accountService.getAccountById(id);
        try {
            Scorecard scorecard = scorecardService.getActiveEmployeeScorecardByOwner(owner);
            PortletUtils.addInfoMsg("You have selected "+ owner.getFullName()+ "'s contract", request);
            return "redirect:/scorecards/clone-scorecard/"+ scorecard.getId();
        }catch (Exception e){
            PortletUtils.addErrorMsg(owner.getFullName()+ " Does not have an active contract. Please try another employee or You may go to view contracts and select the scorecard you want from the archives", request);
            return "redirect:/scorecards/clone-scorecard-select-owner";
        }

    }


    @RequestMapping(value = "/copy-scorecard", method = RequestMethod.POST)
    public String copyScorecard(HttpServletRequest request,Scorecard imaginaryScorecard) {

        if (scorecardService.countActiveScorecards(imaginaryScorecard.getOwner(), imaginaryScorecard.getReportingPeriod()) >= 1) {
            PortletUtils.addErrorMsg(imaginaryScorecard.getOwner().getFullName() + " already has an active contract for the selected reporting period (" + imaginaryScorecard.getReportingPeriod().getStartDate() + " - " + imaginaryScorecard.getReportingPeriod().getEndDate() + ")", request);
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

            newScorecard = scorecardService.saveScorecard(newScorecard);
            long id = newScorecard.getId();
            List<Output> outputs = outputService.listAllOutputs(scorecard);

            for (Output output : outputs) {
                Output newOutput = new Output();

                newOutput.setScorecard(newScorecard);
                newOutput.setOutcome(output.getOutcome());
                newOutput.setWeight(output.getWeight());
                newOutput.setName(output.getName());

                Output savedOut = outputService.saveOutput(newOutput);

                List<Target> targetList = targetService.getAllTargetsByOutput(output);
                for(Target target: targetList){
                    Target newTarget = new Target();
                    newTarget.setOutput(savedOut);
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
            String subject = "Contract Creation,";
            String template = "Good day, \n\n"
                    + "We are pleased to notify you that your contract has been cloned and is already populated with targets. "
                    + "You can now login and modify targets to match your performance goals\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            try {
                mailservice.sendEmail(recipient, subject, template);
                PortletUtils.addInfoMsg("An email alert successfully sent to "+ recipient, request);
            }catch (Exception e){
                PortletUtils.addErrorMsg("Email to "+ recipient + " failed to send. It's likely due to a network issue. Must be alerted offline", request);
            }
            return "redirect:/scorecards/view-scorecard/" + id;
        }
    }

    @RequestMapping("/view-evidence/{fileName}")
    public void viewEvidence(@PathVariable("fileName") String fileName, HttpServletResponse response, HttpServletRequest request) throws IOException {
        String pathto = environment.getProperty("spring.servlet.multipart.location");
        String path = pathto.concat(fileName);

        File file = new File(path);

        try {
            FileInputStream inputStream = new FileInputStream(file);
            String ext = commonService.getFileExtention(fileName);
            boolean supportedFile = false;
            if (ext != null) {
                if ("pdf".equalsIgnoreCase(ext)) {
                    response.setContentType("application/pdf");
                    supportedFile = true;
                } else if ("xls".equalsIgnoreCase(ext) || "xlsx".equalsIgnoreCase(ext) || "csv".equalsIgnoreCase(ext)) {
                    response.setContentType("application/vnd.ms-excel");
                    supportedFile = true;
                } else if ("docx".equalsIgnoreCase(ext)) {
                    response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                    supportedFile = true;
                } else if ("doc".equalsIgnoreCase(ext)) {
                    response.setContentType("application/msword");
                    supportedFile = true;
                } else if ("png".equalsIgnoreCase(ext)) {
                    response.setContentType("image/png");
                    supportedFile = true;
                } else if ("jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext)) {
                    response.setContentType("image/jpeg");
                    supportedFile = true;
                } else if ("pptx".equalsIgnoreCase(ext)) {
                    response.setContentType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
                    supportedFile = true;
                } else if ("ppt".equalsIgnoreCase(ext)) {
                    response.setContentType("application/vnd.ms-powerpoint");
                    supportedFile = true;
                } else {

                }
            }

            if (supportedFile) {
                System.out.println("File PAth to ########: "+ pathto);
                System.out.println("File PAth ########## : "+ path);
                response.setContentLength((int) file.length());
                response.setHeader("Content-Disposition", "inline;filename=\"" + fileName + "\"");
                FileCopyUtils.copy(inputStream, response.getOutputStream());
            }

        } catch (Exception e) {
//            PortletUtils.addErrorMsg("Failed to view file with error: " + e.getMessage(), request);
            fileName = "404.png";
            path = pathto.concat(fileName);
            file = new File(path);
            FileInputStream inputStream = new FileInputStream(file);
            response.setContentType("image/png");
            response.setContentLength((int) file.length());
            response.setHeader("Content-Disposition", "inline;filename=\"" + fileName + "\"");
            FileCopyUtils.copy(inputStream, response.getOutputStream());
        }
    }

    @RequestMapping(value = "/select-gear", method = RequestMethod.POST)
    public String selectGear(long gearId, long scorecardId) {

        Gear gear = gearService.getGearById(gearId);
        for(Goal goal : gear.getGoals()){
            for(Outcome outcome: goal.getOutcomes()){
                Output output = new Output();
                output.setOutcome(outcome);
                output.setScorecard(scorecardService.getScorecardById(scorecardId));
                output = outputService.saveOutput(output);

//                Target target = new Target();
//                target.setOutput(output);
//                target = targetService.saveTarget(target);
            }
        }

        return "redirect:/scorecards/capture-targets/"+ scorecardId;
    }


    @RequestMapping(value = "/save-output-target", method = RequestMethod.POST)
    public String saveOutputTarget(OutputWrapper wrapper) {

              Output output = new Output();
              output.setScorecard(scorecardService.getScorecardById(wrapper.getScorecardId()));
              output.setName(wrapper.getName());
              output.setOutcome(outcomeService.getOutcomeById(wrapper.getOutcomeId()));
              output.setWeight(wrapper.getAllocatedWeight());
              output = outputService.saveOutput(output);

              Target target = new Target();
              target.setMeasure(wrapper.getMeasure());
              target.setNormalTarget(wrapper.getNormalTarget());
              target.setBaseTarget(wrapper.getBaseTarget());
              target.setOutput(output);
              target.setAllocatedWeight(wrapper.getAllocatedWeight());
              target.setUnit(wrapper.getUnit());
              target = targetService.saveTarget(target);

        return "redirect:/scorecards/capture-targets/"+ wrapper.getScorecardId();
    }

}
