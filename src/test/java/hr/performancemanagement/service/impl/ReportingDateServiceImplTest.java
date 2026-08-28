package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.repository.ReportingPeriodRepository;
import hr.performancemanagement.repository.ScorecardReportingDateStageRepository;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.ReportingDateActivityPeriodService;
import hr.performancemanagement.service.api.ScorecardLifecycleService;
import hr.performancemanagement.utils.constants.PMConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingDateServiceImplTest {

    @Mock private ReportingDateRepository reportingDateRepository;
    @Mock private ReportingPeriodRepository reportingPeriodRepository;
    @Mock private ScorecardReportingDateStageRepository scorecardReportingDateStageRepository;
    @Mock private CommonService commonService;
    @Mock private ScorecardLifecycleService scorecardLifecycleService;
    @Mock private ReportingDateActivityPeriodService reportingDateActivityPeriodService;

    private ReportingDateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportingDateServiceImpl();
        ReflectionTestUtils.setField(service, "reportingDateRepository", reportingDateRepository);
        ReflectionTestUtils.setField(service, "reportingPeriodRepository", reportingPeriodRepository);
        ReflectionTestUtils.setField(service, "scorecardReportingDateStageRepository", scorecardReportingDateStageRepository);
        ReflectionTestUtils.setField(service, "cs", commonService);
        ReflectionTestUtils.setField(service, "scorecardLifecycleService", scorecardLifecycleService);
        ReflectionTestUtils.setField(service, "reportingDateActivityPeriodService", reportingDateActivityPeriodService);
    }

    @Test
    void validateSingleOpenOrActiveReportingDateRejectsMultipleCurrentDates() {
        ReportingPeriod reportingPeriod = reportingPeriod(10L, 7L);
        ReportingDate first = reportingDate(20L, reportingPeriod, "2026-03-31");
        ReportingDate second = reportingDate(21L, reportingPeriod, "2026-06-30");
        when(reportingDateRepository.findReportingDatesByReportingPeriod_ClientIdAndStatusInOrderByDateDescIdDesc(
                7L,
                Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
        )).thenReturn(Arrays.asList(first, second));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateSingleOpenOrActiveReportingDate(7L)
        );

        assertTrue(exception.getMessage().contains("Multiple OPEN/ACTIVE reporting dates were detected"));
        assertTrue(exception.getMessage().contains("ID 20"));
        assertTrue(exception.getMessage().contains("ID 21"));
    }

    @Test
    void getActiveReportingDateDoesNotChooseFromMultipleCurrentDates() {
        ReportingPeriod reportingPeriod = reportingPeriod(10L, 7L);
        Account loggedUser = new Account();
        loggedUser.setClientId(7L);
        when(commonService.getLoggedUser()).thenReturn(loggedUser);
        when(reportingDateRepository.findReportingDatesByReportingPeriod_ClientIdAndStatusInOrderByDateDescIdDesc(
                7L,
                Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
        )).thenReturn(Arrays.asList(
                reportingDate(20L, reportingPeriod, "2026-03-31"),
                reportingDate(21L, reportingPeriod, "2026-06-30")
        ));

        assertNull(service.getActiveReportingDate());
    }

    @Test
    void saveOpenReportingDateClosesCompetingOpenDatesForClient() {
        ReportingPeriod reportingPeriod = reportingPeriod(10L, 7L);
        ReportingDate requested = reportingDate(20L, reportingPeriod, "2026-03-31");
        ReportingDate existing = reportingDate(21L, reportingPeriod, "2026-06-30");
        when(reportingPeriodRepository.findReportingPeriodById(10L)).thenReturn(reportingPeriod);
        when(reportingDateRepository.findReportingDatesByReportingPeriod_ClientIdAndStatusInOrderByDateDescIdDesc(
                7L,
                Arrays.asList(PMConstants.REPORTING_DATE_STATUS_OPEN, PMConstants.STATUS_ACTIVE)
        )).thenReturn(Arrays.asList(requested, existing));

        service.saveReportingDate(requested);

        assertEquals(PMConstants.REPORTING_DATE_STATUS_CLOSED, existing.getStatus());
        assertEquals(PMConstants.REPORTING_DATE_STATUS_OPEN, requested.getStatus());
        verify(reportingDateRepository).save(existing);
        verify(reportingDateRepository).save(requested);
        verify(scorecardLifecycleService).closeScorecardReportingDateStages(existing);
    }

    private ReportingPeriod reportingPeriod(long id, long clientId) {
        ReportingPeriod reportingPeriod = new ReportingPeriod();
        reportingPeriod.setId(id);
        reportingPeriod.setClientId(clientId);
        reportingPeriod.setStatus(PMConstants.STATUS_ACTIVE);
        reportingPeriod.setStartDate(LocalDate.now().minusDays(1).toString());
        reportingPeriod.setEndDate(LocalDate.now().plusDays(1).toString());
        reportingPeriod.setModel("standard");
        return reportingPeriod;
    }

    private ReportingDate reportingDate(long id, ReportingPeriod reportingPeriod, String endDate) {
        ReportingDate reportingDate = new ReportingDate();
        reportingDate.setId(id);
        reportingDate.setReportingPeriod(reportingPeriod);
        reportingDate.setEndDate(endDate);
        reportingDate.setStatus(PMConstants.REPORTING_DATE_STATUS_OPEN);
        return reportingDate;
    }
}
