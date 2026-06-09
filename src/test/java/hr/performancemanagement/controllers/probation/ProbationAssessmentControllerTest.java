package hr.performancemanagement.controllers.probation;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ProbationAssessment;
import hr.performancemanagement.entities.ProbationKpi;
import hr.performancemanagement.service.api.ProbationAssessmentService;
import hr.performancemanagement.utils.constants.PMConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProbationAssessmentControllerTest {

    private ProbationAssessmentService probationAssessmentService;
    private ProbationAssessmentController controller;

    @BeforeEach
    void setUp() {
        probationAssessmentService = mock(ProbationAssessmentService.class);
        controller = new ProbationAssessmentController();
        ReflectionTestUtils.setField(controller, "probationAssessmentService", probationAssessmentService);
    }

    @Test
    void saveKpisPersistsNewRowAndUpdatesContractStatus() {
        ProbationAssessment assessment = assessment();
        when(probationAssessmentService.getAssessmentById(20L)).thenReturn(assessment);
        when(probationAssessmentService.isLoggedUserOwner(assessment)).thenReturn(true);
        when(probationAssessmentService.addKpi(anyLong(), any(ProbationKpi.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(probationAssessmentService.updateAssessmentStatus(20L, PMConstants.PROBATION_STATUS_KPI_SET))
                .thenReturn(assessment);

        String result = controller.saveKpis(
                20L,
                new MockHttpServletRequest(),
                null,
                Collections.singletonList("Grow export revenue"),
                Collections.singletonList("Signed contracts"),
                Collections.singletonList("USD 1m")
        );

        assertEquals("redirect:/probation-assessments/add-kpis/20", result);
        verify(probationAssessmentService).addKpi(anyLong(), any(ProbationKpi.class));
        verify(probationAssessmentService).updateAssessmentStatus(20L, PMConstants.PROBATION_STATUS_KPI_SET);
    }

    @Test
    void saveKpisDoesNotReportSuccessWhenPersistenceFails() {
        ProbationAssessment assessment = assessment();
        when(probationAssessmentService.getAssessmentById(20L)).thenReturn(assessment);
        when(probationAssessmentService.isLoggedUserOwner(assessment)).thenReturn(true);
        when(probationAssessmentService.addKpi(anyLong(), any(ProbationKpi.class))).thenReturn(null);

        String result = controller.saveKpis(
                20L,
                new MockHttpServletRequest(),
                null,
                Collections.singletonList("Grow export revenue"),
                Collections.singletonList("Signed contracts"),
                Collections.singletonList("USD 1m")
        );

        assertEquals("redirect:/probation-assessments/add-kpis/20", result);
        verify(probationAssessmentService, never())
                .updateAssessmentStatus(20L, PMConstants.PROBATION_STATUS_KPI_SET);
    }

    @Test
    void saveIncumbentEvaluationDraftPersistsEverySubmittedRow() {
        ProbationAssessment assessment = assessment();
        assessment.setStatus(PMConstants.PROBATION_STATUS_KPI_APPROVED);
        ProbationKpi firstKpi = kpi(101L, "Revenue", assessment);
        ProbationKpi secondKpi = kpi(102L, "Quality", assessment);
        when(probationAssessmentService.getAssessmentById(20L)).thenReturn(assessment);
        when(probationAssessmentService.isLoggedUserOwner(assessment)).thenReturn(true);
        when(probationAssessmentService.listKpis(20L)).thenReturn(Arrays.asList(firstKpi, secondKpi));
        when(probationAssessmentService.updateKpi(any(ProbationKpi.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new HashMap<>();
        params.put("progress_101", "75");
        params.put("incumbentMark_101", "4");
        params.put("incumbentComment_101", "On track");
        params.put("progress_102", "90");
        params.put("incumbentMark_102", "5");
        params.put("incumbentComment_102", "Exceeded target");

        String result = controller.saveIncumbentEvaluationDraft(
                20L,
                new MockHttpServletRequest(),
                params
        );

        assertEquals("redirect:/probation-assessments/evaluate/20", result);
        assertEquals(75.0, firstKpi.getProgressPercent());
        assertEquals(4.0, firstKpi.getIncumbentMark());
        assertEquals("On track", firstKpi.getIncumbentComment());
        assertEquals(90.0, secondKpi.getProgressPercent());
        assertEquals(5.0, secondKpi.getIncumbentMark());
        assertEquals("Exceeded target", secondKpi.getIncumbentComment());
        verify(probationAssessmentService).updateKpi(firstKpi);
        verify(probationAssessmentService).updateKpi(secondKpi);
    }

    @Test
    void saveIncumbentEvaluationDraftSkipsCompletelyBlankNewRows() {
        ProbationAssessment assessment = assessment();
        assessment.setStatus(PMConstants.PROBATION_STATUS_KPI_APPROVED);
        ProbationKpi completedKpi = kpi(101L, "Revenue", assessment);
        ProbationKpi blankKpi = kpi(102L, "Quality", assessment);
        when(probationAssessmentService.getAssessmentById(20L)).thenReturn(assessment);
        when(probationAssessmentService.isLoggedUserOwner(assessment)).thenReturn(true);
        when(probationAssessmentService.listKpis(20L)).thenReturn(Arrays.asList(completedKpi, blankKpi));
        when(probationAssessmentService.updateKpi(any(ProbationKpi.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new HashMap<>();
        params.put("progress_101", "75");
        params.put("incumbentMark_101", "4");
        params.put("incumbentComment_101", "");
        params.put("progress_102", "");
        params.put("incumbentMark_102", "");
        params.put("incumbentComment_102", "");

        controller.saveIncumbentEvaluationDraft(20L, new MockHttpServletRequest(), params);

        verify(probationAssessmentService).updateKpi(completedKpi);
        verify(probationAssessmentService, never()).updateKpi(blankKpi);
        assertNull(blankKpi.getProgressPercent());
        assertNull(blankKpi.getIncumbentMark());
    }

    @Test
    void saveSupervisorEvaluationDraftPersistsEverySubmittedRow() {
        ProbationAssessment assessment = assessment();
        assessment.setStatus(PMConstants.PROBATION_STATUS_EVALUATION_PENDING_SUPERVISOR_REVIEW);
        ProbationKpi firstKpi = kpi(101L, "Revenue", assessment);
        ProbationKpi secondKpi = kpi(102L, "Quality", assessment);
        when(probationAssessmentService.getAssessmentById(20L)).thenReturn(assessment);
        when(probationAssessmentService.isLoggedUserSupervisor(assessment)).thenReturn(true);
        when(probationAssessmentService.listKpis(20L)).thenReturn(Arrays.asList(firstKpi, secondKpi));
        when(probationAssessmentService.updateKpi(any(ProbationKpi.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> params = new HashMap<>();
        params.put("supervisorMark_101", "4");
        params.put("supervisorComment_101", "On track");
        params.put("supervisorMark_102", "5");
        params.put("supervisorComment_102", "Exceeded target");

        String result = controller.saveSupervisorEvaluationDraft(
                20L,
                new MockHttpServletRequest(),
                params
        );

        assertEquals("redirect:/probation-assessments/evaluate/20", result);
        assertEquals(4.0, firstKpi.getSupervisorMark());
        assertEquals("On track", firstKpi.getSupervisorComment());
        assertEquals(5.0, secondKpi.getSupervisorMark());
        assertEquals("Exceeded target", secondKpi.getSupervisorComment());
        verify(probationAssessmentService).updateKpi(firstKpi);
        verify(probationAssessmentService).updateKpi(secondKpi);
    }

    @Test
    void submitSupervisorReviewUsesSavedMarksBeforeProceeding() {
        ProbationAssessment assessment = assessment();
        assessment.setStatus(PMConstants.PROBATION_STATUS_EVALUATION_PENDING_SUPERVISOR_REVIEW);
        ProbationKpi firstKpi = kpi(101L, "Revenue", assessment);
        ProbationKpi secondKpi = kpi(102L, "Quality", assessment);
        firstKpi.setSupervisorMark(4.0);
        secondKpi.setSupervisorMark(5.0);
        when(probationAssessmentService.getAssessmentById(20L)).thenReturn(assessment);
        when(probationAssessmentService.isLoggedUserSupervisor(assessment)).thenReturn(true);
        when(probationAssessmentService.listKpis(20L)).thenReturn(Arrays.asList(firstKpi, secondKpi));

        String result = controller.submitSupervisorReview(20L, new MockHttpServletRequest());

        assertEquals("redirect:/probation-assessments/dimensions/20?step=0", result);
        verify(probationAssessmentService, never()).updateKpi(any(ProbationKpi.class));
        verify(probationAssessmentService)
                .updateAssessmentStatus(20L, PMConstants.PROBATION_STATUS_PERSONAL_DIMENSIONS_IN_PROGRESS);
    }

    private ProbationAssessment assessment() {
        Account owner = new Account();
        owner.setId(30L);
        ProbationAssessment assessment = new ProbationAssessment();
        assessment.setId(20L);
        assessment.setEmployee(owner);
        assessment.setStatus(PMConstants.PROBATION_STATUS_CONTRACT_CREATED);
        return assessment;
    }

    private ProbationKpi kpi(long id, String name, ProbationAssessment assessment) {
        ProbationKpi kpi = new ProbationKpi();
        kpi.setId(id);
        kpi.setName(name);
        kpi.setAssessment(assessment);
        return kpi;
    }
}
