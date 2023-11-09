package hr.performancemanagement.controllers.actionplan;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.service.*;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Client;
import hr.performancemanagement.utils.constants.PMConstants;
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
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Controller
@RequestMapping(value = "/action-plans")
public class ActionPlanController {

    @Autowired
    private final ReportingPeriodService reportingPeriodService;
    @Autowired
    private final ActionPlanService actionPlanService;
    @Autowired
    private final AccountService accountService;
    @Autowired
    private final TaskService taskService;
    @Autowired
    private final IssueService issueService;
    @Autowired
    private final NoteService noteService;
    @Autowired
    private final CommonService commonService;
//    @Autowired
//    HttpSession session;

    public ActionPlanController(ReportingPeriodService reportingPeriodService, ActionPlanService actionPlanService, AccountService accountService, TaskService taskService, IssueService issueService, NoteService noteService, CommonService commonService) {
        this.reportingPeriodService = reportingPeriodService;
        this.actionPlanService = actionPlanService;
        this.accountService = accountService;
        this.taskService = taskService;
        this.issueService = issueService;
        this.noteService = noteService;
        this.commonService = commonService;
    }


    private void preparePage(ModelAndView modelAndView, HttpServletRequest request) {

        List<Account> ACCOUNTS_LIST = accountService.listAllAccounts();
        List<ReportingPeriod> REPORTING_PERIODS_LIST = reportingPeriodService.listAllReportingPeriods();

        modelAndView.addObject("pageDomain", "Action Plans");
        modelAndView.addObject("pageName", "Action Plans");
        modelAndView.addObject("accountsList", ACCOUNTS_LIST);
        modelAndView.addObject("reportingPeriodsList", REPORTING_PERIODS_LIST);
        PortletUtils.addMessagesToPage(modelAndView, request);

    }

//    @RequestMapping
//    public ModelAndView viewActionPlans(HttpServletRequest request) {
//        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_ACTION_PLANS);
//        modelAndView.addObject("pageTitle", "View All");
//        ReportingPeriod reportingPeriod = reportingPeriodService.getActiveReportingPeriod();
//        List<ActionPlan> plansList = actionPlanService.listAllActionPlans(reportingPeriod);
//        modelAndView.addObject("plansList", plansList);
//        modelAndView.addObject("actionPlan", new ActionPlan());
//        preparePage(modelAndView, request);
//        return modelAndView;
//    }
    @RequestMapping()
    public ModelAndView viewActionPlans2(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_ACTION_PLANS2);
        modelAndView.addObject("pageTitle", "View All");
        Account loggedUser = commonService.getLoggedUser();
        ReportingPeriod reportingPeriod = reportingPeriodService.getActiveReportingPeriod();
        List<ActionPlan> plansList = actionPlanService.listAllActionPlans(reportingPeriod);
        for(ActionPlan plan : plansList){
            String initials = commonService.getInitials(plan.getManager().getFullName());
            plan.getManager().setInitials(initials);
        }
        modelAndView.addObject("plansList", plansList);
        modelAndView.addObject("actionPlan", new ActionPlan());
        modelAndView.addObject("loggedUser", loggedUser);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/view-user-action-plans/{userId}")
    public ModelAndView viewUserActionPlans(@PathVariable("userId") Long userId, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_USER_ACTION_PLANS);
        Account manager = accountService.getAccountById(userId);
        modelAndView.addObject("pageTitle", "View "+ manager.getFullName() +" \'s Action Plans");
        List<ActionPlan> plansList = actionPlanService.listAllUserActionPlans(manager);
        modelAndView.addObject("plansList", plansList);
        modelAndView.addObject("actionPlan", new ActionPlan());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/view-plan/{id}")
    public ModelAndView viewActionPlan(@PathVariable("id") long id, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_ACTION_PLAN);
        List<Task> TASKS_LIST = taskService.listAllTasks(id);
        List<Issue> ISSUES_LIST = issueService.listAllIssues(id);
        List<Note> NOTES_LIST = noteService.listAllNotes(id);

        modelAndView.addObject("pageTitle", "Action Plan");
        ActionPlan actionPlan = actionPlanService.getActionPlanById(id);
        modelAndView.addObject("actionPlan", actionPlan);
        modelAndView.addObject("tasksList", TASKS_LIST);
        modelAndView.addObject("issuesList", ISSUES_LIST);
        modelAndView.addObject("notesList", NOTES_LIST);
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping("/add-plan")
    public ModelAndView addPlan(HttpServletRequest request) {

        ModelAndView modelAndView = new ModelAndView(Pages.ADD_ACTION_PLAN);
        modelAndView.addObject("pageTitle", "New Plan");
        modelAndView.addObject("actionPlan", new ActionPlan());
        preparePage(modelAndView, request);
        return modelAndView;
    }

    @RequestMapping(value = "/save-plan", method = RequestMethod.POST)
    public String savePlan(HttpServletRequest request, ActionPlan actionPlan) {

        actionPlan.setStatus("ACTIVE");
        actionPlan.setClientId(Client.CLIENT_ID);
        System.out.println(actionPlan);
        actionPlanService.addActionPlan(actionPlan);
        PortletUtils.addInfoMsg("Action Plan successfully created.", request);
        return "redirect:/action-plans/add-plan/";
    }

    @RequestMapping(value = "/save-action-plan", method = RequestMethod.POST)
    public String saveNewActionPlan(HttpServletRequest request, String plan) {
        Account loggedUser = commonService.getLoggedUser();
        ReportingPeriod reportingPeriod = reportingPeriodService.getActiveReportingPeriod();
        ActionPlan actionPlan = new ActionPlan();
        actionPlan.setStatus("todo");
        actionPlan.setClientId(Client.CLIENT_ID);
        actionPlan.setName(plan);
        actionPlan.setManager(loggedUser);
        actionPlan.setReportingPeriod(reportingPeriod);
        System.out.println(actionPlan);
        actionPlanService.addActionPlan(actionPlan);
        PortletUtils.addInfoMsg("Action Plan successfully created.", request);
        return "redirect:/action-plans";
    }

    @RequestMapping(value = "/update-plan", method = RequestMethod.POST)
    public String updatePlan(HttpServletRequest request, ActionPlan newPlan) {

        actionPlanService.saveActionPlan(newPlan);
        PortletUtils.addInfoMsg("Action Plan successfully updated.", request);
        return "redirect:/action-plans";
    }

    @RequestMapping(value = "/delete-plan", method = RequestMethod.POST)
    public String deletePlan(HttpServletRequest request, ActionPlan actionPlan) {

        actionPlanService.deleteActionPlan(actionPlan);
        PortletUtils.addInfoMsg("Action plan was successfully deleted", request);
        return "redirect:/action-plans";
    }

    @RequestMapping(value = "/save-task", method = RequestMethod.POST)
    public String saveTask(HttpServletRequest request, long actionPlanId, String task) {

        Task newTask = new Task();
        newTask.setActionPlan(actionPlanService.getActionPlanById(actionPlanId));
        newTask.setName(task);
        newTask.setStatus(PMConstants.TASK_STATUS_OPEN);
        taskService.saveTask(newTask);
        PortletUtils.addInfoMsg("Task successfully created.", request);
        return "redirect:/action-plans/view-plan/"+ actionPlanId;
    }

    @RequestMapping(value = "/save-issue", method = RequestMethod.POST)
    public String saveIssue(HttpServletRequest request, long actionPlanId, String issue) {

        Issue newIssue = new Issue();
        newIssue.setActionPlan(actionPlanService.getActionPlanById(actionPlanId));
        newIssue.setName(issue);
        newIssue.setStatus(PMConstants.TASK_STATUS_OPEN);
        issueService.saveIssue(newIssue);
        PortletUtils.addInfoMsg("Issue successfully created.", request);
        return "redirect:/action-plans/view-plan/"+ actionPlanId;
    }

    @RequestMapping(value = "/save-note", method = RequestMethod.POST)
    public String saveNote(HttpServletRequest request, long actionPlanId, String note) {

        Note newNote = new Note();
        newNote.setActionPlan(actionPlanService.getActionPlanById(actionPlanId));
        newNote.setComment(note);
        newNote.setStatus(PMConstants.TASK_STATUS_OPEN);
        noteService.saveNote(newNote);
        PortletUtils.addInfoMsg("Comment successfully posted.", request);
        return "redirect:/action-plans/view-plan/"+ actionPlanId;
    }

    @RequestMapping(value = "/update-task-status", method = RequestMethod.POST)
    public void updateTaskStatus(HttpServletResponse response, String taskId) {

        Task task = taskService.getTaskById(Long.parseLong(taskId));
        if(PMConstants.TASK_STATUS_OPEN.equals(task.getStatus())){
            task.setStatus(PMConstants.TASK_STATUS_COMPLETED);
        }else {
            task.setStatus(PMConstants.TASK_STATUS_OPEN);
        }
        taskService.saveTask(task);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", task.getStatus());

        String jsonString = jsonObject.toString();

        try(OutputStream outputStream = response.getOutputStream()){
            outputStream.write(jsonString.getBytes());

        }catch (IOException e){
            throw  new RuntimeException();
        }
    }

    @RequestMapping(value = "/update-action-plan-status", method = RequestMethod.POST)
    public void updateActionPlanStatus(HttpServletResponse response, long id, String status) {

        ActionPlan plan = actionPlanService.getActionPlanById(id);
        plan.setStatus(status);
        actionPlanService.saveActionPlan(plan);
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
    public void updateIssueStatus(HttpServletResponse response, String issueId) {

        Issue issue = issueService.getIssueById(Long.parseLong(issueId));
        if(PMConstants.TASK_STATUS_OPEN.equals(issue.getStatus())){
            issue.setStatus(PMConstants.TASK_STATUS_COMPLETED);
        }else {
            issue.setStatus(PMConstants.TASK_STATUS_OPEN);
        }
        issueService.saveIssue(issue);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", issue.getStatus());

        String jsonString = jsonObject.toString();

        try(OutputStream outputStream = response.getOutputStream()){
            outputStream.write(jsonString.getBytes());

        }catch (IOException e){
            throw  new RuntimeException();
        }
    }



}
