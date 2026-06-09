package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ProbationAssessment;
import hr.performancemanagement.entities.ProbationDimensionTemplate;
import hr.performancemanagement.entities.ProbationKpi;
import hr.performancemanagement.entities.ProbationKpiComment;
import hr.performancemanagement.repository.PerformanceImprovementPlanRepository;
import hr.performancemanagement.repository.ProbationAssessmentApprovalRepository;
import hr.performancemanagement.repository.ProbationAssessmentDimensionRepository;
import hr.performancemanagement.repository.ProbationAssessmentRepository;
import hr.performancemanagement.repository.ProbationKpiCommentRepository;
import hr.performancemanagement.repository.ProbationKpiRepository;
import hr.performancemanagement.service.api.AccountService;
import hr.performancemanagement.service.api.AccessControlService;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.ProbationConfigService;
import hr.performancemanagement.utils.constants.PMConstants;
import hr.performancemanagement.utils.dto.ProbationResultSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProbationAssessmentServiceImplTest {

    @Mock
    private ProbationAssessmentRepository assessmentRepository;
    @Mock
    private ProbationAssessmentDimensionRepository assessmentDimensionRepository;
    @Mock
    private ProbationKpiRepository probationKpiRepository;
    @Mock
    private ProbationKpiCommentRepository probationKpiCommentRepository;
    @Mock
    private ProbationAssessmentApprovalRepository approvalRepository;
    @Mock
    private ProbationConfigService probationConfigService;
    @Mock
    private CommonService commonService;
    @Mock
    private AccountService accountService;
    @Mock
    private PerformanceImprovementPlanRepository performanceImprovementPlanRepository;
    @Mock
    private AccessControlService accessControlService;

    @InjectMocks
    private ProbationAssessmentServiceImpl service;

    @Test
    void hrRoleCanSeeTenantAssessmentsAwaitingHrAction() {
        Account hr = account(10L, 7L, "HR");
        ProbationAssessment assessment = assessment(20L, 7L, PMConstants.PROBATION_STATUS_KPI_PENDING_HR_APPROVAL, account(30L, 7L, "USER"));
        when(commonService.getLoggedUser()).thenReturn(hr);
        when(commonService.isAdmin()).thenReturn(false);
        when(commonService.hasSpecialRights()).thenReturn(false);
        when(accessControlService.hasPermission(eq(hr), anyString(), nullable(Account.class))).thenReturn(true);
        when(assessmentRepository.findProbationAssessmentsByClientIdOrderByDateDesc(7L))
                .thenReturn(Collections.singletonList(assessment));

        List<ProbationAssessment> visible = service.listVisibleAssessments();

        assertEquals(Collections.singletonList(assessment), visible);
    }

    @Test
    void unrelatedTenantUserCannotOpenAssessmentByGuessingItsId() {
        Account peer = account(11L, 7L, "USER");
        Account owner = account(30L, 7L, "USER");
        ProbationAssessment assessment = assessment(20L, 7L, PMConstants.PROBATION_STATUS_KPI_APPROVED, owner);
        when(commonService.getLoggedUser()).thenReturn(peer);
        when(assessmentRepository.findProbationAssessmentByIdAndClientId(20L, 7L)).thenReturn(assessment);

        assertNull(service.getAssessmentById(20L));
    }

    @Test
    void completedAssessmentCannotBeReopenedOrHaveKpiResultsChanged() {
        Account owner = account(30L, 7L, "USER");
        ProbationAssessment assessment = assessment(20L, 7L, PMConstants.PROBATION_STATUS_EVALUATION_COMPLETED, owner);
        ProbationKpi existing = kpi(40L, assessment, 4.0, 4.5, 90.0);
        when(commonService.getLoggedUser()).thenReturn(owner);
        when(assessmentRepository.findProbationAssessmentByIdAndClientId(20L, 7L)).thenReturn(assessment);
        when(probationKpiRepository.findProbationKpiByIdAndAssessment_ClientId(40L, 7L)).thenReturn(existing);

        ProbationKpi requested = new ProbationKpi();
        requested.setId(40L);
        requested.setIncumbentMark(1.0);

        assertNull(service.updateAssessmentStatus(20L, PMConstants.PROBATION_STATUS_KPI_SET));
        assertNull(service.updateKpi(requested));
        verify(assessmentRepository, never()).save(any(ProbationAssessment.class));
        verify(probationKpiRepository, never()).save(any(ProbationKpi.class));
    }

    @Test
    void incumbentEvaluationUpdateCannotOverwriteSupervisorMarkOrContract() {
        Account owner = account(30L, 7L, "USER");
        ProbationAssessment assessment = assessment(20L, 7L, PMConstants.PROBATION_STATUS_KPI_APPROVED, owner);
        ProbationKpi existing = kpi(40L, assessment, 3.0, 4.5, 60.0);
        existing.setName("Existing KPI");
        existing.setTarget("Existing target");
        when(commonService.getLoggedUser()).thenReturn(owner);
        when(probationKpiRepository.findProbationKpiByIdAndAssessment_ClientId(40L, 7L)).thenReturn(existing);
        when(probationKpiRepository.save(existing)).thenReturn(existing);

        ProbationKpi requested = new ProbationKpi();
        requested.setId(40L);
        requested.setName("Tampered KPI");
        requested.setTarget("Tampered target");
        requested.setIncumbentMark(3.5);
        requested.setSupervisorMark(1.0);
        requested.setProgressPercent(75.0);

        ProbationKpi saved = service.updateKpi(requested);

        assertEquals(3.5, saved.getIncumbentMark());
        assertEquals(75.0, saved.getProgressPercent());
        assertEquals(4.5, saved.getSupervisorMark());
        assertEquals("Existing KPI", saved.getName());
        assertEquals("Existing target", saved.getTarget());
    }

    @Test
    void resultSummaryUsesSupervisorMarksAndProducesActionableRecommendation() {
        Account owner = account(30L, 7L, "USER");
        ProbationAssessment assessment = assessment(20L, 7L, PMConstants.PROBATION_STATUS_EVALUATION_PENDING_HR_APPROVAL, owner);
        ProbationKpi first = kpi(40L, assessment, 4.0, 4.0, 80.0);
        ProbationKpi second = kpi(41L, assessment, 3.5, 3.0, 70.0);
        when(commonService.getLoggedUser()).thenReturn(owner);
        when(assessmentRepository.findProbationAssessmentByIdAndClientId(20L, 7L)).thenReturn(assessment);
        when(probationKpiRepository.findProbationKpisByAssessmentOrderByIdAsc(assessment))
                .thenReturn(Arrays.asList(first, second));

        ProbationResultSummary result = service.getResultSummary(20L);

        assertTrue(result.isComplete());
        assertEquals(3.5, result.getResultMark());
        assertEquals("Supervisor marks", result.getResultSource());
        assertEquals(75.0, result.getAverageProgress());
        assertEquals("Exceeds expectations", result.getPerformanceBand());
        assertTrue(result.getRecommendation().contains("supports confirmation"));
    }

    @Test
    void incompleteResultIsExplicitlyMarkedIncomplete() {
        Account owner = account(30L, 7L, "USER");
        ProbationAssessment assessment = assessment(20L, 7L, PMConstants.PROBATION_STATUS_KPI_APPROVED, owner);
        ProbationKpi kpi = kpi(40L, assessment, 3.0, null, null);
        when(commonService.getLoggedUser()).thenReturn(owner);
        when(assessmentRepository.findProbationAssessmentByIdAndClientId(20L, 7L)).thenReturn(assessment);
        when(probationKpiRepository.findProbationKpisByAssessmentOrderByIdAsc(assessment))
                .thenReturn(Collections.singletonList(kpi));

        ProbationResultSummary result = service.getResultSummary(20L);

        assertFalse(result.isComplete());
        assertTrue(result.getRecommendation().contains("Incumbent evaluation is incomplete"));
    }

    @Test
    void overlappingOpenContractIsRejected() {
        Account hr = account(10L, 7L, "HR");
        Account employee = account(30L, 7L, "USER");
        employee.setSupervisor(account(31L, 7L, "SUPERVISOR"));
        ProbationAssessment requested = assessment(0L, 0L, "IGNORED", employee);
        requested.setStartDate("2026-06-01");
        requested.setEndDate("2026-08-31");
        ProbationAssessment existing = assessment(20L, 7L, PMConstants.PROBATION_STATUS_KPI_APPROVED, employee);
        existing.setStartDate("2026-05-01");
        existing.setEndDate("2026-07-31");
        when(commonService.getLoggedUser()).thenReturn(hr);
        when(accessControlService.hasPermission(eq(hr), anyString(), nullable(Account.class))).thenReturn(true);
        when(accountService.getAccountById(30L)).thenReturn(employee);
        when(probationConfigService.listActiveDimensionTemplates())
                .thenReturn(Collections.singletonList(new ProbationDimensionTemplate()));
        when(assessmentRepository.findProbationAssessmentsByEmployeeOrderByDateDesc(employee))
                .thenReturn(Collections.singletonList(existing));

        assertNull(service.createAssessment(requested));
        verify(assessmentRepository, never()).save(any(ProbationAssessment.class));
    }

    @Test
    void createdContractUsesFixedInitialStatusAndNormalizedPeriod() {
        Account hr = account(10L, 7L, "HR");
        Account employee = account(30L, 7L, "USER");
        employee.setSupervisor(account(31L, 7L, "SUPERVISOR"));
        ProbationAssessment requested = assessment(0L, 0L, PMConstants.PROBATION_STATUS_EVALUATION_COMPLETED, employee);
        requested.setStartDate(" 2026-06-01 ");
        requested.setEndDate(" 2026-08-31 ");
        when(commonService.getLoggedUser()).thenReturn(hr);
        when(accessControlService.hasPermission(eq(hr), anyString(), nullable(Account.class))).thenReturn(true);
        when(accountService.getAccountById(30L)).thenReturn(employee);
        when(probationConfigService.listActiveDimensionTemplates())
                .thenReturn(Collections.singletonList(new ProbationDimensionTemplate()));
        when(assessmentRepository.findProbationAssessmentsByEmployeeOrderByDateDesc(employee))
                .thenReturn(Collections.emptyList());
        when(assessmentRepository.save(requested)).thenReturn(requested);

        ProbationAssessment created = service.createAssessment(requested);

        assertEquals(PMConstants.PROBATION_STATUS_CONTRACT_CREATED, created.getStatus());
        assertEquals("2026-06-01 - 2026-08-31", created.getPerformancePeriod());
    }

    @Test
    void reviewedKpiCannotBeDeletedAndLoseItsAuditHistory() {
        Account owner = account(30L, 7L, "USER");
        ProbationAssessment assessment = assessment(20L, 7L, PMConstants.PROBATION_STATUS_KPI_REJECTED_BY_SUPERVISOR, owner);
        ProbationKpi kpi = kpi(40L, assessment, null, null, null);
        when(commonService.getLoggedUser()).thenReturn(owner);
        when(probationKpiRepository.findProbationKpiByIdAndAssessment_ClientId(40L, 7L)).thenReturn(kpi);
        when(probationKpiCommentRepository.findProbationKpiCommentsByProbationKpiOrderByDateAsc(kpi))
                .thenReturn(Collections.singletonList(new ProbationKpiComment()));

        assertFalse(service.deleteKpi(40L));
        verify(probationKpiRepository, never()).delete(any(ProbationKpi.class));
    }

    private Account account(long id, long clientId, String role) {
        Account account = new Account();
        account.setId(id);
        account.setClientId(clientId);
        account.setRole(role);
        account.setAdmin("NO");
        account.setSpecial("NO");
        account.setStatus(PMConstants.STATUS_ACTIVE);
        return account;
    }

    private ProbationAssessment assessment(long id, long clientId, String status, Account owner) {
        ProbationAssessment assessment = new ProbationAssessment();
        assessment.setId(id);
        assessment.setClientId(clientId);
        assessment.setStatus(status);
        assessment.setEmployee(owner);
        return assessment;
    }

    private ProbationKpi kpi(long id,
                             ProbationAssessment assessment,
                             Double incumbentMark,
                             Double supervisorMark,
                             Double progress) {
        ProbationKpi kpi = new ProbationKpi();
        kpi.setId(id);
        kpi.setAssessment(assessment);
        kpi.setIncumbentMark(incumbentMark);
        kpi.setSupervisorMark(supervisorMark);
        kpi.setProgressPercent(progress);
        return kpi;
    }
}
