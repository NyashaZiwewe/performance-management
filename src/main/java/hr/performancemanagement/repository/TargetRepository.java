package hr.performancemanagement.repository;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.Output;
import hr.performancemanagement.entities.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TargetRepository extends JpaRepository<Target, Long> {
    List<Target> findTargetsByGoalId(long goalId);
    @Query("SELECT t FROM Target t " +
            "LEFT JOIN FETCH t.goal directGoal " +
            "LEFT JOIN FETCH t.output o " +
            "LEFT JOIN FETCH o.outcome oc " +
            "LEFT JOIN FETCH oc.goal g " +
            "LEFT JOIN FETCH oc.pillar p " +
            "LEFT JOIN FETCH p.goal pg " +
            "WHERE o.scorecard.id = :scorecardId OR directGoal.scorecardId = :scorecardId " +
            "ORDER BY COALESCE(directGoal.perspective.id, g.perspective.id, pg.perspective.id), " +
            "COALESCE(directGoal.strategicObjective.id, g.strategicObjective.id, pg.strategicObjective.id), " +
            "COALESCE(directGoal.id, g.id, pg.id), t.id")
    List<Target> findTargetsByScorecardId(@Param("scorecardId") long scorecardId);
    int countTargetsByGoal(Goal goal);
    List<Target> findTargetsByOutput(Output output);
    Target findTargetById(long id);
    int countTargetsByOutput(Output output);

    @Query("SELECT DISTINCT(t.unit) FROM Target t")
    List<String> listAllUnits();

}
