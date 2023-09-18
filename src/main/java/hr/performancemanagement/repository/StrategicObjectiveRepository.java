package hr.performancemanagement.repository;

import hr.performancemanagement.entities.StrategicObjective;
import hr.performancemanagement.entities.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StrategicObjectiveRepository extends JpaRepository<StrategicObjective, Long> {

    List<StrategicObjective> findStrategicObjectivesByReportingPeriodId(long reportingPeriodId);
    StrategicObjective findStrategicObjectiveById(long id);

    @Query(value = "SELECT DISTINCT(g.strategicObjective) FROM Goal g WHERE g.scorecardId = :scorecardId")
    List<StrategicObjective> strategicObjectivesByScorecard(@Param("scorecardId") long scorecardId);
    @Query(value = "SELECT coalesce(AVG(t.weightedScore), 0) FROM Target t LEFT JOIN Goal g ON t.goal = g WHERE g.strategicObjective = :strategicObjective")
    Double weightedScoreByScorecardAndStrategicObjective(@Param("strategicObjective") StrategicObjective strategicObjective);
}
