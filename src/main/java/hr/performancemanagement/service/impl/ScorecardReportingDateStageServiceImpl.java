package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.entities.ScorecardReportingDateStage;
import hr.performancemanagement.entities.ScorecardWorkflowStage;
import hr.performancemanagement.repository.ScorecardReportingDateStageRepository;
import hr.performancemanagement.repository.ScorecardWorkflowStageRepository;
import hr.performancemanagement.service.api.ScorecardReportingDateStageService;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ScorecardReportingDateStageServiceImpl implements ScorecardReportingDateStageService {

    private final ScorecardReportingDateStageRepository repository;
    private final ScorecardWorkflowStageRepository workflowStageRepository;

    public ScorecardReportingDateStageServiceImpl(
            ScorecardReportingDateStageRepository repository,
            ScorecardWorkflowStageRepository workflowStageRepository
    ) {
        this.repository = repository;
        this.workflowStageRepository = workflowStageRepository;
    }

    @Override
    @Transactional
    public ScorecardReportingDateStage getOrCreateStage(Scorecard scorecard, ReportingDate reportingDate) {
        if (scorecard == null || scorecard.getId() <= 0 || reportingDate == null || reportingDate.getId() <= 0) {
            return null;
        }

        ScorecardReportingDateStage stage = repository.findScorecardReportingDateStageByScorecardAndReportingDate(scorecard, reportingDate);
        if (stage != null) {
            if (stage.getApprovalStage() == null) {
                ScorecardWorkflowStage mapped = resolveStageByRoleKey(resolveClientId(scorecard), fallbackRoleKey(scorecard));
                if (mapped != null) {
                    stage.setApprovalStage(mapped);
                    stage = repository.save(stage);
                }
            }
            return stage;
        }

        ScorecardWorkflowStage defaultStage = resolveStageByRoleKey(resolveClientId(scorecard), fallbackRoleKey(scorecard));
        ScorecardReportingDateStage created = new ScorecardReportingDateStage();
        created.setClientId(resolveClientId(scorecard));
        created.setScorecard(scorecard);
        created.setReportingDate(reportingDate);
        created.setApprovalStage(defaultStage);
        created.setStatus(PMConstants.STATUS_ACTIVE);
        return repository.save(created);
    }

    @Override
    @Transactional
    public ScorecardReportingDateStage moveToRole(Scorecard scorecard, ReportingDate reportingDate, String roleKey) {
        if (scorecard == null || reportingDate == null) {
            return null;
        }
        ScorecardReportingDateStage stage = getOrCreateStage(scorecard, reportingDate);
        if (stage == null) {
            return null;
        }
        ScorecardWorkflowStage workflowStage = resolveStageByRoleKey(resolveClientId(scorecard), roleKey);
        if (workflowStage == null) {
            return stage;
        }
        stage.setApprovalStage(workflowStage);
        stage.setStatus(PMConstants.STATUS_ACTIVE);
        return repository.save(stage);
    }

    @Override
    @Transactional(readOnly = true)
    public String getCurrentRoleKey(Scorecard scorecard, ReportingDate reportingDate) {
        if (scorecard == null || reportingDate == null) {
            return null;
        }
        ScorecardReportingDateStage stage = repository.findScorecardReportingDateStageByScorecardAndReportingDate(scorecard, reportingDate);
        if (stage == null || stage.getApprovalStage() == null || stage.getApprovalStage().getRoleKey() == null) {
            return null;
        }
        return normalize(stage.getApprovalStage().getRoleKey());
    }

    private ScorecardWorkflowStage resolveStageByRoleKey(long clientId, String roleKey) {
        String normalizedRoleKey = normalize(roleKey);
        if (clientId <= 0 || normalizedRoleKey == null) {
            return null;
        }

        List<ScorecardWorkflowStage> activeStages = workflowStageRepository
                .findScorecardWorkflowStagesByClientIdAndStatusOrderByStageOrderAsc(clientId, PMConstants.STATUS_ACTIVE);
        if (activeStages != null) {
            for (ScorecardWorkflowStage stage : activeStages) {
                if (stage == null || stage.getRoleKey() == null) {
                    continue;
                }
                if (normalizedRoleKey.equals(normalize(stage.getRoleKey()))) {
                    return stage;
                }
            }
        }
        return null;
    }

    private String fallbackRoleKey(Scorecard scorecard) {
        if (scorecard != null && scorecard.getApprovalStage() != null && scorecard.getApprovalStage().getRoleKey() != null) {
            String currentRole = normalize(scorecard.getApprovalStage().getRoleKey());
            if (currentRole != null) {
                if (PMConstants.SCORECARD_STAGE_OWNER_SCORING.equals(currentRole)
                        || PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL.equals(currentRole)
                        || PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING.equals(currentRole)
                        || PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING.equals(currentRole)
                        || PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL.equals(currentRole)
                        || PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING.equals(currentRole)
                        || PMConstants.SCORECARD_STAGE_CLOSED.equals(currentRole)) {
                    return currentRole;
                }
            }
        }
        return mapStatusToRoleKey(scorecard == null ? null : scorecard.getApprovalStatus());
    }

    private String mapStatusToRoleKey(String approvalStatus) {
        String status = normalize(approvalStatus);
        if (status == null) {
            return PMConstants.SCORECARD_STAGE_OWNER_SCORING;
        }
        switch (status) {
            case PMConstants.APPROVAL_STATUS_APPROVED_BY_HR:
                return PMConstants.SCORECARD_STAGE_OWNER_SCORING;
            case PMConstants.APPROVAL_STATUS_SCORED_BY_EMPLOYEE:
                return PMConstants.SCORECARD_STAGE_OWNER_SCORE_APPROVAL;
            case PMConstants.APPROVAL_STATUS_APPROVED_OWNER_SCORES:
                return PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING;
            case PMConstants.APPROVAL_STATUS_SCORED_BY_SUPERVISOR:
                return PMConstants.SCORECARD_STAGE_AGREED_SCORE_CAPTURING;
            case PMConstants.APPROVAL_STATUS_AGREED_BY_TWO:
                return PMConstants.SCORECARD_STAGE_AGREED_SCORE_APPROVAL;
            case PMConstants.APPROVAL_STATUS_APPROVED_AGREED_SCORES:
                return PMConstants.SCORECARD_STAGE_MODERATOR_SCORE_CAPTURING;
            case PMConstants.APPROVAL_STATUS_MODERATED_BY_HR:
            case PMConstants.APPROVAL_STATUS_CLOSED:
                return PMConstants.SCORECARD_STAGE_CLOSED;
            default:
                return PMConstants.SCORECARD_STAGE_OWNER_SCORING;
        }
    }

    private long resolveClientId(Scorecard scorecard) {
        if (scorecard == null) {
            return 0;
        }
        if (scorecard.getClientId() > 0) {
            return scorecard.getClientId();
        }
        if (scorecard.getOwner() != null && scorecard.getOwner().getClientId() > 0) {
            return scorecard.getOwner().getClientId();
        }
        return 0;
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ENGLISH);
    }
}

