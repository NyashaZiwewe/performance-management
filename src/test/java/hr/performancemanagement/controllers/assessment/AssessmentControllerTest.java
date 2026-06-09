package hr.performancemanagement.controllers.assessment;

import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.service.OutcomeService;
import hr.performancemanagement.service.api.AccountService;
import hr.performancemanagement.service.api.GoalService;
import hr.performancemanagement.service.api.ReportingDateService;
import hr.performancemanagement.service.api.ReportingPeriodService;
import hr.performancemanagement.service.api.ScorecardService;
import hr.performancemanagement.service.api.TargetService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AssessmentControllerTest {

    @Test
    void initialScoresVisitDoesNotGenerateScores() {
        TargetService targetService = mock(TargetService.class);
        GoalService goalService = mock(GoalService.class);
        OutcomeService outcomeService = mock(OutcomeService.class);
        AccountService accountService = mock(AccountService.class);
        ReportingPeriodService reportingPeriodService = mock(ReportingPeriodService.class);
        ReportingDateService reportingDateService = mock(ReportingDateService.class);
        ScorecardService scorecardService = mock(ScorecardService.class);
        AssessmentController controller = new AssessmentController(targetService, goalService, outcomeService, accountService);
        ReflectionTestUtils.setField(controller, "reportingPeriodService", reportingPeriodService);
        ReflectionTestUtils.setField(controller, "reportingDateService", reportingDateService);
        ReflectionTestUtils.setField(controller, "scorecardService", scorecardService);

        ReportingPeriod period = new ReportingPeriod();
        period.setId(12L);
        period.setStartDate("2026-01-01");
        period.setEndDate("2026-12-31");
        when(reportingPeriodService.getReportingPeriodById(period.getId())).thenReturn(period);
        when(reportingPeriodService.listAllReportingPeriods()).thenReturn(Collections.singletonList(period));
        when(reportingDateService.listAllReportingDates(period)).thenReturn(Collections.emptyList());

        ModelAndView modelAndView = controller.viewScores(
                period.getId(),
                null,
                null,
                false,
                new MockHttpServletRequest()
        );

        assertEquals(false, modelAndView.getModel().get("scoreFiltersApplied"));
        verifyNoInteractions(scorecardService, targetService, accountService);
    }

    @Test
    void applyingScoreFiltersRedirectsWithExplicitGenerationFlag() {
        AssessmentController controller = new AssessmentController(
                mock(TargetService.class),
                mock(GoalService.class),
                mock(OutcomeService.class),
                mock(AccountService.class)
        );

        String redirect = controller.goToViewScores(new MockHttpServletRequest(), 12L, 21L);

        assertEquals(
                "redirect:/performance-review/view-scores/12?applyFilters=true&reportingDateId=21",
                redirect
        );
    }

    @Test
    void individualTrendsWithoutEmployeeReturnsToSelectionPage() {
        AccountService accountService = mock(AccountService.class);
        AssessmentController controller = new AssessmentController(
                mock(TargetService.class),
                mock(GoalService.class),
                mock(OutcomeService.class),
                accountService
        );

        ModelAndView modelAndView = controller.viewIndividualTrends(null, new MockHttpServletRequest());

        assertEquals(
                "redirect:/performance-review/view-individual-trends-select-year",
                modelAndView.getViewName()
        );
        verifyNoInteractions(accountService);
    }

    @Test
    void scoresWithoutReportingPeriodReturnsToSelectionPage() {
        AssessmentController controller = new AssessmentController(
                mock(TargetService.class),
                mock(GoalService.class),
                mock(OutcomeService.class),
                mock(AccountService.class)
        );

        String redirect = controller.goToViewScores(new MockHttpServletRequest(), null, null);

        assertEquals("redirect:/performance-review/view-scores-select-year", redirect);
    }
}
