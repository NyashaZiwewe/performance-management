package hr.performancemanagement.repository;

import hr.performancemanagement.entities.OverallScore;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OverallScoreRepository extends JpaRepository<OverallScore, Long> {

    OverallScore findOverallScoreByScorecardAndReportingDate(Scorecard scorecard, ReportingDate reportingDate);

    List<OverallScore> getOverallScoresByScorecard(Scorecard scorecard);

    @Query("SELECT o FROM OverallScore o WHERE o.scorecard.id IN :scorecardIds AND o.reportingDate.id IN :reportingDateIds")
    List<OverallScore> findOverallScoresByScorecardIdsAndReportingDateIds(
            @Param("scorecardIds") List<Long> scorecardIds,
            @Param("reportingDateIds") List<Long> reportingDateIds
    );
}
