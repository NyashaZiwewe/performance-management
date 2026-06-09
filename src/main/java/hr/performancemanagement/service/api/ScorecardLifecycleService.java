package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;

public interface ScorecardLifecycleService {
    void prepareScorecardForSave(Scorecard scorecard);
    void applyStatusTransition(Scorecard scorecard, String recordStatus, String approvalStatus, String lockStatus);
    int closeScorecardsForInactiveReportingPeriod(ReportingPeriod reportingPeriod);
    int closeScorecardReportingDateStages(ReportingDate reportingDate);
}
