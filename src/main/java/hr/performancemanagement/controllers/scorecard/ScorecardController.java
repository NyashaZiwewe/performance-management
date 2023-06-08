package hr.performancemanagement.controllers.scorecard;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.ScoreCardRepository;
import hr.performancemanagement.service.*;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Client;
import hr.performancemanagement.utils.constants.Pages;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

@Controller
@RequestMapping(value="/scorecards")
public class ScorecardController {

    @Autowired
    private final ScoreCardRepository scoreCardRepository;
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
    private final StrategicObjectiveService strategicObjectiveService;
    @Autowired
    private final CommentService commentService;
    @Autowired
    private final Mailservice mailservice;
    @Autowired
    private final ApprovalService approvalService;
    @Autowired
    HttpSession session;


    public ScorecardController(ScoreCardRepository scoreCardRepository, ReportingPeriodService reportingPeriodService, AccountService accountService, ScorecardService scorecardService, PerspectiveService perspectiveService, GoalService goalService, StrategicObjectiveService strategicObjectiveService, CommentService commentService, Mailservice mailservice, ApprovalService approvalService) {
        this.scoreCardRepository = scoreCardRepository;
        this.reportingPeriodService = reportingPeriodService;
        this.accountService = accountService;
        this.scorecardService = scorecardService;
        this.perspectiveService = perspectiveService;
        this.goalService = goalService;
        this.strategicObjectiveService = strategicObjectiveService;
        this.commentService = commentService;
        this.mailservice = mailservice;
        this.approvalService = approvalService;
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request, HttpSession session) {

        List<ReportingPeriod> REPORTING_PERIODS_LIST = reportingPeriodService.listAllReportingPeriods();
        List<Account> ACCOUNTS_LIST = accountService.listAllAccounts();
        List<Perspective> PERSPECTIVES_LIST = perspectiveService.listAllPerspectives((Long) session.getAttribute("clientId"));
//        Account loggedUser = (Account) session.getAttribute("loggedUser");

        modelAndView.addObject("pageDomain", "Performance");
        modelAndView.addObject("pageName", "Scorecards");
        modelAndView.addObject("reportingPeriodsList", REPORTING_PERIODS_LIST);
        modelAndView.addObject("accountsList", ACCOUNTS_LIST);
        modelAndView.addObject("perspectivesList", PERSPECTIVES_LIST);
//        modelAndView.addObject("profile", loggedUser.getRole());
        PortletUtils.addMessagesToPage(modelAndView, request);

    }

    @RequestMapping
    public ModelAndView viewScorecards(HttpServletRequest request, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_SCORECARDS);
        modelAndView.addObject("pageTitle", "View Scorecards");
        ReportingPeriod reportingPeriod = reportingPeriodService.getActiveReportingPeriod();
        List<Scorecard> scorecards = scorecardService.getScorecardsByReportingPeriodId(reportingPeriod);
        Account loggedUser = (Account) session.getAttribute("loggedUser");
        long loggedUserId = loggedUser.getId();
        String role = loggedUser.getRole();

        modelAndView.addObject("scorecards", scorecards);
        modelAndView.addObject("loggedUserId", loggedUserId);
        modelAndView.addObject("role", role);
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping("/add-scorecard")
    public ModelAndView addAScorecard(HttpServletRequest request, HttpSession session) {

        ModelAndView modelAndView = new ModelAndView(Pages.ADD_SCORECARD);
        modelAndView.addObject("pageTitle", "New Scorecard");
        modelAndView.addObject("scorecard", new Scorecard());
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping(value = "/save-scorecard", method = RequestMethod.POST)
    public String saveScorecard(HttpServletRequest request, Scorecard newScorecard) throws UnsupportedEncodingException {

        if(scorecardService.countActiveScorecards(newScorecard.getOwner(), newScorecard.getReportingPeriod()) >= 1){
            PortletUtils.addErrorMsg(newScorecard.getOwner().getFullName() + " already has an active scorecard for the selected reporting period (" + newScorecard.getReportingPeriod().getStartDate() +" - "+ newScorecard.getReportingPeriod().getEndDate() +")", request);
            return "redirect:/scorecards/add-scorecard";
        }else {
            newScorecard.setClientId(Client.CLIENT_ID);
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
            mailservice.sendEmail(recipient, subject, template);

            PortletUtils.addInfoMsg("Scorecard successfully created. You can proceed with capturing targets", request);
            return "redirect:/scorecards/capture-targets/"+ newScorecard.getId();
        }
    }

    @RequestMapping("/capture-targets/{id}")
    public ModelAndView captureTargets(@PathVariable("id") long id, HttpServletRequest request, HttpSession session) {

        Scorecard scorecard = scorecardService.getScorecardById(id);
        long reportingPeriodId = scorecard.getReportingPeriod().getId();
        List<Goal> GOALS_LIST = goalService.listAllGoals(id);
        List<StrategicObjective> STRATEGIC_OBJECTIVES_LIST = strategicObjectiveService.listAllStrategicObjectives(reportingPeriodId);
        Account loggedUser = (Account) session.getAttribute("loggedUser");
        long loggedId = loggedUser.getId();
        long ownerId = scorecard.getOwner().getId();
        String approvalStatus = scorecard.getApprovalStatus();
        String status = scorecard.getStatus();
        String lockStatus = scorecard.getLockStatus();
        ModelAndView modelAndView;

        if(loggedId == ownerId && "NEW".equals(approvalStatus) && "ACTIVE".equals(status) && "OPEN".equals(lockStatus)){

            modelAndView = new ModelAndView(Pages.CAPTURE_TARGETS);
            modelAndView.addObject("pageTitle", "Capture Targets");
            modelAndView.addObject("scorecard", scorecard);
            modelAndView.addObject("goalsList", GOALS_LIST);
            modelAndView.addObject("strategicObjectivesList", STRATEGIC_OBJECTIVES_LIST);
            modelAndView.addObject("totalAllocatedWeight", goalService.getTotalAllocatedWeight(id));

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
        Account loggedUser = (Account) session.getAttribute("loggedUser");
        long loggedUserId = loggedUser.getId();
        long ownerId = scorecard.getOwner().getId();
        long supervisorId = scorecard.getOwner().getSupervisor().getId();
        String role = loggedUser.getRole();
        String approvalStatus = scorecard.getApprovalStatus();
        String status = scorecard.getStatus();
        String lockStatus = scorecard.getLockStatus();
        boolean canCapture = false;
        boolean isOwner = false;
        boolean isSupervisor = false;
        boolean isModerator = false;
        String url = "";

        double averageEmployeeScore = goalService.getAverageEmployeeScore(id);
        double averageManagerScore = goalService.getAverageManagerScore(id);
        double averageModeratedScore = goalService.getAverageModeratorScore(id);
        double totalAllocatedWeight = goalService.getTotalAllocatedWeight(id);

        double weightedScore;
        try {
            weightedScore = (averageModeratedScore / 5 ) * 100;
        }catch (Exception e){
            weightedScore = 0;
        }

        if("ACTIVE".equals(status) && "OPEN".equals(lockStatus)) {

            if (loggedUserId == ownerId && "APPROVED_BY_HR".equals(approvalStatus)) {
                canCapture = true;
                isOwner = true;
                url = "submit-scorecard-for-scoring";
            } else if (loggedUserId == supervisorId && "SCORED_BY_EMPLOYEE".equals(approvalStatus)) {
                canCapture = true;
                isSupervisor = true;
                url = "submit-scorecard-for-moderation";
            } else if ("MODERATOR".equals(role) && "SCORED_BY_SUPERVISOR".equals(approvalStatus)) {
                canCapture = true;
                isModerator = true;
                url = "submit-moderated-scorecard";
            }else{
                System.out.println("User can not capture scores");
            }
        }else{

            System.out.println("User can not capture scores");
        }

        ModelAndView modelAndView;

        if(!canCapture){
            modelAndView = new ModelAndView(Pages.BLANK_PAGE);
            PortletUtils.addErrorMsg("You are not allowed to capture scores on this scorecard at this moment", request);
        }else{

            modelAndView = new ModelAndView(Pages.CAPTURE_SCORES);
            modelAndView.addObject("pageTitle", "Capture Scores");
            modelAndView.addObject("scorecard", scorecard);
            modelAndView.addObject("isOwner", isOwner);
            modelAndView.addObject("isSupervisor", isSupervisor);
            modelAndView.addObject("isModerator", isModerator);
            modelAndView.addObject("url", url);
            List<Goal> GOALS_LIST = goalService.listAllGoals(id);
            modelAndView.addObject("goalsList", GOALS_LIST);
            modelAndView.addObject("averageEmployeeScore", averageEmployeeScore);
            modelAndView.addObject("averageManagerScore", averageManagerScore);
            modelAndView.addObject("averageModeratedScore", averageModeratedScore);
            modelAndView.addObject("weightedScore", weightedScore);
            modelAndView.addObject("totalAllocatedWeight", totalAllocatedWeight);
        }
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping(value = "/save-target", method = RequestMethod.POST)
    public String saveTarget(HttpServletRequest request, Goal goal) {

        long scorecardId = goal.getScorecardId();

        goalService.saveGoal(goal);
        return "redirect:/scorecards/capture-targets/"+ scorecardId;
    }

    @RequestMapping(value = "/delete-target", method = RequestMethod.POST)
    public String deleteTarget(HttpServletRequest request, Goal goal) {

        long scorecardId = goal.getScorecardId();
        goalService.deleteGoal(goal);
        PortletUtils.addInfoMsg("Goal was successfully deleted", request);
        return "redirect:/scorecards/capture-targets/"+ scorecardId;
    }

    @RequestMapping(value = "/submit-scorecard-for-approval", method = RequestMethod.POST)
    public String submitScorecardForApproval(HttpServletRequest request, Scorecard updatedScorecard) throws UnsupportedEncodingException {

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());

        scorecard.setApprovalStatus("PENDING_APPROVAL");
        scorecard.setLockStatus("LOCKED");
        scorecardService.saveScorecard(scorecard);

//        String recipient = scorecard.getOwner().getSupervisor().getEmail();
        String recipient = "ziwewend@gmail.com";
        String subject = "Scorecard Approval,";
        String template = "Good day, \n\n"
                + "Please note that "+ scorecard.getOwner().getFullName() +" has submitted his/her scorecard for your approval. "
                + "You can now login and approve\n\n"
                + "Best regards,\n"
                + "The ZimTrade Team";
        mailservice.sendEmail(recipient, subject, template);

        PortletUtils.addInfoMsg("Scorecard successfully submitted for approval. An email was sent to your supervisor", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping(value = "/submit-scorecard-for-scoring", method = RequestMethod.POST)
    public String submitScorecardForScoring(HttpServletRequest request, Scorecard updatedScorecard) throws UnsupportedEncodingException {

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());

        scorecard.setApprovalStatus("SCORED_BY_EMPLOYEE");
        scorecardService.saveScorecard(scorecard);
        Account supervisor = scorecard.getOwner().getSupervisor();

//        String recipient = supervisor.getEmail();
        String recipient = "ziwewend@gmail.com";
        String subject = "Scorecard Scoring,";
        String template = "Good day, \n\n"
                + "Please note that "+ scorecard.getOwner().getFullName() +" has submitted his/her scorecard for scoring by supervisor. "
                + "You can now login and add your scores\n\n"
                + "Best regards,\n"
                + "The ZimTrade Team";
        mailservice.sendEmail(recipient, subject, template);

        PortletUtils.addInfoMsg("Scorecard successfully submitted for scoring by supervisor. An email was sent to "+ supervisor.getFullName(), request);
        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping(value = "/submit-scorecard-for-moderation", method = RequestMethod.POST)
    public String submitScorecardForModeration(HttpServletRequest request, Scorecard updatedScorecard) throws UnsupportedEncodingException {

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());

        scorecard.setApprovalStatus("SCORED_BY_SUPERVISOR");
        scorecardService.saveScorecard(scorecard);
        Account supervisor = scorecard.getOwner().getSupervisor();
        Account owner = scorecard.getOwner();

//        String recipient = hr;
        String recipient = "ziwewend@gmail.com";
        String subject = "Scorecard Scoring,";
        String template = "Good day, \n\n"
                + "Please note that "+ supervisor.getFullName() +" has submitted "+ owner.getFullName()+"'s scorecard for moderation by HR. "
                + "You can now login and add your scores\n\n"
                + "Best regards,\n"
                + "The ZimTrade Team";
        mailservice.sendEmail(recipient, subject, template);

        PortletUtils.addInfoMsg("Scorecard successfully submitted for moderation by HR. An email was sent to them", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping(value = "/submit-moderated-scorecard", method = RequestMethod.POST)
    public String submitModeratedScorecard(HttpServletRequest request, Scorecard updatedScorecard) throws UnsupportedEncodingException {

        Scorecard scorecard = scorecardService.getScorecardById(updatedScorecard.getId());
        scorecard.setApprovalStatus("MODERATED_BY_HR");
        Account loggedUser = (Account) session.getAttribute("loggedUser");
        Account supervisor = scorecard.getOwner().getSupervisor();
        Account owner = scorecard.getOwner();

        try {
            scorecardService.saveScorecard(scorecard);

//        String recipient = owner.getEmail();
            String recipient = "ziwewend@gmail.com";
            String subject = "Scorecard Moderation,";
            String template = "Good day, \n\n"
                    + "Please note that " + supervisor.getFullName() + " has moderated your scorecard. "
                    + "You can now login and see results\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";

          //  String recipient2 = supervisor.getEmail();
            String recipient2 = "ziwewend@gmail.com";
            String subject2 = "Scorecard Moderation,";
            String template2 = "Good day, \n\n"
                    + "Please note that " + supervisor.getFullName() + " has moderated " + owner.getFullName() + "'s scorecard. "
                    + "You can now login and see results\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";

            mailservice.sendEmail(recipient, subject, template);
            mailservice.sendEmail(recipient2, subject2, template2);
            PortletUtils.addInfoMsg("Scorecard successfully moderated by HR. Emails were sent to "+ owner.getFullName()+" and "+ supervisor.getFullName(), request);

        }catch (Exception e){

            //        String recipient = "amkwazhe@zimtrade.co.zw";
            String recipient = "ziwewend@gmail.com";
            String subject = "Scorecard Moderation,";
            String template = "Good day, \n\n"
                    + "Please note that " + loggedUser.getFullName() + " has failed to submit a moderated scorecard for" + owner.getFullName() + ". "
                    + "Kindly assist\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            mailservice.sendEmail(recipient, subject, template);
            PortletUtils.addErrorMsg("Scorecard wasn't submitted. An Email was sent to the administrator", request);
        }

        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping(value = "/supervisor-approve", method = RequestMethod.POST)
    public String supervisorApproveScorecard(HttpServletRequest request, Long id) throws UnsupportedEncodingException {

        Scorecard scorecard = scorecardService.getScorecardById(id);
        String supervisor = scorecard.getOwner().getSupervisor().getFullName();
        String owner = scorecard.getOwner().getFullName();
//        String recipient = scorecard.getOwner().getEmail();
        String recipient = "ziwewend@gmail.com";
        scorecard.setApprovalStatus("APPROVED_BY_SUPERVISOR");

        try {
            scorecardService.saveScorecard(scorecard);
            try {
                Account loggedUser = (Account) session.getAttribute("loggedUser");
                Approval approval = new Approval();
                approval.setScorecard(scorecard);
                approval.setAccount(loggedUser);
                approval.setStatus("APPROVED_BY_SUPERVISOR");
                approvalService.addApproval(approval);
            }catch (Exception e){
                e.printStackTrace();
            }

            String subject = "Scorecard Approval,";
            String template = "Good day "+ owner + ", \n\n"
                            + "Please note that "+ supervisor +" has approved your scorecard. "
                            + "We are now waiting for HR to approve so that you can proceed with capturing scores. \n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            mailservice.sendEmail(recipient, subject, template);


            String recipient2 = "ziwewend@gmail.com";
            String subject2 = "Scorecard Approval,";
            String template2 = "Good day HR, \n\n"
                            + "Please note that "+ supervisor +" has approved "+owner+"'s scorecard. "
                            + "You are now eligible to review and approve so that they can proceed with capturing scores. \n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            mailservice.sendEmail(recipient2, subject2, template2);


            PortletUtils.addInfoMsg("Scorecard successfully approved. An email was sent to HR for further approval and to "+ owner + " as feedback", request);
            return "redirect:/scorecards/view-scorecard/"+ id;
        }catch (Exception e){

            String admin = "ziwewend@gmail.com";
            String subject  = "Technical failure,";
            String template = "Good day, \n\n"
                            + "Please note that "+ supervisor +"  failed to approve "+ owner +"'s scorecard. "
                            + "With error :. \n\n"
                            + e.getMessage() +"\n\n"
                            + "Best regards,\n"
                            + "The ZimTrade Team";
            mailservice.sendEmail(admin, subject, template);
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
        scorecard.setApprovalStatus("REJECTED_BY_SUPERVISOR");
        scorecard.setStatus("OPEN");

        try{
            scorecardService.saveScorecard(scorecard);

            try {
                Account loggedUser = (Account) session.getAttribute("loggedUser");
                Approval approval = new Approval();
                approval.setScorecard(scorecard);
                approval.setAccount(loggedUser);
                approval.setMessage(message);
                approval.setStatus("REJECTED_BY_SUPERVISOR");
                approvalService.addApproval(approval);
            }catch (Exception e){
                e.printStackTrace();
            }

            String subject = "Scorecard Approval,";
            String template = "Good day "+ owner + ", \n\n"
                    + "Please note that "+ supervisor +" has rejected your scorecard with the following message \n\n. "
                    +".........................................................................................\n"
                    + message + "\n"
                    +".........................................................................................\n\n"
                    + "Please log in and make recommended changes. Also look for comments and flags on your scorecard and rectify\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            mailservice.sendEmail(recipient, subject, template);

            PortletUtils.addInfoMsg("Scorecard successfully rejected. An email was sent to "+ owner + " as feedback", request);

        }catch (Exception e){

            String admin = "ziwewend@gmail.com";
            String subject  = "Technical failure,";
            String template = "Good day, \n\n"
                    + "Please note that "+ supervisor +"  failed to approve "+ owner +"'s scorecard. "
                    + "With error :. \n\n"
                    + e.getMessage() +"\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            mailservice.sendEmail(admin, subject, template);
            PortletUtils.addInfoMsg("Scorecard approval failed. An email was sent to the administrator with error details. ", request);
        }

        return "redirect:/scorecards/view-scorecard/"+ id;
    }

    @RequestMapping(value = "/hr-approve", method = RequestMethod.POST)
    public String hrApproveScorecard(HttpServletRequest request, Long id) throws UnsupportedEncodingException {

        Scorecard scorecard = scorecardService.getScorecardById(id);
        String supervisor = scorecard.getOwner().getSupervisor().getFullName();
        String owner = scorecard.getOwner().getFullName();
//        String recipient = scorecard.getOwner().getSupervisor().getEmail();
        String recipient = "ziwewend@gmail.com";
        Account loggedUser = (Account) session.getAttribute("loggedUser");
        scorecard.setApprovalStatus("APPROVED_BY_HR");
        scorecard.setLockStatus("OPEN");

        try {
            scorecardService.saveScorecard(scorecard);
            try {
                Approval approval = new Approval();
                approval.setScorecard(scorecard);
                approval.setAccount(loggedUser);
                approval.setStatus("APPROVED_BY_HR");
                approvalService.addApproval(approval);
            }catch (Exception e){
                e.printStackTrace();
            }

            String subject = "Scorecard Approval,";
            String template = "Good day "+ supervisor + ", \n\n"
                    + "Please note that "+ loggedUser.getFullName() +" has approved "+ owner +" scorecard. "
                    + "The owner is now eligible for capturing scores. \n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            mailservice.sendEmail(recipient, subject, template);


//            String recipient2 = scorecard.getOwner().getEmail();
            String recipient2 = "ziwewend@gmail.com";
            String subject2 = "Scorecard Approval,";
            String template2 = "Good day "+owner+", \n\n"
                    + "Please note that "+ loggedUser.getFullName() +" has your scorecard. "
                    + "You are now eligible to capture scores. \n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            mailservice.sendEmail(recipient2, subject2, template2);


            PortletUtils.addInfoMsg("Scorecard successfully approved. An email was sent to "+supervisor+" and to "+ owner + " as feedback", request);
            return "redirect:/scorecards/view-scorecard/"+ id;
        }catch (Exception e){

            String admin = "ziwewend@gmail.com";
            String subject  = "Technical failure,";
            String template = "Good day, \n\n"
                    + "Please note that "+ loggedUser.getFullName() +"  failed to approve "+ owner +"'s scorecard. "
                    + "With error :. \n\n"
                    + e.getMessage() +"\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            mailservice.sendEmail(admin, subject, template);
            PortletUtils.addInfoMsg("Scorecard approval failed. An email was sent to the administrator with error details. ", request);
            return "redirect:/scorecards/view-scorecard/"+ id;
        }
    }

    @RequestMapping(value = "/hr-reject", method = RequestMethod.POST)
    public String hrRejectScorecard(HttpServletRequest request, Long id, String message) throws UnsupportedEncodingException {

        Scorecard scorecard = scorecardService.getScorecardById(id);
        String supervisor = scorecard.getOwner().getSupervisor().getFullName();
        String owner = scorecard.getOwner().getFullName();
//        String recipient = scorecard.getOwner().getSupervisor().getEmail();
        String recipient = "ziwewend@gmail.com";
        scorecard.setApprovalStatus("REJECTED_BY_HR");
        Account loggedUser = (Account) session.getAttribute("loggedUser");

        try{
            scorecardService.saveScorecard(scorecard);

            try {

                Approval approval = new Approval();
                approval.setScorecard(scorecard);
                approval.setAccount(loggedUser);
                approval.setMessage(message);
                approval.setStatus("REJECTED_BY_HR");
                approvalService.addApproval(approval);
            }catch (Exception e){
              e.printStackTrace();
            }

            String subject = "Scorecard Approval,";
            String template = "Good day "+ supervisor + ", \n\n"
                    + "Please note that "+ loggedUser.getFullName() +" has rejected "+ owner+"'s scorecard with the following message \n\n. "
                    +".........................................................................................\n"
                    + message + "\n"
                    +".........................................................................................\n\n"
                    + "Please log in and make recommended changes. Also look for comments and flags on your scorecard and rectify\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            mailservice.sendEmail(recipient, subject, template);

            PortletUtils.addInfoMsg("Scorecard successfully rejected. An email was sent to "+ supervisor + " as feedback", request);

        }catch (Exception e){

            String admin = "ziwewend@gmail.com";
            String subject  = "Technical failure,";
            String template = "Good day, \n\n"
                    + "Please note that "+ loggedUser.getFullName() +"  failed to reject "+ owner +"'s scorecard. "
                    + "With error :. \n\n"
                    + e.getMessage() +"\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            mailservice.sendEmail(admin, subject, template);
            PortletUtils.addInfoMsg("Scorecard approval failed. An email was sent to the administrator with error details. ", request);
        }

        return "redirect:/scorecards/view-scorecard/"+ id;
    }

    @RequestMapping("/view-scorecard/{id}")
    public ModelAndView viewScorecard(@PathVariable("id") long id, HttpServletRequest request, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_SCORECARD);
        List<Goal> GOALS_LIST = goalService.listAllGoals(id);
        double averageEmployeeScore = goalService.getAverageEmployeeScore(id);
        double averageManagerScore = goalService.getAverageManagerScore(id);
        double averageModeratedScore = goalService.getAverageModeratorScore(id);
        double totalAllocatedWeight = goalService.getTotalAllocatedWeight(id);
        double weightedScore;
        try {
             weightedScore = (averageModeratedScore / 5 ) * 100;
        }catch (Exception e){
             weightedScore = 0;
        }

        Scorecard scorecard = scorecardService.getScorecardById(id);
        modelAndView.addObject("pageTitle", "View Scorecard {"+ scorecard.getOwner().getFullName() +"}");
        modelAndView.addObject("scorecard", scorecard);
        modelAndView.addObject("goalsList", GOALS_LIST);
        modelAndView.addObject("comment", new Comment());
        modelAndView.addObject("averageEmployeeScore", averageEmployeeScore);
        modelAndView.addObject("averageManagerScore", averageManagerScore);
        modelAndView.addObject("averageModeratedScore", averageModeratedScore);
        modelAndView.addObject("totalAllocatedWeight", totalAllocatedWeight);
        modelAndView.addObject("weightedScore", weightedScore);
        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping(value = "/save-comment", method = RequestMethod.POST)
    public String saveComment(HttpServletRequest request, Comment comment, @RequestParam("scorecardId") long scorecardId) {

        comment.setSender(accountService.getAccountById(1));
        commentService.saveComment(comment);
        PortletUtils.addInfoMsg("Comment successfully saved", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecardId;
    }

    @RequestMapping(value = "/save-flag", method = RequestMethod.POST)
    public String saveFlag(HttpServletRequest request, Goal updatedGoal) {

        Goal goal = goalService.getGoalById(updatedGoal.getId());

        goal.setFlag(updatedGoal.getFlag());
        goalService.saveGoal(goal);
        PortletUtils.addInfoMsg("Goal successfully flagged and the reason was saved", request);
        return "redirect:/scorecards/view-scorecard/"+ updatedGoal.getScorecardId();
    }

    @RequestMapping(value = "/save-score", method = RequestMethod.POST, consumes = {"*/*"})
    public void saveScore(HttpServletRequest request, HttpServletResponse response, Long id, Double employeeScore, Double managerScore, Double actualScore, String supportingDocument, String justification) throws IOException {

        String jsonString  = null;
        String result = null;
        JSONObject jsonObject = new JSONObject();

        System.out.println("goal id is: " +id);
        System.out.println("supporting document is: " +supportingDocument);
        System.out.println("supporting document name is: " + String.valueOf(supportingDocument));
        String fileName = String.valueOf(supportingDocument);

        System.out.println("file name is: " +fileName);


        //supportingDocument.transferTo( new File("/Users/nyashaziwewe/Desktop/supportingdocuments" + fileName));

        Goal goal = goalService.getGoalById(id);
        goal.setEmployeeScore(employeeScore);
        goal.setManagerScore(managerScore);
        goal.setActualScore(actualScore);
        goal.setSupportingDocument(fileName);
        goal.setJustification(justification);
        goalService.saveGoal(goal);

        jsonObject.put("employeeScore",employeeScore);
        jsonObject.put("id",id);
        jsonObject.put("managerScore",managerScore);
        jsonObject.put("supportingDocument",fileName);
        jsonObject.put("justification",justification);

        jsonString = jsonObject.toString();

        try(OutputStream outputStream = response.getOutputStream()){
            StringBuilder builder = new StringBuilder();
            builder.append(jsonString);
            outputStream.write(builder.toString().getBytes());

        }catch (IOException e){
            throw  new RuntimeException();
        }
    }

    @RequestMapping(value = "/fake-save-scorecard", method = RequestMethod.POST)
    public String fakeSaveScorecard(HttpServletRequest request, Scorecard scorecard) {
        PortletUtils.addInfoMsg("Scorecard successfully saved for later. You can update it anytime before submitting for approval", request);
        return "redirect:/scorecards/view-scorecard/"+ scorecard.getId();
    }

    @RequestMapping("/clone-scorecard/{id}")
    public ModelAndView cloneScorecard(@PathVariable("id") long id, HttpServletRequest request, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView(Pages.CLONE_SCORECARD);
        List<Goal> GOALS_LIST = goalService.listAllGoals(id);
        modelAndView.addObject("pageTitle", "Clone Scorecard");
        Scorecard scorecard = scorecardService.getScorecardById(id);
        modelAndView.addObject("scorecard", scorecard);

        preparePage(modelAndView, request, session);
        return modelAndView;
    }

    @RequestMapping(value = "/copy-scorecard", method = RequestMethod.POST)
    public String copyScorecard(HttpServletRequest request,Scorecard imaginaryScorecard) throws UnsupportedEncodingException {

        if (scorecardService.countActiveScorecards(imaginaryScorecard.getOwner(), imaginaryScorecard.getReportingPeriod()) >= 1) {
            PortletUtils.addErrorMsg(imaginaryScorecard.getOwner().getFullName() + " already has an active scorecard for the selected reporting period (" + imaginaryScorecard.getReportingPeriod().getStartDate() + " - " + imaginaryScorecard.getReportingPeriod().getEndDate() + ")", request);
            return "redirect:/scorecards/clone-scorecard/"+ imaginaryScorecard.getId();
        } else {

            Scorecard scorecard = scorecardService.getScorecardById(imaginaryScorecard.getId());
            Scorecard newScorecard = new Scorecard();

            newScorecard.setClientId(scorecard.getClientId());
            newScorecard.setOwner(imaginaryScorecard.getOwner());
            newScorecard.setReportingPeriod(imaginaryScorecard.getReportingPeriod());
            newScorecard.setStatus("ACTIVE");
            newScorecard.setApprovalStatus("NEW");
            newScorecard.setLockStatus("OPEN");

            scorecardService.saveScorecard(newScorecard);
            long id = newScorecard.getId();
            List<Goal> goalList = goalService.listAllGoals(scorecard.getId());

            for (Goal goal : goalList) {
                Goal newGoal = new Goal();

                newGoal.setScorecardId(id);
                newGoal.setPerspective(goal.getPerspective());
                newGoal.setStrategicObjective(goal.getStrategicObjective());
                newGoal.setName(goal.getName());
                newGoal.setMeasure(goal.getMeasure());
                newGoal.setUnit(goal.getUnit());
                newGoal.setAllocatedWeight(goal.getAllocatedWeight());
                newGoal.setTarget(goal.getTarget());

                goalService.saveGoal(newGoal);


            }
            String recipient = imaginaryScorecard.getOwner().getEmail();
            String subject = "Scorecard Creation,";
            String template = "Good day, \n\n"
                    + "We are pleased to notify you that your scorecard has been cloned and is already populated with targets. "
                    + "You can now login and modify targets to match your performance goals\n\n"
                    + "Best regards,\n"
                    + "The ZimTrade Team";
            mailservice.sendEmail(recipient, subject, template);
            return "redirect:/scorecards/view-scorecard/" + id;
        }
    }

}
