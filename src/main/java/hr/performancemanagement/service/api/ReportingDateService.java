package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import java.util.List;

public interface ReportingDateService {
    ReportingDate getReportingDateById(long id);
    ReportingDate getActiveReportingDate();
    List<ReportingDate> listOpenOrActiveReportingDates(long clientId);
    boolean hasMultipleOpenOrActiveReportingDates(long clientId);
    void validateSingleOpenOrActiveReportingDate(long clientId);
    boolean isReportingDateOpen(ReportingDate reportingDate);
    List<ReportingDate> listAllReportingDates(ReportingPeriod reportingPeriod);
    long countReportingDates(long reportingPeriodId);
    void saveReportingDate(ReportingDate reportingDate);
    void deleteReportingDate(ReportingDate reportingDate);
}
