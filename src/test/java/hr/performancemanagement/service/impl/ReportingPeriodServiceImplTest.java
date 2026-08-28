package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.repository.ReportingPeriodRepository;
import hr.performancemanagement.service.api.CommonService;
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
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingPeriodServiceImplTest {

    @Mock private ReportingPeriodRepository reportingPeriodRepository;
    @Mock private ReportingDateRepository reportingDateRepository;
    @Mock private CommonService commonService;
    @Mock private ScorecardLifecycleService scorecardLifecycleService;

    private ReportingPeriodServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportingPeriodServiceImpl();
        ReflectionTestUtils.setField(service, "reportingPeriodRepository", reportingPeriodRepository);
        ReflectionTestUtils.setField(service, "reportingDateRepository", reportingDateRepository);
        ReflectionTestUtils.setField(service, "cs", commonService);
        ReflectionTestUtils.setField(service, "scorecardLifecycleService", scorecardLifecycleService);
    }

    @Test
    void validateSingleActiveReportingPeriodRejectsMultipleActivePeriods() {
        ReportingPeriod first = reportingPeriod(10L, 7L);
        ReportingPeriod second = reportingPeriod(11L, 7L);
        when(reportingPeriodRepository.findReportingPeriodsByClientIdAndStatusOrderByStartDateDescEndDateDescIdDesc(
                7L,
                PMConstants.STATUS_ACTIVE
        )).thenReturn(Arrays.asList(first, second));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateSingleActiveReportingPeriod(7L)
        );

        assertTrue(exception.getMessage().contains("Multiple active reporting periods were detected"));
        assertTrue(exception.getMessage().contains("ID 10"));
        assertTrue(exception.getMessage().contains("ID 11"));
    }

    @Test
    void getActiveReportingPeriodDoesNotChooseFromMultipleActivePeriods() {
        ReportingPeriod first = reportingPeriod(10L, 7L);
        ReportingPeriod second = reportingPeriod(11L, 7L);
        when(commonService.getConfiguredClientId()).thenReturn(7L);
        when(reportingPeriodRepository.findReportingPeriodsByClientIdAndStatusOrderByStartDateDescEndDateDescIdDesc(
                7L,
                PMConstants.STATUS_ACTIVE
        )).thenReturn(Arrays.asList(first, second));

        assertNull(service.getActiveReportingPeriod());
    }

    @Test
    void saveReportingPeriodRejectsSecondActivePeriodForClient() {
        ReportingPeriod existing = reportingPeriod(10L, 7L);
        ReportingPeriod requested = reportingPeriod(11L, 7L);
        when(reportingPeriodRepository.findReportingPeriodsByClientIdAndStatusOrderByStartDateDescEndDateDescIdDesc(
                7L,
                PMConstants.STATUS_ACTIVE
        )).thenReturn(Collections.singletonList(existing));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveReportingPeriod(requested)
        );

        assertTrue(exception.getMessage().contains("Cannot activate this reporting period"));
        assertTrue(exception.getMessage().contains("ID 10"));
        verify(reportingPeriodRepository, never()).save(requested);
    }

    @Test
    void getActiveReportingPeriodReturnsSingleCurrentActivePeriod() {
        ReportingPeriod active = reportingPeriod(10L, 7L);
        when(commonService.getConfiguredClientId()).thenReturn(7L);
        when(reportingPeriodRepository.findReportingPeriodsByClientIdAndStatusOrderByStartDateDescEndDateDescIdDesc(
                7L,
                PMConstants.STATUS_ACTIVE
        )).thenReturn(Collections.singletonList(active));

        assertEquals(active, service.getActiveReportingPeriod());
    }

    private ReportingPeriod reportingPeriod(long id, long clientId) {
        ReportingPeriod reportingPeriod = new ReportingPeriod();
        reportingPeriod.setId(id);
        reportingPeriod.setClientId(clientId);
        reportingPeriod.setStartDate(LocalDate.now().minusDays(1).toString());
        reportingPeriod.setEndDate(LocalDate.now().plusDays(1).toString());
        reportingPeriod.setStatus(PMConstants.STATUS_ACTIVE);
        reportingPeriod.setModel("standard");
        return reportingPeriod;
    }
}
