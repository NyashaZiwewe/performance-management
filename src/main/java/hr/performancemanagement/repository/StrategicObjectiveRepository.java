package hr.performancemanagement.repository;

import hr.performancemanagement.entities.StrategicObjective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StrategicObjectiveRepository extends JpaRepository<StrategicObjective, Long> {

    List<StrategicObjective> findStrategicObjectivesByReportingPeriodId(long reportingPeriodId);
    long countByReportingPeriodId(long reportingPeriodId);
    StrategicObjective findStrategicObjectiveById(long id);

    @Query(value = "SELECT DISTINCT(g.strategicObjective) FROM Goal g WHERE g.scorecardId = :scorecardId")
    List<StrategicObjective> strategicObjectivesByScorecard(@Param("scorecardId") long scorecardId);
    @Query("SELECT coalesce(AVG(s.weightedScore), 0) " +
            "FROM Score s " +
            "LEFT JOIN s.target t " +
            "LEFT JOIN t.goal g " +
            "LEFT JOIN s.output o " +
            "LEFT JOIN o.outcome oc " +
            "LEFT JOIN oc.goal og " +
            "LEFT JOIN oc.pillar p " +
            "LEFT JOIN p.goal pg " +
            "WHERE g.strategicObjective = :strategicObjective " +
            "OR og.strategicObjective = :strategicObjective " +
            "OR pg.strategicObjective = :strategicObjective")
    Double weightedScoreByScorecardAndStrategicObjective(@Param("strategicObjective") StrategicObjective strategicObjective);
}
