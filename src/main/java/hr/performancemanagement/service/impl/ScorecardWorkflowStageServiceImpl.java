package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ScorecardWorkflowStage;
import hr.performancemanagement.repository.ScorecardWorkflowStageRepository;
import hr.performancemanagement.service.api.ScorecardWorkflowStageService;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ScorecardWorkflowStageServiceImpl implements ScorecardWorkflowStageService {

    private final ScorecardWorkflowStageRepository repository;
    private final HttpSession session;

    public ScorecardWorkflowStageServiceImpl(ScorecardWorkflowStageRepository repository, HttpSession session) {
        this.repository = repository;
        this.session = session;
    }

    @Override
    public List<ScorecardWorkflowStage> listAllWorkflowStages() {
        long clientId = getClientId();
        if (clientId <= 0) {
            return new ArrayList<ScorecardWorkflowStage>();
        }
        ensureDefaultStages(clientId);
        return sortStages(repository.findScorecardWorkflowStagesByClientIdOrderByStageOrderAsc(clientId));
    }

    @Override
    public List<ScorecardWorkflowStage> listActiveWorkflowStages() {
        long clientId = getClientId();
        if (clientId <= 0) {
            return new ArrayList<ScorecardWorkflowStage>();
        }
        ensureDefaultStages(clientId);
        return sortStages(repository.findScorecardWorkflowStagesByClientIdAndStatusOrderByStageOrderAsc(clientId, PMConstants.STATUS_ACTIVE));
    }

    @Override
    public ScorecardWorkflowStage getWorkflowStageById(long id) {
        long clientId = getClientId();
        if (clientId <= 0) {
            return null;
        }
        return repository.findScorecardWorkflowStageByIdAndClientId(id, clientId);
    }

    @Override
    public ScorecardWorkflowStage saveWorkflowStage(ScorecardWorkflowStage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Workflow stage cannot be null");
        }

        long clientId = getClientId();
        if (clientId <= 0) {
            throw new IllegalStateException("Unable to resolve client context");
        }

        normalize(stage);
        validate(stage, clientId);

        if (stage.getClientId() <= 0) {
            stage.setClientId(clientId);
        }
        if (stage.getStatus() == null || stage.getStatus().trim().isEmpty()) {
            stage.setStatus(PMConstants.STATUS_ACTIVE);
        }

        return repository.save(stage);
    }

    @Override
    public void deactivateWorkflowStage(long id) {
        ScorecardWorkflowStage stage = getWorkflowStageById(id);
        if (stage != null) {
            stage.setStatus(PMConstants.STATUS_IN_ACTIVE);
            repository.save(stage);
        }
    }

    @Override
    public void deleteWorkflowStage(long id) {
        ScorecardWorkflowStage stage = getWorkflowStageById(id);
        if (stage != null) {
            repository.delete(stage);
        }
    }

    @Override
    public List<String> listWorkflowStages() {
        return Arrays.asList(
                PMConstants.SCORECARD_STAGE_NEW,
                PMConstants.SCORECARD_STAGE_CAPTURE_TARGETS,
                PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR,
                PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR,
                PMConstants.SCORECARD_STAGE_OWNER_SCORING,
                PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL,
                PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING,
                PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING,
                PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL,
                PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING,
                PMConstants.SCORECARD_STAGE_CLOSED
        );
    }

    private void ensureDefaultStages(long clientId) {
        List<ScorecardWorkflowStage> activeStages = repository.findScorecardWorkflowStagesByClientIdAndStatusOrderByStageOrderAsc(clientId, PMConstants.STATUS_ACTIVE);
        if (activeStages != null && !activeStages.isEmpty()) {
            return;
        }

        // Stage 1: SCORECARD_STAGE_NEW - Scorecard just created
        saveDefaultStage(clientId, 1, PMConstants.SCORECARD_STAGE_NEW, PMConstants.APPROVAL_STATUS_NEW, "New", "New", "Start Capturing Targets", null);

        // Stage 2: SCORECARD_STAGE_CAPTURE_TARGETS - Capturing targets
        saveDefaultStage(clientId, 2, PMConstants.SCORECARD_STAGE_CAPTURE_TARGETS, PMConstants.APPROVAL_STATUS_PENDING_APPROVAL, "Capture Targets", "Pending", "Submit for Approval", null);

        // Stage 3: SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR - Supervisor approval with multiple statuses
        saveDefaultStage(clientId, 3, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR, PMConstants.APPROVAL_STATUS_PENDING_APPROVAL, "Targets Approval by Supervisor", "Pending Approval", "Approve", "Reject");
        saveDefaultStage(clientId, 3, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR, PMConstants.APPROVAL_STATUS_APPROVED_BY_SUPERVISOR, "Targets Approval by Supervisor", "Approved", "Move to HR Approval", null);
        saveDefaultStage(clientId, 3, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_SUPERVISOR, PMConstants.APPROVAL_STATUS_REJECTED_BY_SUPERVISOR, "Targets Approval by Supervisor", "Rejected", "Revise Targets", null);

        // Stage 4: SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR - HR approval with multiple statuses
        saveDefaultStage(clientId, 4, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR, PMConstants.APPROVAL_STATUS_PENDING_APPROVAL, "Targets Approval by HR", "Pending Approval", "Approve", "Reject");
        saveDefaultStage(clientId, 4, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR, PMConstants.APPROVAL_STATUS_APPROVED_BY_HR, "Targets Approval by HR", "Approved", "Start Owner Scoring", null);
        saveDefaultStage(clientId, 4, PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR, PMConstants.APPROVAL_STATUS_REJECTED_BY_HR, "Targets Approval by HR", "Rejected", "Send Back", null);

        // Stage 5: SCORECARD_STAGE_OWNER_SCORING - Employee self-assessment
        saveDefaultStage(clientId, 5, PMConstants.SCORECARD_STAGE_OWNER_SCORING, PMConstants.APPROVAL_STATUS_SCORED_BY_EMPLOYEE, "Owner Scoring", "Pending", "Submit Scores", null);

        // Stage 6: SCORECARD_STAGE_OWNER_SCORE_APPROVAL - Owner score approval with multiple statuses
        saveDefaultStage(clientId, 6, PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL, PMConstants.APPROVAL_STATUS_PENDING_APPROVAL, "Owner Score Approval", "Pending Approval", "Approve", "Reject");
        saveDefaultStage(clientId, 6, PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL, PMConstants.APPROVAL_STATUS_APPROVED_OWNER_SCORES, "Owner Score Approval", "Approved", "Start Supervisor Scoring", null);

        // Stage 7: SCORECARD_STAGE_SUPERVISOR_SCORING - Supervisor scoring
        saveDefaultStage(clientId, 7, PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING, PMConstants.APPROVAL_STATUS_SCORED_BY_SUPERVISOR, "Supervisor Scoring", "Pending", "Submit Scores", null);

        // Stage 8: SCORECARD_STAGE_AGREED_SCORE_CAPTURING - Capturing agreed scores
        saveDefaultStage(clientId, 8, PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING, PMConstants.APPROVAL_STATUS_AGREED_BY_TWO, "Agreed Score Capturing", "Pending", "Submit Agreed Scores", null);

        // Stage 9: SCORECARD_STAGE_AGREED_SCORE_APPROVAL - Agreed score approval with multiple statuses
        saveDefaultStage(clientId, 9, PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL, PMConstants.APPROVAL_STATUS_PENDING_APPROVAL, "Agreed Score Approval", "Pending Approval", "Approve", "Reject");
        saveDefaultStage(clientId, 9, PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL, PMConstants.APPROVAL_STATUS_APPROVED_AGREED_SCORES, "Agreed Score Approval", "Approved", "Start Moderation", null);

        // Stage 10: SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING - Moderator scoring
        saveDefaultStage(clientId, 10, PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING, PMConstants.APPROVAL_STATUS_MODERATED_BY_HR, "Moderator Score Capturing", "Pending", "Submit Moderated Scores", null);

        // Stage 11: SCORECARD_STAGE_CLOSED - Closed
        saveDefaultStage(clientId, 11, PMConstants.SCORECARD_STAGE_CLOSED, PMConstants.APPROVAL_STATUS_CLOSED, "Closed", "Closed", "Closed", null);
    }

    private void saveDefaultStage(long clientId, int order, String roleKey, String statusCode, String stageName, String statusLabel, String actionButtonLabel, String rejectionButtonLabel) {
        ScorecardWorkflowStage stage = new ScorecardWorkflowStage();
        stage.setClientId(clientId);
        stage.setStageOrder(order);
        stage.setRoleKey(roleKey);
        stage.setStatusCode(statusCode);
        stage.setName(stageName);
        stage.setStatusLabel(statusLabel);
        stage.setActionButtonLabel(actionButtonLabel);
        stage.setRejectionButtonLabel(rejectionButtonLabel);
        stage.setStatus(PMConstants.STATUS_ACTIVE);
        repository.save(stage);
    }

    private void normalize(ScorecardWorkflowStage stage) {
        if (stage.getRoleKey() != null) {
            stage.setRoleKey(stage.getRoleKey().trim().toUpperCase(Locale.ENGLISH));
        }
        if (stage.getStatusCode() != null) {
            stage.setStatusCode(stage.getStatusCode().trim().toUpperCase(Locale.ENGLISH));
        }
        if (stage.getStatusCodes() != null) {
            stage.setStatusCodes(stage.getStatusCodes().trim().toUpperCase(Locale.ENGLISH));
        }
        if (stage.getName() != null) {
            stage.setName(stage.getName().trim());
        }
        if (stage.getStatusLabel() != null) {
            stage.setStatusLabel(stage.getStatusLabel().trim());
        }
        if (stage.getActionButtonLabel() != null) {
            stage.setActionButtonLabel(stage.getActionButtonLabel().trim());
        }
        if (stage.getRejectionButtonLabel() != null) {
            stage.setRejectionButtonLabel(stage.getRejectionButtonLabel().trim());
        }
        if (stage.getStatus() != null) {
            stage.setStatus(stage.getStatus().trim().toUpperCase(Locale.ENGLISH));
        }
    }

    private void validate(ScorecardWorkflowStage candidate, long clientId) {
        if (candidate.getStageOrder() == null || candidate.getStageOrder() <= 0) {
            throw new IllegalArgumentException("Stage order must be a positive number");
        }
        if (candidate.getRoleKey() == null || candidate.getRoleKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow role is required");
        }
        List<String> validRoles = new ArrayList<String>();
        validRoles.addAll(Arrays.asList(
                PMConstants.SCORECARD_WORKFLOW_ROLE_NEW,
                PMConstants.SCORECARD_WORKFLOW_ROLE_NONE,
                PMConstants.SCORECARD_WORKFLOW_ROLE_PENDING_APPROVAL,
                PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_BY_SUPERVISOR,
                PMConstants.SCORECARD_WORKFLOW_ROLE_REJECTED_BY_SUPERVISOR,
                PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_BY_HR,
                PMConstants.SCORECARD_WORKFLOW_ROLE_REJECTED_BY_HR,
                PMConstants.SCORECARD_WORKFLOW_ROLE_SCORED_BY_EMPLOYEE,
                PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_OWNER_SCORES,
                PMConstants.SCORECARD_WORKFLOW_ROLE_SCORED_BY_SUPERVISOR,
                PMConstants.SCORECARD_WORKFLOW_ROLE_AGREED_BY_TWO,
                PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_AGREED_SCORES,
                PMConstants.SCORECARD_WORKFLOW_ROLE_MODERATED_BY_HR,
                PMConstants.SCORECARD_WORKFLOW_ROLE_CLOSED
        ));
        validRoles.addAll(listWorkflowStages());
        if (!validRoles.contains(candidate.getRoleKey())) {
            throw new IllegalArgumentException("Unknown workflow role: " + candidate.getRoleKey());
        }
        // Status codes validation - either single statusCode or multi statusCodes must be provided
        boolean hasStatusCode = candidate.getStatusCode() != null && !candidate.getStatusCode().trim().isEmpty();
        boolean hasStatusCodes = candidate.getStatusCodes() != null && !candidate.getStatusCodes().trim().isEmpty();
        if (!hasStatusCode && !hasStatusCodes) {
            throw new IllegalArgumentException("At least one status code is required (use Status Codes multi-select)");
        }
        if (candidate.getName() == null || candidate.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Stage name is required");
        }

        List<ScorecardWorkflowStage> stages = repository.findScorecardWorkflowStagesByClientIdOrderByStageOrderAsc(clientId);
        boolean candidateActive = PMConstants.STATUS_ACTIVE.equalsIgnoreCase(candidate.getStatus() == null ? PMConstants.STATUS_ACTIVE : candidate.getStatus());

        // STRICT VALIDATION: Each workflow role can only appear ONCE across all active stages
        // This ensures we don't have duplicate stages with the same workflow role
        for (ScorecardWorkflowStage existing : stages) {
            if (existing == null || existing.getId() == candidate.getId()) {
                continue;
            }

            // Check for duplicate workflow role (strict - only one stage per role allowed)
            if (existing.getRoleKey() != null
                    && existing.getRoleKey().equalsIgnoreCase(candidate.getRoleKey())
                    && PMConstants.STATUS_ACTIVE.equalsIgnoreCase(existing.getStatus())
                    && candidateActive) {
                throw new IllegalArgumentException("Workflow role '" + candidate.getRoleKey() + "' is already used in another active stage (ID: " + existing.getId() + ", Stage: " + existing.getName() + "). Each workflow role can only be used once.");
            }
        }
        validateGroupedStageConsistency(candidate, stages, candidateActive);
    }

    private void validateGroupedStageConsistency(ScorecardWorkflowStage candidate,
                                                 List<ScorecardWorkflowStage> stages,
                                                 boolean candidateActive) {
        if (!candidateActive || candidate == null || candidate.getRoleKey() == null) {
            return;
        }
        List<String> groupedRoles = groupedRolesFor(candidate.getRoleKey());
        if (groupedRoles.isEmpty()) {
            return;
        }

        Integer expectedOrder = null;
        String expectedStageName = null;
        for (ScorecardWorkflowStage existing : stages) {
            if (existing == null || existing.getId() == candidate.getId()) {
                continue;
            }
            if (!PMConstants.STATUS_ACTIVE.equalsIgnoreCase(existing.getStatus())) {
                continue;
            }
            if (existing.getRoleKey() == null || !containsIgnoreCase(groupedRoles, existing.getRoleKey())) {
                continue;
            }
            if (expectedOrder == null) {
                expectedOrder = existing.getStageOrder();
                expectedStageName = normalized(existing.getName());
                continue;
            }
            if (!safeEquals(expectedOrder, existing.getStageOrder())
                    || !safeEquals(expectedStageName, normalized(existing.getName()))) {
                throw new IllegalArgumentException("Workflow configuration for " + normalized(candidate.getRoleKey())
                        + " is inconsistent. Grouped statuses must share one stage.");
            }
        }

        if (expectedOrder != null && !safeEquals(expectedOrder, candidate.getStageOrder())) {
            throw new IllegalArgumentException("Grouped statuses must share the same stage sequence.");
        }
        if (expectedStageName != null && !safeEquals(expectedStageName, normalized(candidate.getName()))) {
            throw new IllegalArgumentException("Grouped statuses must share the same stage name.");
        }
    }

    private List<String> groupedRolesFor(String roleKey) {
        if (roleKey == null) {
            return new ArrayList<String>();
        }
        if (PMConstants.SCORECARD_WORKFLOW_ROLE_PENDING_APPROVAL.equalsIgnoreCase(roleKey)
                || PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_BY_SUPERVISOR.equalsIgnoreCase(roleKey)
                || PMConstants.SCORECARD_WORKFLOW_ROLE_REJECTED_BY_SUPERVISOR.equalsIgnoreCase(roleKey)) {
            return Arrays.asList(
                    PMConstants.SCORECARD_WORKFLOW_ROLE_PENDING_APPROVAL,
                    PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_BY_SUPERVISOR,
                    PMConstants.SCORECARD_WORKFLOW_ROLE_REJECTED_BY_SUPERVISOR
            );
        }
        if (PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_BY_HR.equalsIgnoreCase(roleKey)
                || PMConstants.SCORECARD_WORKFLOW_ROLE_REJECTED_BY_HR.equalsIgnoreCase(roleKey)) {
            return Arrays.asList(
                    PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_BY_HR,
                    PMConstants.SCORECARD_WORKFLOW_ROLE_REJECTED_BY_HR
            );
        }
        if (PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_OWNER_SCORES.equalsIgnoreCase(roleKey)
                || PMConstants.SCORECARD_WORKFLOW_ROLE_SCORED_BY_SUPERVISOR.equalsIgnoreCase(roleKey)) {
            return Arrays.asList(
                    PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_OWNER_SCORES,
                    PMConstants.SCORECARD_WORKFLOW_ROLE_SCORED_BY_SUPERVISOR
            );
        }
        if (PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_AGREED_SCORES.equalsIgnoreCase(roleKey)
                || PMConstants.SCORECARD_WORKFLOW_ROLE_MODERATED_BY_HR.equalsIgnoreCase(roleKey)) {
            return Arrays.asList(
                    PMConstants.SCORECARD_WORKFLOW_ROLE_APPROVED_AGREED_SCORES,
                    PMConstants.SCORECARD_WORKFLOW_ROLE_MODERATED_BY_HR
            );
        }
        return new ArrayList<String>();
    }

    private boolean containsIgnoreCase(List<String> values, String value) {
        if (values == null || value == null) {
            return false;
        }
        for (String candidate : values) {
            if (candidate != null && candidate.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH);
    }

    private boolean safeEquals(Object left, Object right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private List<ScorecardWorkflowStage> sortStages(List<ScorecardWorkflowStage> stages) {
        List<ScorecardWorkflowStage> sorted = stages == null
                ? new ArrayList<ScorecardWorkflowStage>()
                : new ArrayList<ScorecardWorkflowStage>(stages);
        sorted.sort(Comparator.comparing(ScorecardWorkflowStage::getStageOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparingLong(ScorecardWorkflowStage::getId));
        return sorted;
    }

    private long getClientId() {
        Object loggedUser = session.getAttribute("loggedUser");
        if (!(loggedUser instanceof Account)) {
            return 0;
        }
        return ((Account) loggedUser).getClientId();
    }
}
