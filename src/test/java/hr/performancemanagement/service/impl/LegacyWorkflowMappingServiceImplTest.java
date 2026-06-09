package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.*;
import hr.performancemanagement.repository.*;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.ScorecardWorkflowStageService;
import hr.performancemanagement.utils.constants.PMConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyWorkflowMappingServiceImplTest {

    @Mock private ScoreCardRepository scoreCardRepository;
    @Mock private ScorecardWorkflowStageRepository workflowStageRepository;
    @Mock private ReportingDateRepository reportingDateRepository;
    @Mock private ScorecardReportingDateStageRepository reportingDateStageRepository;
    @Mock private ScorecardWorkflowMappingAuditRepository auditRepository;
    @Mock private CommonService commonService;
    @Mock private ScorecardWorkflowStageService workflowStageService;

    private LegacyWorkflowMappingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LegacyWorkflowMappingServiceImpl(
                scoreCardRepository,
                workflowStageRepository,
                reportingDateRepository,
                reportingDateStageRepository,
                auditRepository,
                commonService,
                workflowStageService
        );
    }

    @Test
    void mapsUnmappedScorecardAndInitializesMissingOpenReportingDateStage() {
        Account actor = account(1L, 7L);
        Scorecard scorecard = scorecard(10L, 7L);
        ScorecardWorkflowStage newStage = stage(20L, 7L);
        ReportingDate reportingDate = reportingDate(30L, scorecard.getReportingPeriod());

        when(commonService.getLoggedUser()).thenReturn(actor);
        when(commonService.isAdmin()).thenReturn(true);
        when(workflowStageRepository.findScorecardWorkflowStageByIdAndClientId(20L, 7L)).thenReturn(newStage);
        when(scoreCardRepository.findAllById(any())).thenReturn(Collections.singletonList(scorecard));
        when(reportingDateRepository.findReportingDatesByReportingPeriodAndStatusIn(
                scorecard.getReportingPeriod(),
                java.util.Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
        )).thenReturn(Collections.singletonList(reportingDate));
        when(reportingDateStageRepository.findScorecardReportingDateStageByScorecardAndReportingDate(scorecard, reportingDate))
                .thenReturn(null);

        int mapped = service.saveWorkflowStageMappings(
                Collections.singletonList(scorecard.getId()),
                newStage.getId(),
                true,
                "Mapped before UAT"
        );

        assertEquals(1, mapped);
        assertEquals(newStage, scorecard.getApprovalStage());
        verify(scoreCardRepository).save(scorecard);

        ArgumentCaptor<ScorecardReportingDateStage> datedStageCaptor =
                ArgumentCaptor.forClass(ScorecardReportingDateStage.class);
        verify(reportingDateStageRepository).save(datedStageCaptor.capture());
        assertEquals(newStage, datedStageCaptor.getValue().getApprovalStage());
        assertEquals(reportingDate, datedStageCaptor.getValue().getReportingDate());

        ArgumentCaptor<ScorecardWorkflowMappingAudit> auditCaptor =
                ArgumentCaptor.forClass(ScorecardWorkflowMappingAudit.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertEquals("Mapped before UAT", auditCaptor.getValue().getReason());
        assertEquals(1, auditCaptor.getValue().getReportingDateStagesInitialized());
    }

    @Test
    void refusesToRemapScorecardThatAlreadyHasAWorkflowStage() {
        Account actor = account(1L, 7L);
        ScorecardWorkflowStage existingStage = stage(19L, 7L);
        ScorecardWorkflowStage requestedStage = stage(20L, 7L);
        Scorecard scorecard = scorecard(10L, 7L);
        scorecard.setApprovalStage(existingStage);

        when(commonService.getLoggedUser()).thenReturn(actor);
        when(commonService.isAdmin()).thenReturn(true);
        when(workflowStageRepository.findScorecardWorkflowStageByIdAndClientId(20L, 7L)).thenReturn(requestedStage);
        when(scoreCardRepository.findAllById(any())).thenReturn(Collections.singletonList(scorecard));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveWorkflowStageMappings(
                        Collections.singletonList(scorecard.getId()),
                        requestedStage.getId(),
                        false,
                        "Attempted remap"
                )
        );

        assertTrue(exception.getMessage().contains("already mapped"));
    }

    @Test
    void doesNotOverwriteExistingReportingDateWorkflowProgress() {
        Account actor = account(1L, 7L);
        Scorecard scorecard = scorecard(10L, 7L);
        ScorecardWorkflowStage newStage = stage(20L, 7L);
        ScorecardWorkflowStage existingDatedStage = stage(19L, 7L);
        ReportingDate reportingDate = reportingDate(30L, scorecard.getReportingPeriod());
        ScorecardReportingDateStage datedStage = new ScorecardReportingDateStage();
        datedStage.setScorecard(scorecard);
        datedStage.setReportingDate(reportingDate);
        datedStage.setApprovalStage(existingDatedStage);

        when(commonService.getLoggedUser()).thenReturn(actor);
        when(commonService.isAdmin()).thenReturn(true);
        when(workflowStageRepository.findScorecardWorkflowStageByIdAndClientId(20L, 7L)).thenReturn(newStage);
        when(scoreCardRepository.findAllById(any())).thenReturn(Collections.singletonList(scorecard));
        when(reportingDateRepository.findReportingDatesByReportingPeriodAndStatusIn(
                scorecard.getReportingPeriod(),
                java.util.Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
        )).thenReturn(Collections.singletonList(reportingDate));
        when(reportingDateStageRepository.findScorecardReportingDateStageByScorecardAndReportingDate(scorecard, reportingDate))
                .thenReturn(datedStage);

        service.saveWorkflowStageMappings(
                Collections.singletonList(scorecard.getId()),
                newStage.getId(),
                true,
                "Map without changing dated progress"
        );

        assertEquals(existingDatedStage, datedStage.getApprovalStage());
        verify(reportingDateStageRepository, never()).save(any(ScorecardReportingDateStage.class));
        ArgumentCaptor<ScorecardWorkflowMappingAudit> auditCaptor =
                ArgumentCaptor.forClass(ScorecardWorkflowMappingAudit.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertEquals(0, auditCaptor.getValue().getReportingDateStagesInitialized());
    }

    @Test
    void rejectsNonAdministratorMappingAttempt() {
        Account actor = account(1L, 7L);
        when(commonService.getLoggedUser()).thenReturn(actor);
        when(commonService.isAdmin()).thenReturn(false);
        when(commonService.hasSpecialRights()).thenReturn(false);

        assertThrows(
                IllegalStateException.class,
                () -> service.saveWorkflowStageMappings(
                        Collections.singletonList(10L),
                        20L,
                        false,
                        "Unauthorized attempt"
                )
        );
    }

    private Account account(long id, long clientId) {
        Account account = new Account();
        account.setId(id);
        account.setClientId(clientId);
        return account;
    }

    private Scorecard scorecard(long id, long clientId) {
        ReportingPeriod reportingPeriod = new ReportingPeriod();
        reportingPeriod.setId(40L);
        reportingPeriod.setClientId(clientId);
        Scorecard scorecard = new Scorecard();
        scorecard.setId(id);
        scorecard.setClientId(clientId);
        scorecard.setReportingPeriod(reportingPeriod);
        return scorecard;
    }

    private ScorecardWorkflowStage stage(long id, long clientId) {
        ScorecardWorkflowStage stage = new ScorecardWorkflowStage();
        stage.setId(id);
        stage.setClientId(clientId);
        stage.setName("Owner Scoring");
        stage.setRoleKey(PMConstants.SCORECARD_STAGE_OWNER_SCORING);
        stage.setStatusCode(PMConstants.APPROVAL_STATUS_APPROVED_BY_HR);
        stage.setStatus(PMConstants.STATUS_ACTIVE);
        return stage;
    }

    private ReportingDate reportingDate(long id, ReportingPeriod reportingPeriod) {
        ReportingDate reportingDate = new ReportingDate();
        reportingDate.setId(id);
        reportingDate.setReportingPeriod(reportingPeriod);
        reportingDate.setStatus(PMConstants.REPORTING_DATE_STATUS_OPEN);
        return reportingDate;
    }
}
