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
        Map<String, String> roleStageNames = mapRoleStageNames(stages);
        Map<String, String> roleActionButtons = mapRoleActionButtons(stages);
        Map<String, String> roleRejectionButtons = mapRoleRejectionButtons(stages);
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

        applyRoleDrivenStatusStages(statusStages, roleStageNames,
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
                closedStatus);

        applyRoleDrivenStatusActions(statusActionButtons, roleActionButtons,
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
                closedStatus);

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
                statusActionButtons,
                roleStageNames,
                roleActionButtons,
                roleRejectionButtons
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
            if (stage == null) {
                continue;
            }
            String statusLabel = stage.getStatusLabel() == null ? "" : stage.getStatusLabel().trim();
            for (String statusCode : extractStageStatusCodes(stage)) {
                if (statusCode == null || map.containsKey(statusCode)) {
                    continue;
                }
                String resolvedLabel = statusLabel.isEmpty() ? defaultStatusLabel(statusCode) : statusLabel;
                map.put(statusCode, resolvedLabel);
            }
        }
        return map;
    }

    private Map<String, String> mapStatusStages(List<ScorecardWorkflowStage> stages) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (stages == null) {
            return map;
        }
        for (ScorecardWorkflowStage stage : stages) {
            if (stage == null) {
                continue;
            }
            String stageName = stage.getName() == null ? "" : stage.getName().trim();
            if (stageName.isEmpty()) {
                stageName = "Workflow";
            }
            for (String statusCode : extractStageStatusCodes(stage)) {
                if (statusCode == null || map.containsKey(statusCode)) {
                    continue;
                }
                map.put(statusCode, stageName);
            }
        }
        return map;
    }

    private Map<String, String> mapStatusActionButtons(List<ScorecardWorkflowStage> stages) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (stages == null) {
            return map;
        }
        for (ScorecardWorkflowStage stage : stages) {
            if (stage == null) {
                continue;
            }
            String buttonLabel = stage.getActionButtonLabel() == null ? "" : stage.getActionButtonLabel().trim();
            for (String statusCode : extractStageStatusCodes(stage)) {
                if (statusCode == null || map.containsKey(statusCode)) {
                    continue;
                }
                String resolvedButtonLabel = buttonLabel.isEmpty() ? defaultStatusLabel(statusCode) : buttonLabel;
                map.put(statusCode, resolvedButtonLabel);
            }
        }
        return map;
    }

    private Map<String, String> mapRoleStageNames(List<ScorecardWorkflowStage> stages) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (stages == null) {
            return map;
        }
        for (ScorecardWorkflowStage stage : stages) {
            if (stage == null) {
                continue;
            }
            String roleKey = sanitize(stage.getRoleKey(), null);
            if (roleKey == null || map.containsKey(roleKey)) {
                continue;
            }
            String stageName = stage.getName() == null ? "" : stage.getName().trim();
            map.put(roleKey, stageName.isEmpty() ? "Workflow" : stageName);
        }
        return map;
    }

    private Map<String, String> mapRoleActionButtons(List<ScorecardWorkflowStage> stages) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (stages == null) {
            return map;
        }
        for (ScorecardWorkflowStage stage : stages) {
            if (stage == null) {
                continue;
            }
            String roleKey = sanitize(stage.getRoleKey(), null);
            if (roleKey == null || map.containsKey(roleKey)) {
                continue;
            }
            String actionLabel = stage.getActionButtonLabel() == null ? "" : stage.getActionButtonLabel().trim();
            if (!actionLabel.isEmpty()) {
                map.put(roleKey, actionLabel);
            }
        }
        return map;
    }

    private Map<String, String> mapRoleRejectionButtons(List<ScorecardWorkflowStage> stages) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (stages == null) {
            return map;
        }
        for (ScorecardWorkflowStage stage : stages) {
            if (stage == null) {
                continue;
            }
            String roleKey = sanitize(stage.getRoleKey(), null);
            if (roleKey == null || map.containsKey(roleKey)) {
                continue;
            }
            String rejectionLabel = stage.getRejectionButtonLabel() == null ? "" : stage.getRejectionButtonLabel().trim();
            if (!rejectionLabel.isEmpty()) {
                map.put(roleKey, rejectionLabel);
            }
        }
        return map;
    }

    private List<String> extractStageStatusCodes(ScorecardWorkflowStage stage) {
        List<String> statuses = new ArrayList<String>();
        if (stage == null) {
            return statuses;
        }
        String primary = sanitize(stage.getStatusCode(), null);
        if (primary != null && !containsIgnoreCase(statuses, primary)) {
            statuses.add(primary);
        }
        if (stage.getStatusCodes() != null) {
            String[] extraCodes = stage.getStatusCodes().split(",");
            for (String code : extraCodes) {
                String cleaned = sanitize(code, null);
                if (cleaned != null && !containsIgnoreCase(statuses, cleaned)) {
                    statuses.add(cleaned);
                }
            }
        }
        return statuses;
    }

    private void applyRoleDrivenStatusStages(Map<String, String> statusStages,
                                             Map<String, String> roleStageNames,
                                             String newStatus,
                                             String pendingApprovalStatus,
                                             String approvedBySupervisorStatus,
                                             String rejectedBySupervisorStatus,
                                             String approvedByHrStatus,
                                             String rejectedByHrStatus,
                                             String scoredByEmployeeStatus,
                                             String approvedOwnerScoresStatus,
                                             String scoredBySupervisorStatus,
                                             String agreedByTwoStatus,
                                             String approvedAgreedScoresStatus,
                                             String moderatedByHrStatus,
                                             String closedStatus) {
        applyRoleStage(statusStages, roleStageNames, newStatus, PMConstants.SCORECARD_STAGE_NEW);
        applyRoleStage(statusStages, roleStageNames, pendingApprovalStatus, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR);
        applyRoleStage(statusStages, roleStageNames, approvedBySupervisorStatus, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR);
        applyRoleStage(statusStages, roleStageNames, rejectedBySupervisorStatus, PMConstants.SCORECARD_STAGE_CAPTURE_TARGETS);
        applyRoleStage(statusStages, roleStageNames, approvedByHrStatus, PMConstants.SCORECARD_STAGE_OWNER_SCORING);
        applyRoleStage(statusStages, roleStageNames, rejectedByHrStatus, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR);
        applyRoleStage(statusStages, roleStageNames, scoredByEmployeeStatus, PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL);
        applyRoleStage(statusStages, roleStageNames, approvedOwnerScoresStatus, PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING);
        applyRoleStage(statusStages, roleStageNames, scoredBySupervisorStatus, PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING);
        applyRoleStage(statusStages, roleStageNames, agreedByTwoStatus, PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL);
        applyRoleStage(statusStages, roleStageNames, approvedAgreedScoresStatus, PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING);
        applyRoleStage(statusStages, roleStageNames, moderatedByHrStatus, PMConstants.SCORECARD_STAGE_CLOSED);
        applyRoleStage(statusStages, roleStageNames, closedStatus, PMConstants.SCORECARD_STAGE_CLOSED);
    }

    private void applyRoleDrivenStatusActions(Map<String, String> statusActions,
                                              Map<String, String> roleActionButtons,
                                              String newStatus,
                                              String pendingApprovalStatus,
                                              String approvedBySupervisorStatus,
                                              String rejectedBySupervisorStatus,
                                              String approvedByHrStatus,
                                              String rejectedByHrStatus,
                                              String scoredByEmployeeStatus,
                                              String approvedOwnerScoresStatus,
                                              String scoredBySupervisorStatus,
                                              String agreedByTwoStatus,
                                              String approvedAgreedScoresStatus,
                                              String moderatedByHrStatus,
                                              String closedStatus) {
        applyRoleAction(statusActions, roleActionButtons, newStatus, PMConstants.SCORECARD_STAGE_CAPTURE_TARGETS);
        applyRoleAction(statusActions, roleActionButtons, pendingApprovalStatus, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR);
        applyRoleAction(statusActions, roleActionButtons, approvedBySupervisorStatus, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR);
        applyRoleAction(statusActions, roleActionButtons, rejectedBySupervisorStatus, PMConstants.SCORECARD_STAGE_CAPTURE_TARGETS);
        applyRoleAction(statusActions, roleActionButtons, approvedByHrStatus, PMConstants.SCORECARD_STAGE_OWNER_SCORING);
        applyRoleAction(statusActions, roleActionButtons, rejectedByHrStatus, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR);
        applyRoleAction(statusActions, roleActionButtons, scoredByEmployeeStatus, PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL);
        applyRoleAction(statusActions, roleActionButtons, approvedOwnerScoresStatus, PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL);
        applyRoleAction(statusActions, roleActionButtons, scoredBySupervisorStatus, PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING);
        applyRoleAction(statusActions, roleActionButtons, agreedByTwoStatus, PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL);
        applyRoleAction(statusActions, roleActionButtons, approvedAgreedScoresStatus, PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL);
        applyRoleAction(statusActions, roleActionButtons, moderatedByHrStatus, PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING);
        applyRoleAction(statusActions, roleActionButtons, closedStatus, PMConstants.SCORECARD_STAGE_CLOSED);
    }

    private void applyRoleStage(Map<String, String> statusStages,
                                Map<String, String> roleStageNames,
                                String status,
                                String roleKey) {
        if (statusStages == null || roleStageNames == null || status == null || roleKey == null) {
            return;
        }
        String cleanedStatus = sanitize(status, null);
        String cleanedRole = sanitize(roleKey, null);
        if (cleanedStatus == null || cleanedRole == null) {
            return;
        }
        String roleStage = roleStageNames.get(cleanedRole);
        if (roleStage != null && !roleStage.trim().isEmpty()) {
            statusStages.put(cleanedStatus, roleStage.trim());
        }
    }

    private void applyRoleAction(Map<String, String> statusActions,
                                 Map<String, String> roleActionButtons,
                                 String status,
                                 String roleKey) {
        if (statusActions == null || roleActionButtons == null || status == null || roleKey == null) {
            return;
        }
        String cleanedStatus = sanitize(status, null);
        String cleanedRole = sanitize(roleKey, null);
        if (cleanedStatus == null || cleanedRole == null) {
            return;
        }
        String roleAction = roleActionButtons.get(cleanedRole);
        if (roleAction != null && !roleAction.trim().isEmpty()) {
            statusActions.put(cleanedStatus, roleAction.trim());
        }
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
