package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.ReportingPeriod;
import java.util.List;

public interface ReportingPeriodService {
    ReportingPeriod getReportingPeriodById(long id);
    ReportingPeriod getActiveReportingPeriod();
    void validateSingleActiveReportingPeriod(long clientId);
    List<ReportingPeriod> listAllReportingPeriods();
    List<ReportingPeriod> listAllReportingPeriods(long clientId);
    void saveReportingPeriod(ReportingPeriod reportingPeriod);
    void deleteReportingPeriod(ReportingPeriod reportingPeriod);
}
