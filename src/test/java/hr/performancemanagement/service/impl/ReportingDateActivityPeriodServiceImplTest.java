package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingDateActivityPeriod;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ReportingDateActivityPeriodRepository;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.repository.ScoreCardRepository;
import hr.performancemanagement.repository.ScorecardReportingDateStageRepository;
import hr.performancemanagement.service.api.NotificationService;
import hr.performancemanagement.service.api.SystemSettingService;
import hr.performancemanagement.utils.constants.PMConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingDateActivityPeriodServiceImplTest {

    @Mock private ReportingDateActivityPeriodRepository activityPeriodRepository;
    @Mock private ReportingDateRepository reportingDateRepository;
    @Mock private ScoreCardRepository scoreCardRepository;
    @Mock private ScorecardReportingDateStageRepository scorecardReportingDateStageRepository;
    @Mock private NotificationService notificationService;
    @Mock private SystemSettingService systemSettingService;

    private ReportingDateActivityPeriodServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportingDateActivityPeriodServiceImpl(
                activityPeriodRepository,
                reportingDateRepository,
                scoreCardRepository,
                scorecardReportingDateStageRepository,
                notificationService,
                systemSettingService
        );
    }

    @Test
    void rejectsInclusiveOverlapBecauseLastDateIsTheCutoff() {
        ReportingDate reportingDate = reportingDate();
        ReportingDateActivityPeriod targetPeriod = period(
                reportingDate,
                PMConstants.REPORTING_ACTIVITY_TARGET_CAPTURE,
                LocalDate.now().minusDays(5),
                LocalDate.now()
        );
        ReportingDateActivityPeriod scorePeriod = period(
                reportingDate,
                PMConstants.REPORTING_ACTIVITY_SCORE_CAPTURE,
                LocalDate.now(),
                LocalDate.now().plusDays(5)
        );
        when(reportingDateRepository.findReportingDateById(reportingDate.getId())).thenReturn(reportingDate);
        when(activityPeriodRepository.findReportingDateActivityPeriodsByReportingDateOrderByStartDateAscEndDateAsc(reportingDate))
                .thenReturn(Collections.singletonList(targetPeriod));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveActivityPeriod(scorePeriod)
        );

        assertTrue(exception.getMessage().contains("cannot overlap"));
    }

    @Test
    void rejectsActivitiesConfiguredOutOfLifecycleOrder() {
        ReportingDate reportingDate = reportingDate();
        ReportingDateActivityPeriod scoring = period(
                reportingDate,
                PMConstants.REPORTING_ACTIVITY_SCORE_CAPTURE,
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(5)
        );
        ReportingDateActivityPeriod target = period(
                reportingDate,
                PMConstants.REPORTING_ACTIVITY_TARGET_CAPTURE,
                LocalDate.now(),
                LocalDate.now().plusDays(5)
        );
        when(reportingDateRepository.findReportingDateById(reportingDate.getId())).thenReturn(reportingDate);
        when(activityPeriodRepository.findReportingDateActivityPeriodsByReportingDateOrderByStartDateAscEndDateAsc(reportingDate))
                .thenReturn(Collections.singletonList(scoring));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveActivityPeriod(target)
        );

        assertTrue(exception.getMessage().contains("must follow this order"));
    }

    @Test
    void onlyAllowsActionsMappedToTheCurrentConfiguredActivity() {
        ReportingDate reportingDate = reportingDate();
        ReportingDateActivityPeriod scoring = period(
                reportingDate,
                PMConstants.REPORTING_ACTIVITY_SCORE_CAPTURE,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );
        when(activityPeriodRepository.existsReportingDateActivityPeriodByReportingDate(reportingDate)).thenReturn(true);
        when(activityPeriodRepository.findReportingDateActivityPeriodsByReportingDateOrderByStartDateAscEndDateAsc(reportingDate))
                .thenReturn(Collections.singletonList(scoring));

        assertTrue(service.isActivityAllowed(reportingDate, PMConstants.ACTIVITY_CAPTURE_EMPLOYEE_SCORES));
        assertTrue(service.isActivityAllowed(reportingDate, PMConstants.ACTIVITY_CAPTURE_AGREED_SCORES));
        assertFalse(service.isActivityAllowed(reportingDate, PMConstants.ACTIVITY_CAPTURE_TARGETS));
        assertFalse(service.isActivityAllowed(reportingDate, PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES));
    }

    @Test
    void preservesExistingWorkflowUntilActivityPeriodsAreConfigured() {
        ReportingDate reportingDate = reportingDate();
        when(activityPeriodRepository.existsReportingDateActivityPeriodByReportingDate(reportingDate)).thenReturn(false);

        assertTrue(service.isActivityAllowed(reportingDate, PMConstants.ACTIVITY_CAPTURE_TARGETS));
        assertTrue(service.isActivityAllowed(reportingDate, PMConstants.ACTIVITY_CAPTURE_MODERATED_SCORES));
    }

    private ReportingDate reportingDate() {
        ReportingPeriod reportingPeriod = new ReportingPeriod();
        reportingPeriod.setId(10L);
        ReportingDate reportingDate = new ReportingDate();
        reportingDate.setId(20L);
        reportingDate.setReportingPeriod(reportingPeriod);
        reportingDate.setStatus(PMConstants.REPORTING_DATE_STATUS_OPEN);
        return reportingDate;
    }

    private ReportingDateActivityPeriod period(ReportingDate reportingDate,
                                               String activity,
                                               LocalDate startDate,
                                               LocalDate endDate) {
        ReportingDateActivityPeriod period = new ReportingDateActivityPeriod();
        period.setReportingDate(reportingDate);
        period.setActivityType(activity);
        period.setStartDate(startDate.toString());
        period.setEndDate(endDate.toString());
        return period;
    }
}
