package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.entities.ScorecardReportingDateStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScorecardReportingDateStageRepository extends JpaRepository<ScorecardReportingDateStage, Long> {

    ScorecardReportingDateStage findScorecardReportingDateStageByScorecardAndReportingDate(Scorecard scorecard, ReportingDate reportingDate);

    List<ScorecardReportingDateStage> findScorecardReportingDateStagesByScorecardOrderByReportingDate_IdAsc(Scorecard scorecard);

    List<ScorecardReportingDateStage> findScorecardReportingDateStagesByReportingDate(ReportingDate reportingDate);

    List<ScorecardReportingDateStage> findScorecardReportingDateStagesByReportingDateInAndStatusIn(
            List<ReportingDate> reportingDates,
            List<String> statuses
    );

    void deleteScorecardReportingDateStagesByReportingDate(ReportingDate reportingDate);
}
