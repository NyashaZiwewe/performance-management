package hr.performancemanagement.controllers.actionplan;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.service.api.*;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping(value = "/action-plans")
public class ActionPlanController {

    private static final Logger log = LoggerFactory.getLogger(ActionPlanController.class);

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
    public ModelAndView viewActionPlans2(@RequestParam(value = "reportingPeriodId", required = false) Long reportingPeriodId,
                                         HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.VIEW_ACTION_PLANS2);
        modelAndView.addObject("pageTitle", "View All");
        Account loggedUser = commonService.getLoggedUser();
        ReportingPeriod reportingPeriod = reportingPeriodService.getActiveReportingPeriod();
        if (reportingPeriodId != null) {
            ReportingPeriod selectedPeriod = reportingPeriodService.getReportingPeriodById(reportingPeriodId);
            if (selectedPeriod != null && loggedUser != null && selectedPeriod.getClientId() == loggedUser.getClientId()) {
                reportingPeriod = selectedPeriod;
            }
        }
        List<ActionPlan> plansList = reportingPeriod == null
                ? Collections.emptyList()
                : actionPlanService.listAllActionPlans(reportingPeriod);
        for(ActionPlan plan : plansList){
            String initials = commonService.getInitials(plan.getManager().getFullName());
            plan.getManager().setInitials(initials);
        }
        modelAndView.addObject("plansList", plansList);
        modelAndView.addObject("actionPlan", new ActionPlan());
        modelAndView.addObject("loggedUser", loggedUser);
        modelAndView.addObject("selectedReportingPeriodId", reportingPeriod != null ? reportingPeriod.getId() : null);
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
        log.info("Creating action plan: {}", actionPlan.getName());
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
        log.info("Creating action plan for manager: {}", loggedUser.getEmail());
        actionPlanService.addActionPlan(actionPlan);
        PortletUtils.addInfoMsg("Action Plan successfully created.", request);
        return "redirect:/action-plans";
    }

    @RequestMapping(value = "/update-plan", method = RequestMethod.POST)
    public String updatePlan(HttpServletRequest request, ActionPlan newPlan) {
        try {
            ActionPlan existingPlan = actionPlanService.getActionPlanById(newPlan.getId());
            if(existingPlan == null){
                PortletUtils.addErrorMsg("Action plan not found.", request);
                return "redirect:/action-plans";
            }

            existingPlan.setName(newPlan.getName());
            existingPlan.setDescription(newPlan.getDescription());
            existingPlan.setMeasureOfSuccess(newPlan.getMeasureOfSuccess());
            existingPlan.setManager(newPlan.getManager());
            existingPlan.setReportingPeriod(newPlan.getReportingPeriod());
            existingPlan.setStartDate(newPlan.getStartDate());
            existingPlan.setEndDate(newPlan.getEndDate());

            applyTaskDrivenProgress(existingPlan);
            actionPlanService.saveActionPlan(existingPlan);
            PortletUtils.addInfoMsg("Action Plan successfully updated.", request);
        }catch (Exception e){
            log.error("Error updating action plan: {}", newPlan != null ? newPlan.getId() : "null", e);
            PortletUtils.addErrorMsg("Failed to update action plan.", request);
        }

        return "redirect:/action-plans";
    }

    @RequestMapping(value = "/delete-plan", method = RequestMethod.POST)
    public String deletePlan(HttpServletRequest request, ActionPlan actionPlan) {

        actionPlanService.deleteActionPlan(actionPlan);
        PortletUtils.addInfoMsg("Action plan was successfully deleted", request);
        return "redirect:/action-plans";
    }

    @RequestMapping(value = "/save-task", method = RequestMethod.POST)
    public void saveTask(HttpServletResponse response, long actionPlanId, String task) {

        JSONObject jsonObject = new JSONObject();
        String taskName = task == null ? "" : task.trim();
        if(taskName.isEmpty()){
            jsonObject.put("saved", false);
            jsonObject.put("message", "Task cannot be empty.");
            writeJsonResponse(response, jsonObject);
            return;
        }

        try {
            Task newTask = new Task();
            newTask.setActionPlan(actionPlanService.getActionPlanById(actionPlanId));
            newTask.setName(taskName);
            newTask.setStatus(PMConstants.TASK_STATUS_OPEN);
            taskService.saveTask(newTask);

            jsonObject.put("saved", true);
            jsonObject.put("id", newTask.getId());
            jsonObject.put("name", newTask.getName());
            jsonObject.put("status", newTask.getStatus());
            appendActionPlanProgressPayload(jsonObject, actionPlanId);
        } catch (Exception e){
            log.error("Error saving action plan task for planId={}", actionPlanId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonObject.put("saved", false);
            jsonObject.put("message", "Failed to create task.");
        }
        writeJsonResponse(response, jsonObject);
    }

    @RequestMapping(value = "/save-issue", method = RequestMethod.POST)
    public void saveIssue(HttpServletResponse response, long actionPlanId, String issue) {

        JSONObject jsonObject = new JSONObject();
        String issueName = issue == null ? "" : issue.trim();
        if(issueName.isEmpty()){
            jsonObject.put("saved", false);
            jsonObject.put("message", "Issue cannot be empty.");
            writeJsonResponse(response, jsonObject);
            return;
        }

        try {
            Issue newIssue = new Issue();
            newIssue.setActionPlan(actionPlanService.getActionPlanById(actionPlanId));
            newIssue.setName(issueName);
            newIssue.setStatus(PMConstants.TASK_STATUS_OPEN);
            issueService.saveIssue(newIssue);

            jsonObject.put("saved", true);
            jsonObject.put("id", newIssue.getId());
            jsonObject.put("name", newIssue.getName());
            jsonObject.put("status", newIssue.getStatus());
        } catch (Exception e){
            log.error("Error saving action plan issue for planId={}", actionPlanId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonObject.put("saved", false);
            jsonObject.put("message", "Failed to create issue.");
        }
        writeJsonResponse(response, jsonObject);
    }

    @RequestMapping(value = "/save-note", method = RequestMethod.POST)
    public void saveNote(HttpServletResponse response, long actionPlanId, String note) {

        JSONObject jsonObject = new JSONObject();
        String noteComment = note == null ? "" : note.trim();
        if(noteComment.isEmpty()){
            jsonObject.put("saved", false);
            jsonObject.put("message", "Comment cannot be empty.");
            writeJsonResponse(response, jsonObject);
            return;
        }

        try {
            Note newNote = new Note();
            newNote.setActionPlan(actionPlanService.getActionPlanById(actionPlanId));
            newNote.setComment(noteComment);
            newNote.setEmployee(commonService.getLoggedUser());
            newNote.setStatus(PMConstants.TASK_STATUS_OPEN);
            noteService.saveNote(newNote);

            jsonObject.put("saved", true);
            jsonObject.put("id", newNote.getId());
            jsonObject.put("comment", newNote.getComment());
        } catch (Exception e){
            log.error("Error saving action plan comment for planId={}", actionPlanId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            jsonObject.put("saved", false);
            jsonObject.put("message", "Failed to post comment.");
        }
        writeJsonResponse(response, jsonObject);
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
        appendActionPlanProgressPayload(jsonObject, task.getActionPlan().getId());
        writeJsonResponse(response, jsonObject);
    }

    @RequestMapping(value = "/update-action-plan-status", method = RequestMethod.POST)
    public void updateActionPlanStatus(HttpServletResponse response, long id, String status) {

        ActionPlan plan = actionPlanService.getActionPlanById(id);
        plan.setStatus(status);
        if(PMConstants.TASK_STATUS_COMPLETED.equalsIgnoreCase(status)){
            plan.setProgress(100);
        }
        actionPlanService.saveActionPlan(plan);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("alreadyExists", false);
        writeJsonResponse(response, jsonObject);
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
        writeJsonResponse(response, jsonObject);
    }

    private void writeJsonResponse(HttpServletResponse response, JSONObject jsonObject) {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try(OutputStream outputStream = response.getOutputStream()){
            outputStream.write(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e){
            throw new RuntimeException("Failed to write JSON response.", e);
        }
    }

    private void appendActionPlanProgressPayload(JSONObject jsonObject, long actionPlanId){
        ActionPlan plan = actionPlanService.getActionPlanById(actionPlanId);
        int[] progressSnapshot = applyTaskDrivenProgress(plan);
        actionPlanService.saveActionPlan(plan);

        jsonObject.put("planId", plan.getId());
        jsonObject.put("planStatus", plan.getStatus());
        jsonObject.put("planProgress", plan.getProgress());
        jsonObject.put("totalTasks", progressSnapshot[0]);
        jsonObject.put("completedTasks", progressSnapshot[1]);
    }

    private int[] applyTaskDrivenProgress(ActionPlan plan){
        List<Task> taskList = taskService.listAllTasks(plan.getId());
        int totalTasks = taskList == null ? 0 : taskList.size();
        int completedTasks = 0;
        if(taskList != null){
            for (Task planTask : taskList){
                if(PMConstants.TASK_STATUS_COMPLETED.equalsIgnoreCase(planTask.getStatus())){
                    completedTasks += 1;
                }
            }
        }

        double progress = totalTasks == 0 ? 0.0 : (completedTasks * 100.0) / totalTasks;
        progress = Math.round(progress * 10.0) / 10.0;
        plan.setProgress(progress);
        if(totalTasks == 0 || completedTasks == 0){
            plan.setStatus("todo");
        } else if (completedTasks == totalTasks){
            plan.setStatus("completed");
        } else {
            plan.setStatus("inprogress");
        }
        return new int[]{totalTasks, completedTasks};
    }



}
