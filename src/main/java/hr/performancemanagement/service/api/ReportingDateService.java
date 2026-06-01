package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ReportingDateRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import java.util.ArrayList;
import java.util.List;

public interface ReportingDateService {
    ReportingDate getReportingDateById(long id);
    ReportingDate getActiveReportingDate();
    List<ReportingDate> listOpenOrActiveReportingDates(long clientId);
    boolean hasMultipleOpenOrActiveReportingDates(long clientId);
    boolean isReportingDateOpen(ReportingDate reportingDate);
    List<ReportingDate> listAllReportingDates(ReportingPeriod reportingPeriod);
    long countReportingDates(long reportingPeriodId);
    void saveReportingDate(ReportingDate reportingDate);
    void deleteReportingDate(ReportingDate reportingDate);
}
