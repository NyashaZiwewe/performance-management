package hr.performancemanagement.repository;

import hr.performancemanagement.entities.OverallScore;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OverallScoreRepository extends JpaRepository<OverallScore, Long> {

    OverallScore findOverallScoreByScorecardAndReportingDate(Scorecard scorecard, ReportingDate reportingDate);

    List<OverallScore> getOverallScoresByScorecard(Scorecard scorecard);
}
