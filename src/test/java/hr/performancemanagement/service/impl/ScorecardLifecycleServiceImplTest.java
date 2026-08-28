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
import hr.performancemanagement.utils.constants.PMConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScorecardLifecycleServiceImplTest {

    @Mock
    private ScoreCardRepository scoreCardRepository;
    @Mock
    private ReportingPeriodRepository reportingPeriodRepository;
    @Mock
    private ReportingDateRepository reportingDateRepository;
    @Mock
    private ScorecardReportingDateStageRepository scorecardReportingDateStageRepository;
    @Mock
    private ScorecardWorkflowStageRepository scorecardWorkflowStageRepository;

    private ScorecardLifecycleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScorecardLifecycleServiceImpl(
                scoreCardRepository,
                reportingPeriodRepository,
                reportingDateRepository,
                scorecardReportingDateStageRepository,
                scorecardWorkflowStageRepository
        );
    }

    @Test
    void prepareScorecardForSaveRejectsActiveScorecardUnderInactivePeriod() {
        ReportingPeriod requestedPeriod = reportingPeriod(10L, PMConstants.STATUS_ACTIVE, 3L);
        ReportingPeriod inactivePeriod = reportingPeriod(10L, PMConstants.STATUS_IN_ACTIVE, 3L);
        when(reportingPeriodRepository.findReportingPeriodById(10L)).thenReturn(inactivePeriod);

        Scorecard scorecard = new Scorecard();
        scorecard.setStatus(PMConstants.STATUS_ACTIVE);
        scorecard.setLockStatus(PMConstants.LOCK_STATUS_OPEN);
        scorecard.setReportingPeriod(requestedPeriod);

        InvalidWorkflowStateException exception = assertThrows(
                InvalidWorkflowStateException.class,
                () -> service.prepareScorecardForSave(scorecard)
        );

        assertTrue(exception.getMessage().contains("selected reporting period is not active"));
    }

    @Test
    void closeScorecardsForInactiveReportingPeriodClosesActiveScorecardsAndReportingDateStages() {
        ReportingPeriod reportingPeriod = reportingPeriod(10L, PMConstants.STATUS_IN_ACTIVE, 3L);
        Scorecard activeScorecard = new Scorecard();
        activeScorecard.setStatus(PMConstants.STATUS_ACTIVE);
        activeScorecard.setLockStatus(PMConstants.LOCK_STATUS_OPEN);
        activeScorecard.setReportingPeriod(reportingPeriod);
        activeScorecard.setClientId(3L);

        ReportingDate reportingDate = new ReportingDate();
        reportingDate.setId(20L);
        reportingDate.setReportingPeriod(reportingPeriod);

        ScorecardReportingDateStage stage = new ScorecardReportingDateStage();
        stage.setStatus(PMConstants.STATUS_ACTIVE);

        ScorecardWorkflowStage closedStage = new ScorecardWorkflowStage();
        closedStage.setRoleKey(PMConstants.SCORECARD_STAGE_CLOSED);
        closedStage.setStatusCode(PMConstants.APPROVAL_STATUS_CLOSED);

        List<ReportingDate> reportingDates = Collections.singletonList(reportingDate);
        when(reportingPeriodRepository.findReportingPeriodById(10L)).thenReturn(reportingPeriod);
        when(scorecardWorkflowStageRepository.findScorecardWorkflowStageByClientIdAndRoleKey(3L, PMConstants.SCORECARD_STAGE_CLOSED)).thenReturn(closedStage);
        when(scoreCardRepository.findScorecardsByReportingPeriod(reportingPeriod)).thenReturn(Collections.singletonList(activeScorecard));
        when(reportingDateRepository.findReportingDatesByReportingPeriod(reportingPeriod)).thenReturn(reportingDates);
        when(scorecardReportingDateStageRepository.findScorecardReportingDateStagesByReportingDateInAndStatusIn(eq(reportingDates), anyList()))
                .thenReturn(Collections.singletonList(stage));

        int closedCount = service.closeScorecardsForInactiveReportingPeriod(reportingPeriod);

        assertEquals(1, closedCount);
        assertEquals(PMConstants.STATUS_IN_ACTIVE, activeScorecard.getStatus());
        assertEquals(PMConstants.LOCK_STATUS_OPEN, activeScorecard.getLockStatus());
        assertEquals(PMConstants.APPROVAL_STATUS_CLOSED, activeScorecard.getApprovalStatus());
        assertEquals(closedStage, activeScorecard.getApprovalStage());
        assertEquals(PMConstants.STATUS_IN_ACTIVE, stage.getStatus());
        assertEquals(closedStage, stage.getApprovalStage());
        verify(scoreCardRepository).save(activeScorecard);
        verify(scorecardReportingDateStageRepository).save(stage);
    }

    @Test
    void applyStatusTransitionClosesScorecardWhenApprovalStatusIsClosed() {
        ReportingPeriod reportingPeriod = reportingPeriod(10L, PMConstants.STATUS_ACTIVE, 3L);
        ScorecardWorkflowStage closedStage = new ScorecardWorkflowStage();
        closedStage.setRoleKey(PMConstants.SCORECARD_STAGE_CLOSED);
        closedStage.setStatusCode(PMConstants.APPROVAL_STATUS_CLOSED);
        when(scorecardWorkflowStageRepository.findScorecardWorkflowStageByClientIdAndRoleKey(3L, PMConstants.SCORECARD_STAGE_CLOSED)).thenReturn(closedStage);

        Scorecard scorecard = new Scorecard();
        scorecard.setStatus(PMConstants.STATUS_ACTIVE);
        scorecard.setLockStatus(PMConstants.LOCK_STATUS_OPEN);
        scorecard.setReportingPeriod(reportingPeriod);
        scorecard.setClientId(3L);

        service.applyStatusTransition(scorecard, null, PMConstants.APPROVAL_STATUS_CLOSED);

        assertEquals(PMConstants.STATUS_IN_ACTIVE, scorecard.getStatus());
        assertEquals(PMConstants.LOCK_STATUS_OPEN, scorecard.getLockStatus());
        assertEquals(PMConstants.APPROVAL_STATUS_CLOSED, scorecard.getApprovalStatus());
        assertEquals(closedStage, scorecard.getApprovalStage());
    }

    @Test
    void prepareScorecardForSaveDoesNotCloseScorecardFromLegacyLockStatus() {
        ReportingPeriod reportingPeriod = reportingPeriod(10L, PMConstants.STATUS_ACTIVE, 3L);
        when(reportingPeriodRepository.findReportingPeriodById(10L)).thenReturn(reportingPeriod);

        Scorecard scorecard = new Scorecard();
        scorecard.setStatus(PMConstants.STATUS_ACTIVE);
        scorecard.setLockStatus(PMConstants.LOCK_STATUS_CLOSED);
        scorecard.setReportingPeriod(reportingPeriod);
        scorecard.setClientId(3L);

        service.prepareScorecardForSave(scorecard);

        assertEquals(PMConstants.STATUS_ACTIVE, scorecard.getStatus());
        assertEquals(PMConstants.LOCK_STATUS_CLOSED, scorecard.getLockStatus());
    }

    private ReportingPeriod reportingPeriod(long id, String status, long clientId) {
        ReportingPeriod reportingPeriod = new ReportingPeriod();
        reportingPeriod.setId(id);
        reportingPeriod.setClientId(clientId);
        reportingPeriod.setStatus(status);
        reportingPeriod.setStartDate(LocalDate.now().minusDays(1).toString());
        reportingPeriod.setEndDate(LocalDate.now().plusDays(1).toString());
        reportingPeriod.setModel("standard");
        return reportingPeriod;
    }
}
