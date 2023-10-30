package hr.performancemanagement.controllers.performanceimprovementplan;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.PerformanceImprovementPlanRepository;
import hr.performancemanagement.service.*;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Client;
import hr.performancemanagement.utils.constants.Pages;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Controller
@RequestMapping(value = "/performance-improvement-plans")
public class performanceImprovementPlanController {

    @Autowired
    private final PerformanceImprovementPlanRepository performanceImprovementPlanRepository;
    @Autowired
    private final AccountService accountService;
    @Autowired
    private final PerformanceImprovementPlanService performanceImprovementPlanService;
    @Autowired
    private final ReportingPeriodService reportingPeriodService;
    @Autowired
    private final PIPTaskService pipTaskService;
    @Autowired
    private final PIPIssueService pipIssueService;
    @Autowired
    private final PIPNoteService pipNoteService;
    @Autowired
    private final CommonService commonService;

    public performanceImprovementPlanController(PerformanceImprovementPlanRepository performanceImprovementPlanRepository, AccountService accountService, PerformanceImprovementPlanService performanceImprovementPlanService, ReportingPeriodService reportingPeriodService, PIPTaskService pipTaskService, PIPIssueService pipIssueService, PIPNoteService pipNoteService, CommonService commonService) {
        this.performanceImprovementPlanRepository = performanceImprovementPlanRepository;
        this.accountService = accountService;
        this.performanceImprovementPlanService = performanceImprovementPlanService;
        this.reportingPeriodService = reportingPeriodService;
        this.pipTaskService = pipTaskService;
        this.pipIssueService = pipIssueService;
        this.pipNoteService = pipNoteService;
        this.commonService = commonService;
    }

    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {

        List<Account> ACCOUNTS_LIST = accountService.listAllAccounts();
        List<ReportingPeriod> REPORTING_PERIODS_LIST = reportingPeriodService.listAllReportingPeriods();

        modelAndView.addObject("pageDomain", "Performance");
        modelAndView.addObject("pageName", "Performance Improvement Plans");
        modelAndView.addObject("accountsList", ACCOUNTS_LIST);
        modelAndView.addObject("reportingPeriodsList", REPORTING_PERIODS_LIST);
        PortletUtils.addMessagesToPage(modelAndView, request);

    }

//    @RequestMapping
//    public ModelAndView viewPerformanceImprovementPlans(HttpServletRequest request) {
//        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PERFORMANCE_IMPROVEMENT_PLANS);
//        modelAndView.addObject("pageTitle", "View");
//        List<PerformanceImprovementPlan> plansList = performanceImprovementPlanService.listAllPerformanceImprovementPlans();
//        modelAndView.addObject("plansList", plansList);
//        modelAndView.addObject("performanceImprovementPlan", new PerformanceImprovementPlan());
//        preparePage(modelAndView, request);
//        return modelAndView;
//    }

    @RequestMapping
    public ModelAndView viewPerformanceImprovementPlans2(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PERFORMANCE_IMPROVEMENT_PLANS2);
        modelAndView.addObject("pageTitle", "View All");
        Account loggedUser = commonService.getLoggedUser();
        ReportingPeriod reportingPeriod = reportingPeriodService.getActiveReportingPeriod();
        List<PerformanceImprovementPlan> plansList = performanceImprovementPlanService.listAllPerformanceImprovementPlans(reportingPeriod);
        for(PerformanceImprovementPlan plan : plansList){
            String initials = commonService.getInitials(plan.getEmployee().getFullName());
            plan.getEmployee().setInitials(initials);
        }
        modelAndView.addObject("plansList", plansList);
        modelAndView.addObject("performanceImprovementPlan", new PerformanceImprovementPlan());
        modelAndView.addObject("loggedUser", loggedUser);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping (value = "/view-user-performance-improvement-plans/{userId}")
    public ModelAndView viewPerformanceImprovementPlans(@PathVariable("userId") Long userId, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PERFORMANCE_IMPROVEMENT_PLANS);
        modelAndView.addObject("pageTitle", "View");
        Account employee = accountService.getAccountById(userId);
        List<PerformanceImprovementPlan> plansList = performanceImprovementPlanService.listAllPerformanceImprovementPlansByEmployee(employee);
        modelAndView.addObject("plansList", plansList);
        modelAndView.addObject("performanceImprovementPlan", new PerformanceImprovementPlan());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/view-plan/{id}")
    public ModelAndView viewPerformanceImprovementPlan(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_PERFORMANCE_IMPROVEMENT_PLAN);
        List<PIPTask> TASKS_LIST = pipTaskService.listAllPIPTasks(id);
        List<PIPIssue> ISSUES_LIST = pipIssueService.listAllPIPIssues(id);
        List<PIPNote> NOTES_LIST = pipNoteService.listAllPIPNotes(id);

        modelAndView.addObject("pageTitle", "Action Plan");
        PerformanceImprovementPlan performanceImprovementPlan = performanceImprovementPlanService.getPerformanceImprovementPlanById(id);
        modelAndView.addObject("performanceImprovementPlan", performanceImprovementPlan);
        modelAndView.addObject("tasksList", TASKS_LIST);
        modelAndView.addObject("issuesList", ISSUES_LIST);
        modelAndView.addObject("notesList", NOTES_LIST);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/add-plan")
    public ModelAndView addPlan(HttpServletRequest request) {

        ModelAndView modelAndView = new ModelAndView(Pages.ADD_PERFORMANCE_IMPROVEMENT_PLAN);
        modelAndView.addObject("pageTitle", "New Plan");
        modelAndView.addObject("performanceImprovementPlan", new PerformanceImprovementPlan());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-plan", method = RequestMethod.POST)
    public String savePlan(HttpServletRequest request, PerformanceImprovementPlan performanceImprovementPlan) {

        performanceImprovementPlan.setStatus("NEW");
        performanceImprovementPlan.setClientId(Client.CLIENT_ID);
        performanceImprovementPlanService.savePerformanceImprovementPlan(performanceImprovementPlan);
        PortletUtils.addInfoMsg("Performance Improvement Plan successfully created.", request);
        return "redirect:/performance-improvement-plans/add-plan/";
    }

    @RequestMapping(value = "/save-performance-improvement-plan", method = RequestMethod.POST)
    public String saveNewPerformanceImprovementPlan(HttpServletRequest request, String plan) {
        Account loggedUser = commonService.getLoggedUser();
        ReportingPeriod reportingPeriod = reportingPeriodService.getActiveReportingPeriod();
        PerformanceImprovementPlan performanceImprovementPlan = new PerformanceImprovementPlan();
        performanceImprovementPlan.setStatus("todo");
        performanceImprovementPlan.setClientId(Client.CLIENT_ID);
        performanceImprovementPlan.setAgreedAction(plan);
        performanceImprovementPlan.setEmployee(loggedUser);
        performanceImprovementPlan.setReportingPeriod(reportingPeriod);
        System.out.println(performanceImprovementPlan);
        performanceImprovementPlanService.addPerformanceImprovementPlan(performanceImprovementPlan);
        PortletUtils.addInfoMsg("PerformanceImprovementPlan Plan successfully created.", request);
        return "redirect:/performance-improvement-plans/view-performance-improvement-plans-2";
    }

    @RequestMapping(value = "/update-plan", method = RequestMethod.POST)
    public String updatePlan(HttpServletRequest request, PerformanceImprovementPlan newPlan) {
        PerformanceImprovementPlan plan = performanceImprovementPlanService.getPerformanceImprovementPlanById(newPlan.getId());
        plan.setEmployee(newPlan.getEmployee());
        plan.setAgreedAction(newPlan.getAgreedAction());
        plan.setConcern(newPlan.getConcern());
        plan.setEndDate(newPlan.getEndDate());
        plan.setExpectedStandard(newPlan.getExpectedStandard());
        plan.setProgress(newPlan.getProgress());
        plan.setRequiredSupport(newPlan.getRequiredSupport());
        plan.setReviewNotes(newPlan.getReviewNotes());
        plan.setTargetArea(newPlan.getTargetArea());

        performanceImprovementPlanService.addPerformanceImprovementPlan(plan);
        PortletUtils.addInfoMsg("Performance Improvement Plan successfully updated.", request);
        return "redirect:/performance-improvement-plans";
    }

    @RequestMapping(value = "/delete-plan", method = RequestMethod.POST)
    public String deletePlan(HttpServletRequest request, PerformanceImprovementPlan performanceImprovementPlan) {

        performanceImprovementPlanService.deletePerformanceImprovementPlan(performanceImprovementPlan);
        PortletUtils.addInfoMsg("Performance Improvement plan was successfully deleted", request);
        return "redirect:/performance-improvement-plans";
    }

    @RequestMapping(value = "/save-task", method = RequestMethod.POST)
    public String saveTask(HttpServletRequest request, long performanceImprovementPlanId, String task) {

        PIPTask newTask = new PIPTask();
        newTask.setPerformanceImprovementPlan(performanceImprovementPlanService.getPerformanceImprovementPlanById(performanceImprovementPlanId));
        newTask.setName(task);
        newTask.setStatus("OPEN");
        pipTaskService.savePIPTask(newTask);
        PortletUtils.addInfoMsg("Task successfully created.", request);
        return "redirect:/performance-improvement-plans/view-plan/"+ performanceImprovementPlanId;
    }

    @RequestMapping(value = "/save-issue", method = RequestMethod.POST)
    public String saveIssue(HttpServletRequest request, long performanceImprovementPlanId, String issue) {

        PIPIssue newIssue = new PIPIssue();
        newIssue.setPerformanceImprovementPlan(performanceImprovementPlanService.getPerformanceImprovementPlanById(performanceImprovementPlanId));
        newIssue.setName(issue);
        newIssue.setStatus("OPEN");
        pipIssueService.savePIPIssue(newIssue);
        PortletUtils.addInfoMsg("Issue successfully created.", request);
        return "redirect:/performance-improvement-plans/view-plan/"+ performanceImprovementPlanId;
    }

    @RequestMapping(value = "/save-note", method = RequestMethod.POST)
    public String saveNote(HttpServletRequest request, long performanceImprovementPlanId, String note) {

        PIPNote newNote = new PIPNote();
        newNote.setPerformanceImprovementPlan(performanceImprovementPlanService.getPerformanceImprovementPlanById(performanceImprovementPlanId));
        newNote.setComment(note);
        newNote.setStatus("OPEN");
        pipNoteService.savePIPNote(newNote);
        PortletUtils.addInfoMsg("Comment successfully posted.", request);
        return "redirect:/performance-improvement-plans/view-plan/"+ performanceImprovementPlanId;
    }

//    @RequestMapping(value = "/update-task-status", method = RequestMethod.POST)
//    public String updateTaskStatus(HttpServletRequest request, long performanceImprovementPlanId, String taskId) {
//
//        PIPTask task = pipTaskService.getPIPTaskById(Long.parseLong(taskId));
//        if("OPEN".equals(task.getStatus())){
//            task.setStatus("COMPLETED");
//        }else {
//            task.setStatus("OPEN");
//        }
//        pipTaskService.savePIPTask(task);
//        return "redirect:/performance-improvement-plans/view-plan/"+ performanceImprovementPlanId;
//    }
//
//    @RequestMapping(value = "/update-issue-status", method = RequestMethod.POST)
//    public String updateIssueStatus(HttpServletRequest request, long performanceImprovementPlanId, String issueId) {
//
//        PIPIssue issue = pipIssueService.getPIPIssueById(Long.parseLong(issueId));
//        if("OPEN".equals(issue.getStatus())){
//            issue.setStatus("COMPLETED");
//        }else {
//            issue.setStatus("OPEN");
//        }
//        pipIssueService.savePIPIssue(issue);
//        return "redirect:/performance-improvement-plans/view-plan/"+ performanceImprovementPlanId;
//    }


    @RequestMapping(value = "/update-task-status", method = RequestMethod.POST)
    public void updateTaskStatus(HttpServletResponse response, String taskId) {

        PIPTask task = pipTaskService.getPIPTaskById(Long.parseLong(taskId));
        if("OPEN".equals(task.getStatus())){
            task.setStatus("COMPLETED");
        }else {
            task.setStatus("OPEN");
        }
        pipTaskService.savePIPTask(task);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", task.getStatus());

        String jsonString = jsonObject.toString();

        try(OutputStream outputStream = response.getOutputStream()){
            outputStream.write(jsonString.getBytes());

        }catch (IOException e){
            throw  new RuntimeException();
        }
    }

    @RequestMapping(value = "/update-performance-improvement-plan-status", method = RequestMethod.POST)
    public void updatePerformanceImprovementPlanStatus(HttpServletResponse response, long id, String status) {

        PerformanceImprovementPlan plan = performanceImprovementPlanService.getPerformanceImprovementPlanById(id);
        plan.setStatus(status);
        performanceImprovementPlanService.savePerformanceImprovementPlan(plan);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("alreadyExists", false);

        String jsonString = jsonObject.toString();

        try(OutputStream outputStream = response.getOutputStream()){
            outputStream.write(jsonString.getBytes());

        }catch (IOException e){
            throw  new RuntimeException();
        }
    }

    @RequestMapping(value = "/update-issue-status", method = RequestMethod.POST)
    public String updateIssueStatus(HttpServletRequest request, long performanceImprovementPlanId, String issueId) {

        PIPIssue issue = pipIssueService.getPIPIssueById(Long.parseLong(issueId));
        if("OPEN".equals(issue.getStatus())){
            issue.setStatus("COMPLETED");
        }else {
            issue.setStatus("OPEN");
        }
        pipIssueService.savePIPIssue(issue);
        return "redirect:/performance-improvement-plans/view-plan/"+ performanceImprovementPlanId;
    }


}
