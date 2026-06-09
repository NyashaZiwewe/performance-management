package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingDateActivityPeriod;
import hr.performancemanagement.utils.dto.ActivityPeriodProgress;

import java.util.List;

public interface ReportingDateActivityPeriodService {
    ReportingDateActivityPeriod getActivityPeriodById(long id);
    List<ReportingDateActivityPeriod> listActivityPeriods(ReportingDate reportingDate);
    ReportingDateActivityPeriod getCurrentActivityPeriod(ReportingDate reportingDate);
    boolean hasConfiguredActivityPeriods(ReportingDate reportingDate);
    boolean isActivityAllowed(ReportingDate reportingDate, String systemActivity);
    String getCurrentActivityLabel(ReportingDate reportingDate);
    String getCurrentActivitySummary(ReportingDate reportingDate);
    ActivityPeriodProgress getProgress(ReportingDate reportingDate);
    void saveActivityPeriod(ReportingDateActivityPeriod activityPeriod);
    void deleteActivityPeriod(ReportingDateActivityPeriod activityPeriod);
    void deleteActivityPeriods(ReportingDate reportingDate);
}
