package hr.performancemanagement.repository;

import hr.performancemanagement.entities.OverallComment;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OverallCommentRepository extends JpaRepository<OverallComment, Long> {

    boolean existsOverallCommentByScorecardAndAndReportingDate(Scorecard scorecard, ReportingDate reportingDate);
    OverallComment findOverallCommentByScorecardAndReportingDate(Scorecard scorecard, ReportingDate reportingDate);
    List<OverallComment> findOverallCommentsByScorecard(Scorecard scorecard);
}
