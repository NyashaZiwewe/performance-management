package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Perspective;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ReportingPeriodRepository;
import hr.performancemanagement.utils.constants.PMConstants;
import java.util.ArrayList;
import java.util.List;

public interface ReportingPeriodService {
    ReportingPeriod getReportingPeriodById(long id);
    ReportingPeriod getActiveReportingPeriod();
    List<ReportingPeriod> listAllReportingPeriods();
    List<ReportingPeriod> listAllReportingPeriods(long clientId);
    void saveReportingPeriod(ReportingPeriod reportingPeriod);
    void deleteReportingPeriod(ReportingPeriod reportingPeriod);
}
