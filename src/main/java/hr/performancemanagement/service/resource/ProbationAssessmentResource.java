package hr.performancemanagement.service.resource;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.exception.ResourceNotFoundException;
import hr.performancemanagement.service.api.ProbationAssessmentService;
import hr.performancemanagement.service.api.ProbationConfigService;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.dto.CommonResponse;
import hr.performancemanagement.utils.wrappers.ProbationDimensionResponseWrapper;
import hr.performancemanagement.utils.wrappers.RemarksWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/probation-assessments")
@RequiredArgsConstructor
public class ProbationAssessmentResource {

    private final ProbationAssessmentService probationAssessmentService;
    private final ProbationConfigService probationConfigService;

    @GetMapping
    public ResponseEntity<CommonResponse<List<ProbationAssessment>>> listAssessments() {
        List<ProbationAssessment> assessments = probationAssessmentService.listVisibleAssessments();
        return ResponseEntity.ok(CommonResponse.<List<ProbationAssessment>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Probation assessments retrieved successfully")
                .data(assessments)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<ProbationAssessment>> getAssessment(@PathVariable long id) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(id);
        if (assessment == null) {
            throw new ResourceNotFoundException("Probation assessment not found with id " + id);
        }
        return ResponseEntity.ok(CommonResponse.<ProbationAssessment>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Probation assessment retrieved successfully")
                .data(assessment)
                .build());
    }

    @GetMapping("/{id}/permissions")
    public ResponseEntity<CommonResponse<Map<String, Object>>> getAssessmentPermissions(@PathVariable long id) {
        ProbationAssessment assessment = probationAssessmentService.getAssessmentById(id);
        if (assessment == null) {
            throw new ResourceNotFoundException("Probation assessment not found with id " + id);
        }

        Map<String, Object> permissions = new LinkedHashMap<>();
        permissions.put("canSubmit", probationAssessmentService.canLoggedUserSubmit(assessment));
        permissions.put("canApprove", probationAssessmentService.canLoggedUserApprove(assessment));
        permissions.put("isOwner", probationAssessmentService.isLoggedUserOwner(assessment));
        permissions.put("isSupervisor", probationAssessmentService.isLoggedUserSupervisor(assessment));
        permissions.put("workflowConfigured", !probationConfigService.listActiveWorkflowSteps().isEmpty());

        return ResponseEntity.ok(CommonResponse.<Map<String, Object>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Assessment permissions retrieved successfully")
                .data(permissions)
                .build());
    }

    @PostMapping
    public ResponseEntity<CommonResponse<ProbationAssessment>> createAssessment(@RequestBody ProbationAssessment assessment) {
        ProbationAssessment savedAssessment = probationAssessmentService.createAssessment(assessment);
        if (savedAssessment == null) {
            throw new ResourceNotFoundException("Assessment could not be created. Check employee selection and access rights.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<ProbationAssessment>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Probation assessment created successfully")
                .data(savedAssessment)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<ProbationAssessment>> updateAssessment(
            @PathVariable long id,
            @RequestBody ProbationAssessment assessment) {
        assessment.setId(id);
        ProbationAssessment updatedAssessment = probationAssessmentService.updateAssessmentCore(assessment);
        if (updatedAssessment == null) {
            throw new ResourceNotFoundException("Assessment update failed. Check access rights and id.");
        }
        return ResponseEntity.ok(CommonResponse.<ProbationAssessment>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Probation assessment updated successfully")
                .data(updatedAssessment)
                .build());
    }

    @GetMapping("/{id}/dimensions")
    public ResponseEntity<CommonResponse<List<ProbationAssessmentDimension>>> listDimensions(@PathVariable long id) {
        List<ProbationAssessmentDimension> dimensions = probationAssessmentService.listAssessmentDimensions(id);
        return ResponseEntity.ok(CommonResponse.<List<ProbationAssessmentDimension>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Assessment dimensions retrieved successfully")
                .data(dimensions)
                .build());
    }

    @PostMapping("/{id}/dimensions")
    public ResponseEntity<CommonResponse<ProbationAssessmentDimension>> saveDimensionResponse(
            @PathVariable long id,
            @RequestBody ProbationDimensionResponseWrapper wrapper) {
        ProbationAssessmentDimension savedResponse = probationAssessmentService.saveDimensionResponse(
                id,
                wrapper.getDimensionTemplateId(),
                wrapper.getStrengths(),
                wrapper.getAreasForImprovement());
        if (savedResponse == null) {
            throw new ResourceNotFoundException("Dimension response could not be saved. Check access rights and ids.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<ProbationAssessmentDimension>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Dimension response saved successfully")
                .data(savedResponse)
                .build());
    }

    @GetMapping("/{id}/kpis")
    public ResponseEntity<CommonResponse<List<ProbationKpi>>> listKpis(@PathVariable long id) {
        List<ProbationKpi> kpis = probationAssessmentService.listKpis(id);
        return ResponseEntity.ok(CommonResponse.<List<ProbationKpi>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("KPIs retrieved successfully")
                .data(kpis)
                .build());
    }

    @PostMapping("/{id}/kpis")
    public ResponseEntity<CommonResponse<ProbationKpi>> addKpi(@PathVariable long id, @RequestBody ProbationKpi kpi) {
        ProbationKpi savedKpi = probationAssessmentService.addKpi(id, kpi);
        if (savedKpi == null) {
            throw new ResourceNotFoundException("KPI could not be added. Check access rights and assessment id.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<ProbationKpi>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("KPI added successfully")
                .data(savedKpi)
                .build());
    }

    @PutMapping("/kpis/{kpiId}")
    public ResponseEntity<CommonResponse<ProbationKpi>> updateKpi(@PathVariable long kpiId, @RequestBody ProbationKpi kpi) {
        kpi.setId(kpiId);
        ProbationKpi updatedKpi = probationAssessmentService.updateKpi(kpi);
        if (updatedKpi == null) {
            throw new ResourceNotFoundException("KPI update failed. Check access rights and id.");
        }
        return ResponseEntity.ok(CommonResponse.<ProbationKpi>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("KPI updated successfully")
                .data(updatedKpi)
                .build());
    }

    @DeleteMapping("/kpis/{kpiId}")
    public ResponseEntity<CommonResponse<Void>> deleteKpi(@PathVariable long kpiId) {
        probationAssessmentService.deleteKpi(kpiId);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("KPI deleted successfully")
                .data(null)
                .build());
    }

    @GetMapping("/{id}/approval-history")
    public ResponseEntity<CommonResponse<List<ProbationAssessmentApproval>>> listApprovalHistory(@PathVariable long id) {
        List<ProbationAssessmentApproval> approvals = probationAssessmentService.listApprovalHistory(id);
        return ResponseEntity.ok(CommonResponse.<List<ProbationAssessmentApproval>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Approval history retrieved successfully")
                .data(approvals)
                .build());
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<CommonResponse<Boolean>> submitAssessment(@PathVariable long id, @RequestBody(required = false) RemarksWrapper wrapper) {
        boolean submitted = probationAssessmentService.submitAssessment(id, wrapper == null ? null : wrapper.getRemarks());
        return ResponseEntity.ok(CommonResponse.<Boolean>builder()
                .isSuccess(submitted)
                .statusCode(HttpStatus.OK.value())
                .message(submitted
                        ? "Assessment submitted for authorization"
                        : "Submission failed. Ensure a workflow is configured and you have access to submit.")
                .data(submitted)
                .build());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<CommonResponse<Boolean>> approveAssessment(@PathVariable long id, @RequestBody(required = false) RemarksWrapper wrapper) {
        boolean approved = probationAssessmentService.approveAssessment(id, wrapper == null ? null : wrapper.getRemarks());
        return ResponseEntity.ok(CommonResponse.<Boolean>builder()
                .isSuccess(approved)
                .statusCode(HttpStatus.OK.value())
                .message(approved ? "Assessment approved successfully" : "Approval failed. Check access rights.")
                .data(approved)
                .build());
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<CommonResponse<Boolean>> rejectAssessment(@PathVariable long id, @RequestBody(required = false) RemarksWrapper wrapper) {
        boolean rejected = probationAssessmentService.rejectAssessment(id, wrapper == null ? null : wrapper.getRemarks());
        return ResponseEntity.ok(CommonResponse.<Boolean>builder()
                .isSuccess(rejected)
                .statusCode(HttpStatus.OK.value())
                .message(rejected ? "Assessment rejected successfully" : "Rejection failed. Check access rights.")
                .data(rejected)
                .build());
    }

    @GetMapping("/config/dimension-templates")
    public ResponseEntity<CommonResponse<List<ProbationDimensionTemplate>>> listDimensionTemplates() {
        List<ProbationDimensionTemplate> templates = probationConfigService.listAllDimensionTemplates();
        return ResponseEntity.ok(CommonResponse.<List<ProbationDimensionTemplate>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Dimension templates retrieved successfully")
                .data(templates)
                .build());
    }

    @PostMapping("/config/dimension-templates")
    public ResponseEntity<CommonResponse<ProbationDimensionTemplate>> saveDimensionTemplate(@RequestBody ProbationDimensionTemplate template) {
        ProbationDimensionTemplate savedTemplate = probationConfigService.saveDimensionTemplate(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<ProbationDimensionTemplate>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Dimension template saved successfully")
                .data(savedTemplate)
                .build());
    }

    @DeleteMapping("/config/dimension-templates/{id}")
    public ResponseEntity<CommonResponse<Void>> deactivateDimensionTemplate(@PathVariable long id) {
        probationConfigService.deactivateDimensionTemplate(id);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Dimension template deactivated successfully")
                .data(null)
                .build());
    }

    @GetMapping("/config/workflow-steps")
    public ResponseEntity<CommonResponse<List<ProbationWorkflowStep>>> listWorkflowSteps() {
        List<ProbationWorkflowStep> workflowSteps = probationConfigService.listAllWorkflowSteps();
        return ResponseEntity.ok(CommonResponse.<List<ProbationWorkflowStep>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Workflow steps retrieved successfully")
                .data(workflowSteps)
                .build());
    }

    @PostMapping("/config/workflow-steps")
    public ResponseEntity<CommonResponse<ProbationWorkflowStep>> saveWorkflowStep(@RequestBody ProbationWorkflowStep step) {
        normalizeWorkflowStep(step);
        ProbationWorkflowStep savedStep = probationConfigService.saveWorkflowStep(step);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<ProbationWorkflowStep>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Workflow step saved successfully")
                .data(savedStep)
                .build());
    }

    @DeleteMapping("/config/workflow-steps/{id}")
    public ResponseEntity<CommonResponse<Void>> deactivateWorkflowStep(@PathVariable long id) {
        probationConfigService.deactivateWorkflowStep(id);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Workflow step deactivated successfully")
                .data(null)
                .build());
    }

    @GetMapping("/config/approver-modes")
    public ResponseEntity<CommonResponse<List<String>>> listApproverModes() {
        List<String> approverModes = probationConfigService.listApproverModes();
        return ResponseEntity.ok(CommonResponse.<List<String>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Approver modes retrieved successfully")
                .data(approverModes)
                .build());
    }

    private void normalizeWorkflowStep(ProbationWorkflowStep step) {
        if (step.getApproverMode() == null) {
            step.setApproverMode(PMConstants.PROBATION_APPROVER_MODE_SUPERVISOR);
        }
        if (!PMConstants.PROBATION_APPROVER_MODE_ACCOUNT_TYPE.equalsIgnoreCase(step.getApproverMode())) {
            step.setApproverAccountType(null);
            step.setSameDivisionOnly(false);
        }
        if (!PMConstants.PROBATION_APPROVER_MODE_ROLE.equalsIgnoreCase(step.getApproverMode())) {
            step.setApproverRole(null);
        }
        if (!PMConstants.PROBATION_APPROVER_MODE_USER.equalsIgnoreCase(step.getApproverMode())) {
            step.setApproverAccount(null);
        }
        if (step.getStatus() == null || step.getStatus().isEmpty()) {
            step.setStatus(PMConstants.STATUS_ACTIVE);
        }
    }
}
