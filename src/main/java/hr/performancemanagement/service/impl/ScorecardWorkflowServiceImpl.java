package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.ScorecardWorkflowStage;
import hr.performancemanagement.service.api.ScorecardWorkflowService;
import hr.performancemanagement.service.api.ScorecardWorkflowStageService;
import hr.performancemanagement.service.api.SystemSettingService;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.dto.ScorecardWorkflowDefinition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class ScorecardWorkflowServiceImpl implements ScorecardWorkflowService {

    private final SystemSettingService systemSettingService;
    private final ScorecardWorkflowStageService scorecardWorkflowStageService;

    public ScorecardWorkflowServiceImpl(SystemSettingService systemSettingService,
                                        ScorecardWorkflowStageService scorecardWorkflowStageService) {
        this.systemSettingService = systemSettingService;
        this.scorecardWorkflowStageService = scorecardWorkflowStageService;
    }

    @Override
    public ScorecardWorkflowDefinition getWorkflowDefinition() {
        List<ScorecardWorkflowStage> stages = scorecardWorkflowStageService.listActiveWorkflowStages();
        Map<String, String> roleToStatus = mapRolesToStatuses(stages);
        Map<String, String> statusLabels = mapStatusLabels(stages);
        Map<String, String> statusStages = mapStatusStages(stages);
        Map<String, String> statusActionButtons = mapStatusActionButtons(stages);

        String newStatus = configuredStatus(roleToStatus, PMConstants.SCORECARD_WORKFLOW_ROLE_NEW, systemSettingService.getScorecardStatusNew(), PMConstants.APPROVAL_STATUS_NEW);
        String pendingApprovalStatus = configuredStatus(roleToStatus, PMConstants.SCORECARD_WORKFLOW_ROLE_PENDING_APPROVAL, systemSettingService.getScorecardStatusPendingApproval(), PMConstants.APPROVAL_STATUS_PENDING_APPROVAL);
        String approvedBySupervisorStatus = configuredStatus(roleToStatus, PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_BY_SUPERVISOR, systemSettingService.getScorecardStatusApprovedBySupervisor(), PMConstants.APPROVAL_STATUS_APPROVED_BY_SUPERVISOR);
        String rejectedBySupervisorStatus = configuredStatus(roleToStatus, PMConstants.SCORECARD_WORKFLOW_ROLE_REJECTED_BY_SUPERVISOR, systemSettingService.getScorecardStatusRejectedBySupervisor(), PMConstants.APPROVAL_STATUS_REJECTED_BY_SUPERVISOR);
        String approvedByHrStatus = configuredStatus(roleToStatus, PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_BY_HR, systemSettingService.getScorecardStatusApprovedByHr(), PMConstants.APPROVAL_STATUS_APPROVED_BY_HR);
        String rejectedByHrStatus = configuredStatus(roleToStatus, PMConstants.SCORECARD_WORKFLOW_ROLE_REJECTED_BY_HR, systemSettingService.getScorecardStatusRejectedByHr(), PMConstants.APPROVAL_STATUS_REJECTED_BY_HR);
        String scoredByEmployeeStatus = configuredStatus(roleToStatus, PMConstants.SCORECARD_WORKFLOW_ROLE_SCORED_BY_EMPLOYEE, systemSettingService.getScorecardStatusScoredByEmployee(), PMConstants.APPROVAL_STATUS_SCORED_BY_EMPLOYEE);
        String approvedOwnerScoresStatus = configuredStatus(roleToStatus, PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_OWNER_SCORES, null, PMConstants.APPROVAL_STATUS_APPROVED_OWNER_SCORES);
        String scoredBySupervisorStatus = configuredStatus(roleToStatus, PMConstants.SCORECARD_WORKFLOW_ROLE_SCORED_BY_SUPERVISOR, systemSettingService.getScorecardStatusScoredBySupervisor(), PMConstants.APPROVAL_STATUS_SCORED_BY_SUPERVISOR);
        String agreedByTwoStatus = configuredStatus(roleToStatus, PMConstants.SCORECARD_WORKFLOW_ROLE_AGREED_BY_TWO, systemSettingService.getScorecardStatusAgreedByTwo(), PMConstants.APPROVAL_STATUS_AGREED_BY_TWO);
        String approvedAgreedScoresStatus = configuredStatus(roleToStatus, PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_AGREED_SCORES, null, PMConstants.APPROVAL_STATUS_APPROVED_AGREED_SCORES);
        String moderatedByHrStatus = configuredStatus(roleToStatus, PMConstants.SCORECARD_WORKFLOW_ROLE_MODERATED_BY_HR, systemSettingService.getScorecardStatusModeratedByHr(), PMConstants.APPROVAL_STATUS_MODERATED_BY_HR);
        String closedStatus = configuredStatus(roleToStatus, PMConstants.SCORECARD_WORKFLOW_ROLE_CLOSED, systemSettingService.getScorecardStatusClosed(), PMConstants.APPROVAL_STATUS_CLOSED);

        List<String> defaults = Arrays.asList(newStatus, pendingApprovalStatus, approvedBySupervisorStatus, rejectedBySupervisorStatus,
                approvedByHrStatus, rejectedByHrStatus, scoredByEmployeeStatus, approvedOwnerScoresStatus, scoredBySupervisorStatus, agreedByTwoStatus,
                approvedAgreedScoresStatus, moderatedByHrStatus, closedStatus);
        List<String> orderedStatuses = mergeConfiguredStages(stages, defaults);
        for (String status : orderedStatuses) {
            if (status == null) {
                continue;
            }
            if (!statusLabels.containsKey(status)) {
                statusLabels.put(status, defaultStatusLabel(status));
            }
            if (!statusStages.containsKey(status)) {
                statusStages.put(status, "Workflow");
            }
            if (!statusActionButtons.containsKey(status)) {
                statusActionButtons.put(status, defaultStatusLabel(status));
            }
        }

        Map<String, String> cssByStatus = new LinkedHashMap<String, String>();
        cssByStatus.put(newStatus, "label label-primary");
        cssByStatus.put(pendingApprovalStatus, "label label-warning");
        cssByStatus.put(approvedBySupervisorStatus, "label label-success");
        cssByStatus.put(rejectedBySupervisorStatus, "label label-danger");
        cssByStatus.put(approvedByHrStatus, "label label-success");
        cssByStatus.put(rejectedByHrStatus, "label label-danger");
        cssByStatus.put(scoredByEmployeeStatus, "label label-success");
        cssByStatus.put(approvedOwnerScoresStatus, "label label-success");
        cssByStatus.put(scoredBySupervisorStatus, "label label-success");
        cssByStatus.put(agreedByTwoStatus, "label label-success");
        cssByStatus.put(approvedAgreedScoresStatus, "label label-success");
        cssByStatus.put(moderatedByHrStatus, "label label-success");
        cssByStatus.put(closedStatus, "label label-secondary");

        return new ScorecardWorkflowDefinition(
                newStatus,
                pendingApprovalStatus,
                approvedBySupervisorStatus,
                rejectedBySupervisorStatus,
                approvedByHrStatus,
                rejectedByHrStatus,
                scoredByEmployeeStatus,
                approvedOwnerScoresStatus,
                scoredBySupervisorStatus,
                agreedByTwoStatus,
                approvedAgreedScoresStatus,
                moderatedByHrStatus,
                closedStatus,
                orderedStatuses,
                cssByStatus,
                statusLabels,
                statusStages,
                statusActionButtons
        );
    }

    @Override
    public boolean matches(String actualStatus, String expectedStatus) {
        if (actualStatus == null || expectedStatus == null) {
            return false;
        }
        return actualStatus.trim().equalsIgnoreCase(expectedStatus.trim());
    }

    private List<String> mergeSequence(String sequence, List<String> defaults) {
        List<String> merged = new ArrayList<String>();
        if (sequence != null) {
            String[] tokens = sequence.split("[,\\n]");
            for (String token : tokens) {
                String cleaned = sanitize(token, null);
                if (cleaned == null || containsIgnoreCase(merged, cleaned)) {
                    continue;
                }
                merged.add(cleaned);
            }
        }
        for (String status : defaults) {
            if (status == null || containsIgnoreCase(merged, status)) {
                continue;
            }
            merged.add(status);
        }
        return merged;
    }

    private boolean containsIgnoreCase(List<String> values, String value) {
        if (values == null || value == null) {
            return false;
        }
        for (String existing : values) {
            if (existing != null && existing.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> mapRolesToStatuses(List<ScorecardWorkflowStage> stages) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (stages == null) {
            return map;
        }
        for (ScorecardWorkflowStage stage : stages) {
            if (stage == null || stage.getRoleKey() == null || stage.getStatusCode() == null) {
                continue;
            }
            String roleKey = sanitize(stage.getRoleKey(), null);
            String statusCode = sanitize(stage.getStatusCode(), null);
            if (roleKey == null || statusCode == null) {
                continue;
            }
            map.put(roleKey, statusCode);
        }
        return map;
    }

    private Map<String, String> mapStatusLabels(List<ScorecardWorkflowStage> stages) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (stages == null) {
            return map;
        }
        for (ScorecardWorkflowStage stage : stages) {
            if (stage == null || stage.getStatusCode() == null) {
                continue;
            }
            String statusCode = sanitize(stage.getStatusCode(), null);
            if (statusCode == null) {
                continue;
            }
            String statusLabel = stage.getStatusLabel() == null ? "" : stage.getStatusLabel().trim();
            if (statusLabel.isEmpty()) {
                statusLabel = defaultStatusLabel(statusCode);
            }
            map.put(statusCode, statusLabel);
        }
        return map;
    }

    private Map<String, String> mapStatusStages(List<ScorecardWorkflowStage> stages) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (stages == null) {
            return map;
        }
        for (ScorecardWorkflowStage stage : stages) {
            if (stage == null || stage.getStatusCode() == null) {
                continue;
            }
            String statusCode = sanitize(stage.getStatusCode(), null);
            if (statusCode == null) {
                continue;
            }
            String stageName = stage.getName() == null ? "" : stage.getName().trim();
            if (stageName.isEmpty()) {
                stageName = "Workflow";
            }
            map.put(statusCode, stageName);
        }
        return map;
    }

    private Map<String, String> mapStatusActionButtons(List<ScorecardWorkflowStage> stages) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (stages == null) {
            return map;
        }
        for (ScorecardWorkflowStage stage : stages) {
            if (stage == null || stage.getStatusCode() == null) {
                continue;
            }
            String statusCode = sanitize(stage.getStatusCode(), null);
            if (statusCode == null) {
                continue;
            }
            String buttonLabel = stage.getActionButtonLabel() == null ? "" : stage.getActionButtonLabel().trim();
            if (buttonLabel.isEmpty()) {
                buttonLabel = defaultStatusLabel(statusCode);
            }
            map.put(statusCode, buttonLabel);
        }
        return map;
    }

    private String defaultStatusLabel(String statusCode) {
        if (statusCode == null || statusCode.trim().isEmpty()) {
            return "Status";
        }
        String[] tokens = statusCode.trim().toLowerCase(Locale.ENGLISH).split("_");
        StringJoiner joiner = new StringJoiner(" ");
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            joiner.add(Character.toUpperCase(token.charAt(0)) + token.substring(1));
        }
        String label = joiner.toString().trim();
        return label.isEmpty() ? statusCode : label;
    }

    private String configuredStatus(Map<String, String> roleToStatus, String roleKey, String legacySetting, String fallback) {
        String configured = roleToStatus.get(sanitize(roleKey, null));
        if (configured != null && !configured.trim().isEmpty()) {
            return sanitize(configured, fallback);
        }
        return sanitize(legacySetting, fallback);
    }

    private List<String> mergeConfiguredStages(List<ScorecardWorkflowStage> stages, List<String> defaults) {
        List<String> ordered = new ArrayList<String>();
        if (stages != null) {
            for (ScorecardWorkflowStage stage : stages) {
                if (stage == null) {
                    continue;
                }
                String status = sanitize(stage.getStatusCode(), null);
                if (status == null || containsIgnoreCase(ordered, status)) {
                    continue;
                }
                ordered.add(status);
            }
        }
        for (String status : defaults) {
            if (status == null || containsIgnoreCase(ordered, status)) {
                continue;
            }
            ordered.add(status);
        }
        return ordered;
    }

    private String sanitize(String value, String fallback) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isEmpty()) {
            cleaned = fallback == null ? "" : fallback.trim();
        }
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.toUpperCase(Locale.ENGLISH);
    }
}
