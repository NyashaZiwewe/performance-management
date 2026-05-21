package hr.performancemanagement.service.resource;

import hr.performancemanagement.entities.PerformanceImprovementPlan;
import hr.performancemanagement.entities.PIPIssue;
import hr.performancemanagement.entities.PIPNote;
import hr.performancemanagement.entities.PIPTask;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.exception.ResourceNotFoundException;
import hr.performancemanagement.service.api.PIPIssueService;
import hr.performancemanagement.service.api.PIPNoteService;
import hr.performancemanagement.service.api.PIPTaskService;
import hr.performancemanagement.service.api.PerformanceImprovementPlanService;
import hr.performancemanagement.service.api.ReportingPeriodService;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.dto.CommonResponse;
import hr.performancemanagement.utils.wrappers.StatusUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/performance-improvement-plans")
@RequiredArgsConstructor
public class PerformanceImprovementPlanResource {

    private final PerformanceImprovementPlanService performanceImprovementPlanService;
    private final ReportingPeriodService reportingPeriodService;
    private final PIPTaskService pipTaskService;
    private final PIPIssueService pipIssueService;
    private final PIPNoteService pipNoteService;

    @GetMapping("/client/{clientId}")
    public ResponseEntity<CommonResponse<List<PerformanceImprovementPlan>>> listByClientId(@PathVariable long clientId) {
        List<PerformanceImprovementPlan> plans = performanceImprovementPlanService.listAllPerformanceImprovementPlansByClientId(clientId);
        return ResponseEntity.ok(CommonResponse.<List<PerformanceImprovementPlan>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Performance improvement plans retrieved successfully")
                .data(plans)
                .build());
    }

    @GetMapping("/client/{clientId}/reporting-period/{reportingPeriodId}")
    public ResponseEntity<CommonResponse<List<PerformanceImprovementPlan>>> listByReportingPeriod(
            @PathVariable long clientId,
            @PathVariable long reportingPeriodId) {
        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(reportingPeriodId);
        if (reportingPeriod == null) {
            throw new ResourceNotFoundException("Reporting period not found with id " + reportingPeriodId);
        }
        List<PerformanceImprovementPlan> plans = performanceImprovementPlanService.listAllPerformanceImprovementPlans(clientId, reportingPeriod);
        return ResponseEntity.ok(CommonResponse.<List<PerformanceImprovementPlan>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Performance improvement plans retrieved successfully")
                .data(plans)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<PerformanceImprovementPlan>> getById(@PathVariable long id) {
        PerformanceImprovementPlan plan = performanceImprovementPlanService.getPerformanceImprovementPlanById(id);
        if (plan == null) {
            throw new ResourceNotFoundException("Performance improvement plan not found with id " + id);
        }
        return ResponseEntity.ok(CommonResponse.<PerformanceImprovementPlan>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Performance improvement plan retrieved successfully")
                .data(plan)
                .build());
    }

    @PostMapping
    public ResponseEntity<CommonResponse<PerformanceImprovementPlan>> save(@RequestBody PerformanceImprovementPlan plan) {
        PerformanceImprovementPlan savedPlan = performanceImprovementPlanService.savePerformanceImprovementPlan(plan);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<PerformanceImprovementPlan>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Performance improvement plan saved successfully")
                .data(savedPlan)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable long id) {
        PerformanceImprovementPlan plan = performanceImprovementPlanService.getPerformanceImprovementPlanById(id);
        if (plan == null) {
            throw new ResourceNotFoundException("Performance improvement plan not found with id " + id);
        }
        performanceImprovementPlanService.deletePerformanceImprovementPlan(plan);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Performance improvement plan deleted successfully")
                .data(null)
                .build());
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<CommonResponse<List<PIPTask>>> listTasks(@PathVariable long id) {
        ensurePlanExists(id);
        List<PIPTask> tasks = pipTaskService.listAllPIPTasks(id);
        return ResponseEntity.ok(CommonResponse.<List<PIPTask>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Performance improvement plan tasks retrieved successfully")
                .data(tasks)
                .build());
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<CommonResponse<PIPTask>> saveTask(@PathVariable long id, @RequestBody PIPTask task) {
        PerformanceImprovementPlan plan = ensurePlanExists(id);
        task.setPerformanceImprovementPlan(plan);
        if (!hasText(task.getStatus())) {
            task.setStatus(PMConstants.TASK_STATUS_OPEN);
        }
        PIPTask savedTask = pipTaskService.savePIPTask(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<PIPTask>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Task saved successfully")
                .data(savedTask)
                .build());
    }

    @PutMapping("/tasks/{taskId}/status")
    public ResponseEntity<CommonResponse<PIPTask>> updateTaskStatus(
            @PathVariable long taskId,
            @RequestBody(required = false) StatusUpdateWrapper wrapper) {
        PIPTask task = pipTaskService.getPIPTaskById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("Task not found with id " + taskId);
        }
        task.setStatus(resolveStatus(task.getStatus(), wrapper));
        PIPTask updatedTask = pipTaskService.savePIPTask(task);
        return ResponseEntity.ok(CommonResponse.<PIPTask>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Task status updated successfully")
                .data(updatedTask)
                .build());
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<CommonResponse<Void>> deleteTask(@PathVariable long taskId) {
        PIPTask task = pipTaskService.getPIPTaskById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("Task not found with id " + taskId);
        }
        pipTaskService.deletePIPTask(task);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Task deleted successfully")
                .data(null)
                .build());
    }

    @GetMapping("/{id}/issues")
    public ResponseEntity<CommonResponse<List<PIPIssue>>> listIssues(@PathVariable long id) {
        ensurePlanExists(id);
        List<PIPIssue> issues = pipIssueService.listAllPIPIssues(id);
        return ResponseEntity.ok(CommonResponse.<List<PIPIssue>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Performance improvement plan issues retrieved successfully")
                .data(issues)
                .build());
    }

    @PostMapping("/{id}/issues")
    public ResponseEntity<CommonResponse<PIPIssue>> saveIssue(@PathVariable long id, @RequestBody PIPIssue issue) {
        PerformanceImprovementPlan plan = ensurePlanExists(id);
        issue.setPerformanceImprovementPlan(plan);
        if (!hasText(issue.getStatus())) {
            issue.setStatus(PMConstants.TASK_STATUS_OPEN);
        }
        pipIssueService.savePIPIssue(issue);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<PIPIssue>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Issue saved successfully")
                .data(issue)
                .build());
    }

    @PutMapping("/issues/{issueId}/status")
    public ResponseEntity<CommonResponse<PIPIssue>> updateIssueStatus(
            @PathVariable long issueId,
            @RequestBody(required = false) StatusUpdateWrapper wrapper) {
        PIPIssue issue = pipIssueService.getPIPIssueById(issueId);
        if (issue == null) {
            throw new ResourceNotFoundException("Issue not found with id " + issueId);
        }
        issue.setStatus(resolveStatus(issue.getStatus(), wrapper));
        pipIssueService.savePIPIssue(issue);
        return ResponseEntity.ok(CommonResponse.<PIPIssue>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Issue status updated successfully")
                .data(issue)
                .build());
    }

    @DeleteMapping("/issues/{issueId}")
    public ResponseEntity<CommonResponse<Void>> deleteIssue(@PathVariable long issueId) {
        PIPIssue issue = pipIssueService.getPIPIssueById(issueId);
        if (issue == null) {
            throw new ResourceNotFoundException("Issue not found with id " + issueId);
        }
        pipIssueService.deletePIPIssue(issue);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Issue deleted successfully")
                .data(null)
                .build());
    }

    @GetMapping("/{id}/notes")
    public ResponseEntity<CommonResponse<List<PIPNote>>> listNotes(@PathVariable long id) {
        ensurePlanExists(id);
        List<PIPNote> notes = pipNoteService.listAllPIPNotes(id);
        return ResponseEntity.ok(CommonResponse.<List<PIPNote>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Performance improvement plan notes retrieved successfully")
                .data(notes)
                .build());
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<CommonResponse<PIPNote>> saveNote(@PathVariable long id, @RequestBody PIPNote note) {
        PerformanceImprovementPlan plan = ensurePlanExists(id);
        note.setPerformanceImprovementPlan(plan);
        if (!hasText(note.getStatus())) {
            note.setStatus(PMConstants.TASK_STATUS_OPEN);
        }
        pipNoteService.savePIPNote(note);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<PIPNote>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Note saved successfully")
                .data(note)
                .build());
    }

    private PerformanceImprovementPlan ensurePlanExists(long planId) {
        PerformanceImprovementPlan plan = performanceImprovementPlanService.getPerformanceImprovementPlanById(planId);
        if (plan == null) {
            throw new ResourceNotFoundException("Performance improvement plan not found with id " + planId);
        }
        return plan;
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
