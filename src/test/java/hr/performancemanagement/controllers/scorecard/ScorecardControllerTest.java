package hr.performancemanagement.controllers.scorecard;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Approval;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.entities.ScorecardReportingDateStage;
import hr.performancemanagement.entities.ScorecardWorkflowStage;
import hr.performancemanagement.repository.CommentRepository;
import hr.performancemanagement.repository.EvidenceRepository;
import hr.performancemanagement.repository.ScoreRepository;
import hr.performancemanagement.service.EvidenceService;
import hr.performancemanagement.service.GearService;
import hr.performancemanagement.service.OverallCommentService;
import hr.performancemanagement.service.OverallScoreService;
import hr.performancemanagement.service.OutcomeService;
import hr.performancemanagement.service.OutputService;
import hr.performancemanagement.service.api.AccountService;
import hr.performancemanagement.service.api.ApprovalService;
import hr.performancemanagement.service.api.CommentService;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.DepartmentService;
import hr.performancemanagement.service.api.GoalService;
import hr.performancemanagement.service.api.NotificationService;
import hr.performancemanagement.service.api.PerspectiveService;
import hr.performancemanagement.service.api.ReportingDateService;
import hr.performancemanagement.service.api.ReportingPeriodService;
import hr.performancemanagement.service.api.ScorecardModelService;
import hr.performancemanagement.service.api.ScorecardReportingDateStageService;
import hr.performancemanagement.service.api.ScorecardService;
import hr.performancemanagement.service.api.ScorecardWorkflowService;
import hr.performancemanagement.service.api.StrategicObjectiveService;
import hr.performancemanagement.service.api.SystemSettingService;
import hr.performancemanagement.service.api.TargetService;
import hr.performancemanagement.service.api.ScoreService.StandardScorecardScoreService;
import hr.performancemanagement.service.api.ScoreService.ValueBasedScoreService;
import hr.performancemanagement.utils.constants.PMConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScorecardControllerTest {

    private ScorecardService scorecardService;
    private CommonService commonService;
    private ReportingDateService reportingDateService;
    private ScorecardReportingDateStageService reportingDateStageService;
    private ApprovalService approvalService;
    private NotificationService notificationService;
    private ScorecardController controller;

    @BeforeEach
    void setUp() {
        scorecardService = mock(ScorecardService.class);
        commonService = mock(CommonService.class);
        reportingDateService = mock(ReportingDateService.class);
        reportingDateStageService = mock(ScorecardReportingDateStageService.class);
        approvalService = mock(ApprovalService.class);
        notificationService = mock(NotificationService.class);

        controller = new ScorecardController(
                mock(ReportingPeriodService.class),
                mock(AccountService.class),
                mock(DepartmentService.class),
                scorecardService,
                mock(PerspectiveService.class),
                mock(GoalService.class),
                mock(TargetService.class),
                mock(GearService.class),
                mock(OutcomeService.class),
                mock(OutputService.class),
                mock(StrategicObjectiveService.class),
                mock(CommentService.class),
                notificationService,
                approvalService,
                reportingDateService,
                mock(StandardScorecardScoreService.class),
                mock(ValueBasedScoreService.class),
                mock(ScorecardModelService.class),
                commonService,
                mock(SystemSettingService.class),
                mock(ScorecardWorkflowService.class),
                reportingDateStageService,
                mock(EvidenceRepository.class),
                mock(EvidenceService.class),
                mock(CommentRepository.class),
                mock(ScoreRepository.class),
                mock(OverallScoreService.class),
                mock(OverallCommentService.class),
                mock(Environment.class)
        );
    }

    @Test
    void supervisorRejectOwnerScoresRequiresReason() throws Exception {
        Scorecard scorecard = scorecard();
        when(scorecardService.getScorecardById(10L)).thenReturn(scorecard);
        when(commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_OWNER_SCORES, scorecard)).thenReturn(true);

        String result = controller.supervisorRejectOwnerScores(new MockHttpServletRequest(), 10L, " ");

        assertEquals("redirect:/scorecards/view-scorecard/10", result);
        verify(reportingDateStageService, never()).moveToRole(any(), any(), any());
        verify(notificationService, never()).sendUserMessageAsync(any(), any(), any(), any());
        verify(approvalService, never()).addApproval(any());
    }

    @Test
    void supervisorRejectOwnerScoresMovesBackToOwnerScoringAndEmailsOwner() throws Exception {
        Scorecard scorecard = scorecard();
        ReportingDate reportingDate = reportingDate(scorecard.getReportingPeriod());
        ScorecardReportingDateStage movedStage = reportingDateStage(PMConstants.SCORECARD_STAGE_OWNER_SCORING);
        Account supervisor = scorecard.getOwner().getSupervisor();

        when(scorecardService.getScorecardById(10L)).thenReturn(scorecard);
        when(commonService.isUserAllowed(PMConstants.ACTIVITY_APPROVE_OWNER_SCORES, scorecard)).thenReturn(true);
        when(commonService.getLoggedUser()).thenReturn(supervisor);
        when(commonService.getCurrentUrl(any())).thenReturn("http://localhost");
        when(reportingDateService.getActiveReportingDate()).thenReturn(reportingDate);
        when(reportingDateService.isReportingDateOpen(reportingDate)).thenReturn(true);
        when(reportingDateStageService.moveToRole(scorecard, reportingDate, PMConstants.SCORECARD_STAGE_OWNER_SCORING))
                .thenReturn(movedStage);

        String result = controller.supervisorRejectOwnerScores(
                new MockHttpServletRequest(),
                10L,
                "Please add evidence."
        );

        assertEquals("redirect:/scorecards/view-scorecard/10", result);
        verify(reportingDateStageService).moveToRole(scorecard, reportingDate, PMConstants.SCORECARD_STAGE_OWNER_SCORING);

        ArgumentCaptor<Approval> approvalCaptor = ArgumentCaptor.forClass(Approval.class);
        verify(approvalService).addApproval(approvalCaptor.capture());
        assertEquals(PMConstants.APPROVAL_STATUS_REJECTED_OWNER_SCORES, approvalCaptor.getValue().getStatus());
        assertEquals("Please add evidence.", approvalCaptor.getValue().getMessage());
        assertEquals(scorecard, approvalCaptor.getValue().getScorecard());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendUserMessageAsync(
                eq("employee@example.com"),
                eq(null),
                eq("Scorecard Activity - Scoring"),
                bodyCaptor.capture()
        );
        assertTrue(bodyCaptor.getValue().contains("Please add evidence."));
        assertTrue(bodyCaptor.getValue().contains("http://localhost/scorecards/view-scorecard/10"));
    }

    private Scorecard scorecard() {
        ReportingPeriod reportingPeriod = new ReportingPeriod();
        reportingPeriod.setId(99L);
        reportingPeriod.setStatus(PMConstants.STATUS_ACTIVE);

        Account supervisor = new Account();
        supervisor.setId(2L);
        supervisor.setFullName("Supervisor User");
        supervisor.setEmail("supervisor@example.com");

        Account owner = new Account();
        owner.setId(1L);
        owner.setFullName("Employee User");
        owner.setEmail("employee@example.com");
        owner.setSupervisor(supervisor);

        Scorecard scorecard = new Scorecard();
        scorecard.setId(10L);
        scorecard.setOwner(owner);
        scorecard.setReportingPeriod(reportingPeriod);
        scorecard.setStatus(PMConstants.STATUS_ACTIVE);
        scorecard.setLockStatus(PMConstants.LOCK_STATUS_OPEN);
        return scorecard;
    }

    private ReportingDate reportingDate(ReportingPeriod reportingPeriod) {
        ReportingDate reportingDate = new ReportingDate();
        reportingDate.setId(20L);
        reportingDate.setReportingPeriod(reportingPeriod);
        reportingDate.setStatus(PMConstants.REPORTING_DATE_STATUS_OPEN);
        return reportingDate;
    }

    private ScorecardReportingDateStage reportingDateStage(String roleKey) {
        ScorecardWorkflowStage workflowStage = new ScorecardWorkflowStage();
        workflowStage.setId(30L);
        workflowStage.setRoleKey(roleKey);
        workflowStage.setStatusCode(PMConstants.APPROVAL_STATUS_SCORED_BY_EMPLOYEE);

        ScorecardReportingDateStage stage = new ScorecardReportingDateStage();
        stage.setApprovalStage(workflowStage);
        stage.setStatus(PMConstants.STATUS_ACTIVE);
        return stage;
    }
}
