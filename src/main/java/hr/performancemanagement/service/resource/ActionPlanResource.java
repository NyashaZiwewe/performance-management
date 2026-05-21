package hr.performancemanagement.service.resource;

import hr.performancemanagement.entities.ActionPlan;
import hr.performancemanagement.entities.Issue;
import hr.performancemanagement.entities.Note;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.Task;
import hr.performancemanagement.exception.ResourceNotFoundException;
import hr.performancemanagement.service.api.ActionPlanService;
import hr.performancemanagement.service.api.IssueService;
import hr.performancemanagement.service.api.NoteService;
import hr.performancemanagement.service.api.ReportingPeriodService;
import hr.performancemanagement.service.api.TaskService;
import hr.performancemanagement.utils.dto.CommonResponse;
import hr.performancemanagement.utils.wrappers.StatusUpdateWrapper;
import hr.performancemanagement.utils.constants.PMConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/action-plans")
@RequiredArgsConstructor
public class ActionPlanResource {

    private final ActionPlanService actionPlanService;
    private final ReportingPeriodService reportingPeriodService;
    private final TaskService taskService;
    private final IssueService issueService;
    private final NoteService noteService;

    @GetMapping("/client/{clientId}")
    public ResponseEntity<CommonResponse<List<ActionPlan>>> listByClientId(@PathVariable long clientId) {
        List<ActionPlan> actionPlans = actionPlanService.listAllActionPlansByClientId(clientId);
        return ResponseEntity.ok(CommonResponse.<List<ActionPlan>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Action plans retrieved successfully")
                .data(actionPlans)
                .build());
    }

    @GetMapping("/client/{clientId}/reporting-period/{reportingPeriodId}")
    public ResponseEntity<CommonResponse<List<ActionPlan>>> listByReportingPeriod(
            @PathVariable long clientId,
            @PathVariable long reportingPeriodId) {
        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(reportingPeriodId);
        if (reportingPeriod == null) {
            throw new ResourceNotFoundException("Reporting period not found with id " + reportingPeriodId);
        }
        List<ActionPlan> actionPlans = actionPlanService.listAllActionPlansByReportingPeriod(clientId, reportingPeriod);
        return ResponseEntity.ok(CommonResponse.<List<ActionPlan>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Action plans retrieved successfully")
                .data(actionPlans)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<ActionPlan>> getById(@PathVariable long id) {
        ActionPlan actionPlan = actionPlanService.getActionPlanById(id);
        if (actionPlan == null) {
            throw new ResourceNotFoundException("Action plan not found with id " + id);
        }
        return ResponseEntity.ok(CommonResponse.<ActionPlan>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Action plan retrieved successfully")
                .data(actionPlan)
                .build());
    }

    @PostMapping
    public ResponseEntity<CommonResponse<ActionPlan>> save(@RequestBody ActionPlan actionPlan) {
        ActionPlan savedActionPlan = actionPlanService.saveActionPlan(actionPlan);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<ActionPlan>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Action plan saved successfully")
                .data(savedActionPlan)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable long id) {
        ActionPlan actionPlan = actionPlanService.getActionPlanById(id);
        if (actionPlan == null) {
            throw new ResourceNotFoundException("Action plan not found with id " + id);
        }
        actionPlanService.deleteActionPlan(actionPlan);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Action plan deleted successfully")
                .data(null)
                .build());
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<CommonResponse<List<Task>>> listTasks(@PathVariable long id) {
        ensureActionPlanExists(id);
        List<Task> tasks = taskService.listAllTasks(id);
        return ResponseEntity.ok(CommonResponse.<List<Task>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Action plan tasks retrieved successfully")
                .data(tasks)
                .build());
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<CommonResponse<Task>> saveTask(@PathVariable long id, @RequestBody Task task) {
        ActionPlan actionPlan = ensureActionPlanExists(id);
        task.setActionPlan(actionPlan);
        if (!hasText(task.getStatus())) {
            task.setStatus(PMConstants.TASK_STATUS_OPEN);
        }
        taskService.saveTask(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Task>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Task saved successfully")
                .data(task)
                .build());
    }

    @PutMapping("/tasks/{taskId}/status")
    public ResponseEntity<CommonResponse<Task>> updateTaskStatus(
            @PathVariable long taskId,
            @RequestBody(required = false) StatusUpdateWrapper wrapper) {
        Task task = taskService.getTaskById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("Task not found with id " + taskId);
        }
        task.setStatus(resolveStatus(task.getStatus(), wrapper));
        taskService.saveTask(task);
        return ResponseEntity.ok(CommonResponse.<Task>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Task status updated successfully")
                .data(task)
                .build());
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<CommonResponse<Void>> deleteTask(@PathVariable long taskId) {
        Task task = taskService.getTaskById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("Task not found with id " + taskId);
        }
        taskService.deleteTask(task);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Task deleted successfully")
                .data(null)
                .build());
    }

    @GetMapping("/{id}/issues")
    public ResponseEntity<CommonResponse<List<Issue>>> listIssues(@PathVariable long id) {
        ensureActionPlanExists(id);
        List<Issue> issues = issueService.listAllIssues(id);
        return ResponseEntity.ok(CommonResponse.<List<Issue>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Action plan issues retrieved successfully")
                .data(issues)
                .build());
    }

    @PostMapping("/{id}/issues")
    public ResponseEntity<CommonResponse<Issue>> saveIssue(@PathVariable long id, @RequestBody Issue issue) {
        ActionPlan actionPlan = ensureActionPlanExists(id);
        issue.setActionPlan(actionPlan);
        if (!hasText(issue.getStatus())) {
            issue.setStatus(PMConstants.TASK_STATUS_OPEN);
        }
        issueService.saveIssue(issue);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Issue>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Issue saved successfully")
                .data(issue)
                .build());
    }

    @PutMapping("/issues/{issueId}/status")
    public ResponseEntity<CommonResponse<Issue>> updateIssueStatus(
            @PathVariable long issueId,
            @RequestBody(required = false) StatusUpdateWrapper wrapper) {
        Issue issue = issueService.getIssueById(issueId);
        if (issue == null) {
            throw new ResourceNotFoundException("Issue not found with id " + issueId);
        }
        issue.setStatus(resolveStatus(issue.getStatus(), wrapper));
        issueService.saveIssue(issue);
        return ResponseEntity.ok(CommonResponse.<Issue>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Issue status updated successfully")
                .data(issue)
                .build());
    }

    @DeleteMapping("/issues/{issueId}")
    public ResponseEntity<CommonResponse<Void>> deleteIssue(@PathVariable long issueId) {
        Issue issue = issueService.getIssueById(issueId);
        if (issue == null) {
            throw new ResourceNotFoundException("Issue not found with id " + issueId);
        }
        issueService.deleteIssue(issue);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Issue deleted successfully")
                .data(null)
                .build());
    }

    @GetMapping("/{id}/notes")
    public ResponseEntity<CommonResponse<List<Note>>> listNotes(@PathVariable long id) {
        ensureActionPlanExists(id);
        List<Note> notes = noteService.listAllNotes(id);
        return ResponseEntity.ok(CommonResponse.<List<Note>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Action plan notes retrieved successfully")
                .data(notes)
                .build());
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<CommonResponse<Note>> saveNote(@PathVariable long id, @RequestBody Note note) {
        ActionPlan actionPlan = ensureActionPlanExists(id);
        note.setActionPlan(actionPlan);
        if (!hasText(note.getStatus())) {
            note.setStatus(PMConstants.TASK_STATUS_OPEN);
        }
        noteService.saveNote(note);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Note>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Note saved successfully")
                .data(note)
                .build());
    }

    private ActionPlan ensureActionPlanExists(long actionPlanId) {
        ActionPlan actionPlan = actionPlanService.getActionPlanById(actionPlanId);
        if (actionPlan == null) {
            throw new ResourceNotFoundException("Action plan not found with id " + actionPlanId);
        }
        return actionPlan;
    }

    private String resolveStatus(String currentStatus, StatusUpdateWrapper wrapper) {
        if (wrapper != null && hasText(wrapper.getStatus())) {
            return wrapper.getStatus().trim();
        }
        if (PMConstants.TASK_STATUS_OPEN.equalsIgnoreCase(currentStatus)) {
            return PMConstants.TASK_STATUS_COMPLETED;
        }
        return PMConstants.TASK_STATUS_OPEN;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
