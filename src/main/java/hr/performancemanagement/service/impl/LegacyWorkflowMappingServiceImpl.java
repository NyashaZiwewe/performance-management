package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.*;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.LegacyWorkflowMappingService;
import hr.performancemanagement.service.api.ScorecardWorkflowStageService;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class LegacyWorkflowMappingServiceImpl implements LegacyWorkflowMappingService {

    private final ScoreCardRepository scoreCardRepository;
    private final ScorecardWorkflowStageRepository workflowStageRepository;
    private final ReportingDateRepository reportingDateRepository;
    private final ScorecardReportingDateStageRepository reportingDateStageRepository;
    private final ScorecardWorkflowMappingAuditRepository auditRepository;
    private final CommonService commonService;
    private final ScorecardWorkflowStageService workflowStageService;

    public LegacyWorkflowMappingServiceImpl(ScoreCardRepository scoreCardRepository,
                                            ScorecardWorkflowStageRepository workflowStageRepository,
                                            ReportingDateRepository reportingDateRepository,
                                            ScorecardReportingDateStageRepository reportingDateStageRepository,
                                            ScorecardWorkflowMappingAuditRepository auditRepository,
                                            CommonService commonService,
                                            ScorecardWorkflowStageService workflowStageService) {
        this.scoreCardRepository = scoreCardRepository;
        this.workflowStageRepository = workflowStageRepository;
        this.reportingDateRepository = reportingDateRepository;
        this.reportingDateStageRepository = reportingDateStageRepository;
        this.auditRepository = auditRepository;
        this.commonService = commonService;
        this.workflowStageService = workflowStageService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scorecard> listUnmappedScorecards() {
        Account actor = requireAdministrator();
        return scoreCardRepository.findUnmappedScorecardsByClientId(actor.getClientId());
    }

    @Override
    @Transactional
    public List<ScorecardWorkflowStage> listActiveWorkflowStages() {
        requireAdministrator();
        return workflowStageService.listActiveWorkflowStages();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScorecardWorkflowMappingAudit> listRecentAudit() {
        Account actor = requireAdministrator();
        return auditRepository.findTop100ByClientIdOrderByIdDesc(actor.getClientId());
    }

    @Override
    @Transactional
    public int saveWorkflowStageMappings(List<Long> scorecardIds,
                                         long workflowStageId,
                                         boolean initializeReportingDateStages,
                                         String reason) {
        Account actor = requireAdministrator();
        String resolvedReason = validateReason(reason);
        if (workflowStageId <= 0) {
            throw new IllegalArgumentException("Select a workflow stage.");
        }
        LinkedHashSet<Long> selectedIds = normalizeScorecardIds(scorecardIds);
        if (selectedIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one legacy scorecard.");
        }

        ScorecardWorkflowStage newStage =
                workflowStageRepository.findScorecardWorkflowStageByIdAndClientId(workflowStageId, actor.getClientId());
        if (newStage == null || !PMConstants.STATUS_ACTIVE.equalsIgnoreCase(newStage.getStatus())) {
            throw new IllegalArgumentException("Select an active workflow stage from your organisation.");
        }

        List<Scorecard> scorecards = scoreCardRepository.findAllById(selectedIds);
        if (scorecards.size() != selectedIds.size()) {
            throw new IllegalArgumentException("One or more selected scorecards could not be found.");
        }

        for (Scorecard scorecard : scorecards) {
            validateLegacyScorecard(scorecard, actor.getClientId());
        }

        for (Scorecard scorecard : scorecards) {
            ScorecardWorkflowStage previousStage = scorecard.getApprovalStage();
            scorecard.setApprovalStage(newStage);
            scoreCardRepository.save(scorecard);

            int initializedCount = initializeReportingDateStages
                    ? initializeMissingReportingDateStages(scorecard, newStage)
                    : 0;
            saveAudit(actor, scorecard, previousStage, newStage, resolvedReason,
                    initializeReportingDateStages, initializedCount);
        }
        return scorecards.size();
    }

    private int initializeMissingReportingDateStages(Scorecard scorecard, ScorecardWorkflowStage newStage) {
        if (scorecard.getReportingPeriod() == null) {
            return 0;
        }
        List<ReportingDate> reportingDates = reportingDateRepository.findReportingDatesByReportingPeriodAndStatusIn(
                scorecard.getReportingPeriod(),
                Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
        );
        int initialized = 0;
        for (ReportingDate reportingDate : reportingDates) {
            ScorecardReportingDateStage datedStage =
                    reportingDateStageRepository.findScorecardReportingDateStageByScorecardAndReportingDate(
                            scorecard,
                            reportingDate
                    );
            if (datedStage == null) {
                datedStage = new ScorecardReportingDateStage();
                datedStage.setClientId(scorecard.getClientId());
                datedStage.setScorecard(scorecard);
                datedStage.setReportingDate(reportingDate);
                datedStage.setApprovalStage(newStage);
                datedStage.setStatus(PMConstants.STATUS_ACTIVE);
                reportingDateStageRepository.save(datedStage);
                initialized++;
            } else if (datedStage.getApprovalStage() == null) {
                datedStage.setApprovalStage(newStage);
                if (!StringUtils.hasText(datedStage.getStatus())) {
                    datedStage.setStatus(PMConstants.STATUS_ACTIVE);
                }
                reportingDateStageRepository.save(datedStage);
                initialized++;
            }
        }
        return initialized;
    }

    private void saveAudit(Account actor,
                           Scorecard scorecard,
                           ScorecardWorkflowStage previousStage,
                           ScorecardWorkflowStage newStage,
                           String reason,
                           boolean initializeReportingDateStages,
                           int initializedCount) {
        ScorecardWorkflowMappingAudit audit = new ScorecardWorkflowMappingAudit();
        audit.setClientId(actor.getClientId());
        audit.setScorecard(scorecard);
        audit.setPreviousStage(previousStage);
        audit.setNewStage(newStage);
        audit.setReason(reason);
        audit.setInitializeReportingDateStages(initializeReportingDateStages);
        audit.setReportingDateStagesInitialized(initializedCount);
        audit.setActor(actor);
        auditRepository.save(audit);
    }

    private void validateLegacyScorecard(Scorecard scorecard, long clientId) {
        if (scorecard == null || scorecard.getClientId() != clientId) {
            throw new IllegalArgumentException("A selected scorecard does not belong to your organisation.");
        }
        if (scorecard.getApprovalStage() != null) {
            throw new IllegalArgumentException(
                    "Scorecard " + scorecard.getId() + " is already mapped. Use normal workflow actions to move it."
            );
        }
    }

    private LinkedHashSet<Long> normalizeScorecardIds(List<Long> scorecardIds) {
        LinkedHashSet<Long> selectedIds = new LinkedHashSet<>();
        if (scorecardIds == null) {
            return selectedIds;
        }
        for (Long scorecardId : scorecardIds) {
            if (scorecardId != null && scorecardId > 0) {
                selectedIds.add(scorecardId);
            }
        }
        return selectedIds;
    }

    private String validateReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("A reason is required for legacy workflow mapping.");
        }
        String resolvedReason = reason.trim();
        if (resolvedReason.length() > 1000) {
            throw new IllegalArgumentException("Reason cannot exceed 1000 characters.");
        }
        return resolvedReason;
    }

    private Account requireAdministrator() {
        Account actor = commonService.getLoggedUser();
        if (actor == null || actor.getClientId() <= 0) {
            throw new IllegalStateException("A logged-in administrator is required.");
        }
        if (!commonService.isAdmin() && !commonService.hasSpecialRights()) {
            throw new IllegalStateException("Only administrators can map legacy scorecards.");
        }
        return actor;
    }
}
