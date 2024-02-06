package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findGoalsByGear_ClientId(long clientId);
    Goal findGoalById(long id);

    @Query(value = "SELECT DISTINCT(oc.goal) FROM Output o LEFT JOIN Outcome oc WHERE o.scorecard.id = :scorecardId")
    List<Goal> goalsByScorecard(@Param("scorecardId") long scorecardId);
    @Query(value = "SELECT coalesce(AVG(t.weightedScore), 0) FROM Target t LEFT JOIN Output o ON t.output = o WHERE o.outcome.goal = :goal")
    Double weightedScoreByScorecardAndGoal(@Param("goal") Goal goal);
}
