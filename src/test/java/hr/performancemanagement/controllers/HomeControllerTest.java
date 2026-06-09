package hr.performancemanagement.controllers;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.StrategicObjective;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.ReportingDateService;
import hr.performancemanagement.service.api.ReportingPeriodService;
import hr.performancemanagement.service.api.ScorecardService;
import hr.performancemanagement.service.api.StrategicObjectiveService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class HomeControllerTest {

    @Test
    void initialDashboardVisitDoesNotGenerateDashboardData() {
        ReportingPeriodService reportingPeriodService = mock(ReportingPeriodService.class);
        ScorecardService scorecardService = mock(ScorecardService.class);
        ReportingDateService reportingDateService = mock(ReportingDateService.class);
        CommonService commonService = mock(CommonService.class);
        HomeController controller = new HomeController(reportingPeriodService, scorecardService);
        ReflectionTestUtils.setField(controller, "reportingDateService", reportingDateService);
        ReflectionTestUtils.setField(controller, "commonService", commonService);

        ReportingPeriod period = new ReportingPeriod();
        period.setId(12L);
        period.setStartDate("2026-01-01");
        period.setEndDate("2026-12-31");
        Account account = new Account();
        account.setAdmin("NO");
        when(commonService.getLoggedUser()).thenReturn(account);
        when(reportingPeriodService.listAllReportingPeriods()).thenReturn(Collections.singletonList(period));
        when(reportingPeriodService.getActiveReportingPeriod()).thenReturn(period);
        when(reportingDateService.listAllReportingDates(period)).thenReturn(Collections.emptyList());

        ModelAndView modelAndView = controller.goToHome(null, null, false, new MockHttpServletRequest());

        assertEquals(false, modelAndView.getModel().get("dashboardFiltersApplied"));
        verifyNoInteractions(scorecardService);
    }

    @Test
    void objectiveSeriesIncludesConfiguredObjectivesWithoutScorecards() {
        ReportingPeriodService reportingPeriodService = mock(ReportingPeriodService.class);
        ScorecardService scorecardService = mock(ScorecardService.class);
        StrategicObjectiveService strategicObjectiveService = mock(StrategicObjectiveService.class);
        HomeController controller = new HomeController(reportingPeriodService, scorecardService);
        ReflectionTestUtils.setField(controller, "strategicObjectiveService", strategicObjectiveService);

        ReportingPeriod period = new ReportingPeriod();
        period.setId(12L);
        period.setModel("standard");
        StrategicObjective objective = new StrategicObjective();
        objective.setName("Grow export revenue");
        when(strategicObjectiveService.listAllStrategicObjectives(period.getId()))
                .thenReturn(Collections.singletonList(objective));

        Object series = ReflectionTestUtils.invokeMethod(
                controller,
                "buildDashboardObjectiveSeries",
                period,
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                LocalDate.of(2026, 6, 9)
        );

        List<String> labels = ReflectionTestUtils.invokeMethod(series, "getLabels");
        List<Double> weights = ReflectionTestUtils.invokeMethod(series, "getWeights");
        List<Double> scores = ReflectionTestUtils.invokeMethod(series, "getScores");
        assertEquals(Collections.singletonList("Grow export revenue"), labels);
        assertEquals(Collections.singletonList(0.0), weights);
        assertEquals(Collections.singletonList(0.0), scores);
    }
}
