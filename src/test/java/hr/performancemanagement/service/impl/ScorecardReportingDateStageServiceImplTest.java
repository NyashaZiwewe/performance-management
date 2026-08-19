package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.entities.ScorecardReportingDateStage;
import hr.performancemanagement.entities.ScorecardWorkflowStage;
import hr.performancemanagement.repository.ScorecardReportingDateStageRepository;
import hr.performancemanagement.repository.ScorecardWorkflowStageRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScorecardReportingDateStageServiceImplTest {

    @Mock
    private ScorecardReportingDateStageRepository repository;

    @Mock
    private ScorecardWorkflowStageRepository workflowStageRepository;

    private ScorecardReportingDateStageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScorecardReportingDateStageServiceImpl(repository, workflowStageRepository);
    }

    @Test
    void getCurrentRoleKeySyncsStaleHrApprovalStageAfterContractApproval() {
        ScorecardWorkflowStage hrApprovalStage = stage(
                1L,
                PMConstants.SCORECARD_STAGE_TARGETS_APPROVAL_BY_HR,
                PMConstants.APPROVAL_STATUS_APPROVED_BY_SUPERVISOR
        );
        ScorecardWorkflowStage ownerScoringStage = stage(
                2L,
                PMConstants.SCORECARD_STAGE_OWNER_SCORING,
                PMConstants.APPROVAL_STATUS_APPROVED_BY_HR
        );
        Scorecard scorecard = scorecard(10L, ownerScoringStage);
        ReportingDate reportingDate = reportingDate(20L);
        ScorecardReportingDateStage reportingDateStage = reportingDateStage(scorecard, reportingDate, hrApprovalStage);

        when(repository.findScorecardReportingDateStageByScorecardAndReportingDate(scorecard, reportingDate))
                .thenReturn(reportingDateStage);
        when(workflowStageRepository.findScorecardWorkflowStagesByClientIdAndStatusOrderByStageOrderAsc(
                7L, PMConstants.STATUS_ACTIVE
        )).thenReturn(Arrays.asList(hrApprovalStage, ownerScoringStage));
        when(repository.save(reportingDateStage)).thenReturn(reportingDateStage);

        String roleKey = service.getCurrentRoleKey(scorecard, reportingDate);

        assertEquals(PMConstants.SCORECARD_STAGE_OWNER_SCORING, roleKey);
        assertEquals(ownerScoringStage, reportingDateStage.getApprovalStage());
        verify(repository).save(reportingDateStage);
    }

    @Test
    void getCurrentRoleKeyDoesNotRegressExistingScoreWorkflowStage() {
        ScorecardWorkflowStage ownerScoringStage = stage(
                2L,
                PMConstants.SCORECARD_STAGE_OWNER_SCORING,
                PMConstants.APPROVAL_STATUS_APPROVED_BY_HR
        );
        ScorecardWorkflowStage supervisorScoringStage = stage(
                3L,
                PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING,
                PMConstants.APPROVAL_STATUS_APPROVED_OWNER_SCORES
        );
        Scorecard scorecard = scorecard(10L, ownerScoringStage);
        ReportingDate reportingDate = reportingDate(20L);
        ScorecardReportingDateStage reportingDateStage = reportingDateStage(scorecard, reportingDate, supervisorScoringStage);

        when(repository.findScorecardReportingDateStageByScorecardAndReportingDate(scorecard, reportingDate))
                .thenReturn(reportingDateStage);

        String roleKey = service.getCurrentRoleKey(scorecard, reportingDate);

        assertEquals(PMConstants.SCORECARD_STAGE_SUPERVISOR_SCORING, roleKey);
        assertEquals(supervisorScoringStage, reportingDateStage.getApprovalStage());
        verify(repository, never()).save(reportingDateStage);
    }

    private Scorecard scorecard(long id, ScorecardWorkflowStage approvalStage) {
        Scorecard scorecard = new Scorecard();
        scorecard.setId(id);
        scorecard.setClientId(7L);
        scorecard.setApprovalStage(approvalStage);
        return scorecard;
    }

    private ReportingDate reportingDate(long id) {
        ReportingDate reportingDate = new ReportingDate();
        reportingDate.setId(id);
        return reportingDate;
    }

    private ScorecardReportingDateStage reportingDateStage(Scorecard scorecard,
                                                          ReportingDate reportingDate,
                                                          ScorecardWorkflowStage approvalStage) {
        ScorecardReportingDateStage stage = new ScorecardReportingDateStage();
        stage.setScorecard(scorecard);
        stage.setReportingDate(reportingDate);
        stage.setApprovalStage(approvalStage);
        stage.setStatus(PMConstants.STATUS_ACTIVE);
        return stage;
    }

    private ScorecardWorkflowStage stage(long id, String roleKey, String statusCode) {
        ScorecardWorkflowStage stage = new ScorecardWorkflowStage();
        stage.setId(id);
        stage.setClientId(7L);
        stage.setRoleKey(roleKey);
        stage.setStatusCode(statusCode);
        stage.setStatus(PMConstants.STATUS_ACTIVE);
        return stage;
    }
}
