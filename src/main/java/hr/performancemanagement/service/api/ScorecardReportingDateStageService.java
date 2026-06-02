package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.entities.ScorecardReportingDateStage;

public interface ScorecardReportingDateStageService {

    ScorecardReportingDateStage getOrCreateStage(Scorecard scorecard, ReportingDate reportingDate);

    ScorecardReportingDateStage moveToRole(Scorecard scorecard, ReportingDate reportingDate, String roleKey);

    String getCurrentRoleKey(Scorecard scorecard, ReportingDate reportingDate);
}

