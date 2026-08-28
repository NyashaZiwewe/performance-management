package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.entities.ScorecardReportingDateStage;
import hr.performancemanagement.entities.ScorecardWorkflowStage;
import hr.performancemanagement.exception.custom.InvalidWorkflowStateException;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.repository.ReportingPeriodRepository;
import hr.performancemanagement.repository.ScoreCardRepository;
import hr.performancemanagement.repository.ScorecardReportingDateStageRepository;
import hr.performancemanagement.repository.ScorecardWorkflowStageRepository;
import hr.performancemanagement.service.api.ScorecardLifecycleService;
import hr.performancemanagement.utils.constants.PMConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class ScorecardLifecycleServiceImpl implements ScorecardLifecycleService {

    private final ScoreCardRepository scoreCardRepository;
    private final ReportingPeriodRepository reportingPeriodRepository;
    private final ReportingDateRepository reportingDateRepository;
    private final ScorecardReportingDateStageRepository scorecardReportingDateStageRepository;
    private final ScorecardWorkflowStageRepository scorecardWorkflowStageRepository;

    public ScorecardLifecycleServiceImpl(ScoreCardRepository scoreCardRepository,
                                         ReportingPeriodRepository reportingPeriodRepository,
                                         ReportingDateRepository reportingDateRepository,
                                         ScorecardReportingDateStageRepository scorecardReportingDateStageRepository,
                                         ScorecardWorkflowStageRepository scorecardWorkflowStageRepository) {
        this.scoreCardRepository = scoreCardRepository;
        this.reportingPeriodRepository = reportingPeriodRepository;
        this.reportingDateRepository = reportingDateRepository;
        this.scorecardReportingDateStageRepository = scorecardReportingDateStageRepository;
        this.scorecardWorkflowStageRepository = scorecardWorkflowStageRepository;
    }

    @Override
    public void prepareScorecardForSave(Scorecard scorecard) {
        if (scorecard == null) {
            throw new InvalidWorkflowStateException("Scorecard details are required.");
        }

        scorecard.setStatus(normalizeRecordStatus(scorecard.getStatus()));
        scorecard.setLockStatus(normalizeLegacyLockStatus(scorecard.getLockStatus()));
        if (scorecard.getReportingPeriod() != null && scorecard.getReportingPeriod().getId() > 0) {
            ReportingPeriod reportingPeriod = resolveReportingPeriod(scorecard.getReportingPeriod());
            scorecard.setReportingPeriod(reportingPeriod);
        }

        enforceScorecardConsistency(scorecard);
    }

    @Override
    public void applyStatusTransition(Scorecard scorecard, String recordStatus, String approvalStatus) {
        if (scorecard == null) {
            throw new InvalidWorkflowStateException("Scorecard details are required.");
        }
        if (StringUtils.hasText(recordStatus)) {
            scorecard.setStatus(normalizeRecordStatus(recordStatus));
        }
        if (StringUtils.hasText(approvalStatus)) {
            scorecard.setApprovalStatus(normalizeText(approvalStatus));
        }

        enforceScorecardConsistency(scorecard);
    }

    private void enforceScorecardConsistency(Scorecard scorecard) {
        String recordStatus = normalizeRecordStatus(scorecard.getStatus());
        String approvalStatus = normalizeText(scorecard.getApprovalStatus());

        scorecard.setStatus(recordStatus);
        scorecard.setLockStatus(normalizeLegacyLockStatus(scorecard.getLockStatus()));

        if (isTerminalRecordStatus(recordStatus)
                || PMConstants.APPROVAL_STATUS_CLOSED.equalsIgnoreCase(approvalStatus)) {
            closeScorecardFields(scorecard, resolveClosedStage(scorecard.getClientId()));
            return;
        }

        if (PMConstants.STATUS_ACTIVE.equalsIgnoreCase(scorecard.getStatus())) {
            validateActiveScorecardPeriod(scorecard.getReportingPeriod());
        }
    }

    @Override
    @Transactional
    public int closeScorecardsForInactiveReportingPeriod(ReportingPeriod reportingPeriod) {
        ReportingPeriod resolvedReportingPeriod = resolveReportingPeriod(reportingPeriod);
        if (resolvedReportingPeriod == null || resolvedReportingPeriod.getId() <= 0) {
            return 0;
        }

        ScorecardWorkflowStage closedStage = resolveClosedStage(resolvedReportingPeriod.getClientId());
        List<Scorecard> activeScorecards = activeScorecardsForPeriod(resolvedReportingPeriod);
        for (Scorecard scorecard : activeScorecards) {
            closeScorecardFields(scorecard, closedStage);
            scoreCardRepository.save(scorecard);
        }

        closeReportingDateStages(reportingDateRepository.findReportingDatesByReportingPeriod(resolvedReportingPeriod), closedStage);
        return activeScorecards.size();
    }

    private List<Scorecard> activeScorecardsForPeriod(ReportingPeriod reportingPeriod) {
        List<Scorecard> scorecards = scoreCardRepository.findScorecardsByReportingPeriod(reportingPeriod);
        List<Scorecard> activeScorecards = new ArrayList<>();
        if (scorecards == null || scorecards.isEmpty()) {
            return activeScorecards;
        }
        for (Scorecard scorecard : scorecards) {
            if (scorecard != null && PMConstants.STATUS_ACTIVE.equalsIgnoreCase(normalizeText(scorecard.getStatus()))) {
                activeScorecards.add(scorecard);
            }
        }
        return activeScorecards;
    }

    @Override
    @Transactional
    public int closeScorecardReportingDateStages(ReportingDate reportingDate) {
        ReportingDate resolvedReportingDate = resolveReportingDate(reportingDate);
        if (resolvedReportingDate == null || resolvedReportingDate.getId() <= 0) {
            return 0;
        }

        long clientId = 0;
        if (resolvedReportingDate.getReportingPeriod() != null) {
            clientId = resolvedReportingDate.getReportingPeriod().getClientId();
        }
        ScorecardWorkflowStage closedStage = resolveClosedStage(clientId);
        return closeReportingDateStages(Collections.singletonList(resolvedReportingDate), closedStage);
    }

    private void closeScorecardFields(Scorecard scorecard, ScorecardWorkflowStage closedStage) {
        if (scorecard == null) {
            return;
        }
        scorecard.setStatus(PMConstants.STATUS_IN_ACTIVE);
        scorecard.setLockStatus(normalizeLegacyLockStatus(scorecard.getLockStatus()));
        if (closedStage != null) {
            scorecard.setApprovalStage(closedStage);
        }
        scorecard.setApprovalStatus(PMConstants.APPROVAL_STATUS_CLOSED);
    }

    private int closeReportingDateStages(List<ReportingDate> reportingDates, ScorecardWorkflowStage closedStage) {
        if (reportingDates == null || reportingDates.isEmpty()) {
            return 0;
        }

        List<ScorecardReportingDateStage> stages =
                scorecardReportingDateStageRepository.findScorecardReportingDateStagesByReportingDateInAndStatusIn(
                        reportingDates,
                        Arrays.asList(PMConstants.STATUS_ACTIVE, PMConstants.REPORTING_DATE_STATUS_OPEN)
                );
        for (ScorecardReportingDateStage stage : stages) {
            if (stage == null) {
                continue;
            }
            stage.setStatus(PMConstants.STATUS_IN_ACTIVE);
            if (closedStage != null) {
                stage.setApprovalStage(closedStage);
            }
            scorecardReportingDateStageRepository.save(stage);
        }
        return stages.size();
    }

    private void validateActiveScorecardPeriod(ReportingPeriod reportingPeriod) {
        ReportingPeriod resolvedReportingPeriod = resolveReportingPeriod(reportingPeriod);
        if (resolvedReportingPeriod == null) {
            throw new InvalidWorkflowStateException("A scorecard must be linked to a reporting period before it can be active.");
        }
        if (!PMConstants.STATUS_ACTIVE.equalsIgnoreCase(normalizeText(resolvedReportingPeriod.getStatus()))) {
            throw new InvalidWorkflowStateException("This scorecard cannot be active because the selected reporting period is not active.");
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = parsePeriodDate(resolvedReportingPeriod.getStartDate(), "start");
        LocalDate endDate = parsePeriodDate(resolvedReportingPeriod.getEndDate(), "end");
        if (startDate.isAfter(endDate)) {
            throw new InvalidWorkflowStateException("This scorecard cannot be active because the reporting period start date is after the end date.");
        }
        if (today.isBefore(startDate) || today.isAfter(endDate)) {
            throw new InvalidWorkflowStateException(
                    "This scorecard cannot be active because the reporting period does not include today's date (" + today + ")."
            );
        }
    }

    private ReportingPeriod resolveReportingPeriod(ReportingPeriod reportingPeriod) {
        if (reportingPeriod == null || reportingPeriod.getId() <= 0) {
            return reportingPeriod;
        }
        ReportingPeriod resolvedReportingPeriod = reportingPeriodRepository.findReportingPeriodById(reportingPeriod.getId());
        return resolvedReportingPeriod == null ? reportingPeriod : resolvedReportingPeriod;
    }

    private ReportingDate resolveReportingDate(ReportingDate reportingDate) {
        if (reportingDate == null || reportingDate.getId() <= 0) {
            return reportingDate;
        }
        ReportingDate resolvedReportingDate = reportingDateRepository.findReportingDateById(reportingDate.getId());
        return resolvedReportingDate == null ? reportingDate : resolvedReportingDate;
    }

    private ScorecardWorkflowStage resolveClosedStage(long clientId) {
        if (clientId <= 0) {
            return null;
        }

        ScorecardWorkflowStage closedStage = scorecardWorkflowStageRepository.findScorecardWorkflowStageByClientIdAndRoleKey(
                clientId,
                PMConstants.SCORECARD_STAGE_CLOSED
        );
        if (closedStage != null) {
            return closedStage;
        }
        closedStage = scorecardWorkflowStageRepository.findScorecardWorkflowStageByClientIdAndRoleKey(
                clientId,
                PMConstants.SCORECARD_WORKFLOW_ROLE_CLOSED
        );
        if (closedStage != null) {
            return closedStage;
        }

        List<ScorecardWorkflowStage> activeStages =
                scorecardWorkflowStageRepository.findScorecardWorkflowStagesByClientIdAndStatusOrderByStageOrderAsc(
                        clientId,
                        PMConstants.STATUS_ACTIVE
                );
        if (activeStages == null) {
            return null;
        }
        for (ScorecardWorkflowStage stage : activeStages) {
            if (stage == null) {
                continue;
            }
            if (PMConstants.APPROVAL_STATUS_CLOSED.equalsIgnoreCase(normalizeText(stage.getStatusCode()))) {
                return stage;
            }
            if (containsStatus(stage.getStatusCodes(), PMConstants.APPROVAL_STATUS_CLOSED)) {
                return stage;
            }
        }
        return null;
    }

    private boolean containsStatus(String statuses, String expectedStatus) {
        if (!StringUtils.hasText(statuses) || !StringUtils.hasText(expectedStatus)) {
            return false;
        }
        String[] values = statuses.split(",");
        for (String value : values) {
            if (expectedStatus.equalsIgnoreCase(normalizeText(value))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeRecordStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return PMConstants.STATUS_ACTIVE;
        }
        String normalized = status.trim().toUpperCase(Locale.ENGLISH);
        if ("INACTIVE".equals(normalized)) {
            return PMConstants.STATUS_IN_ACTIVE;
        }
        return normalized;
    }

    private String normalizeLegacyLockStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return PMConstants.LOCK_STATUS_OPEN;
        }
        String normalized = status.trim().toUpperCase(Locale.ENGLISH);
        if (PMConstants.LOCK_STATUS_OPEN.equals(normalized)
                || PMConstants.LOCK_STATUS_LOCKED.equals(normalized)
                || PMConstants.LOCK_STATUS_CLOSED.equals(normalized)) {
            return normalized;
        }
        return PMConstants.LOCK_STATUS_OPEN;
    }

    private boolean isTerminalRecordStatus(String status) {
        String normalized = normalizeText(status);
        return normalized != null
                && !PMConstants.STATUS_ACTIVE.equalsIgnoreCase(normalized);
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ENGLISH);
    }

    private LocalDate parsePeriodDate(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidWorkflowStateException("This scorecard cannot be active because the reporting period " + label + " date is missing.");
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new InvalidWorkflowStateException("This scorecard cannot be active because the reporting period " + label + " date is invalid.");
        }
    }
}
