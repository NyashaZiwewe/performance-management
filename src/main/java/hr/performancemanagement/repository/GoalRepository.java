package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findGoalsByPerspectiveId(long perspectiveId);
    Goal findGoalById(long id);
    List<Goal> findGoalsByScorecardIdOrderByPerspective(long scorecardId);
    List<Goal> findGoalsByScorecardIdOrderByPerspectiveAscStrategicObjective(long scorecardId);

    @Query("SELECT COALESCE(SUM(t.allocatedWeight), 0.0) FROM Target t LEFT JOIN Goal g ON t.goal = g WHERE g.scorecardId = :scorecardId")
    double sumAllocatedWeight(@Param("scorecardId") long scorecardId);

    @Query("SELECT COALESCE(AVG(s.employeeScore), 0.0) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Goal g ON t.goal = g WHERE g.scorecardId = :scorecardId")
    double averageEmployeeScore(@Param("scorecardId") long scorecardId);

    @Query("SELECT COALESCE(AVG(s.managerScore), 0.0) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Goal g ON t.goal = g WHERE g.scorecardId = :scorecardId")
    double averageManagerScore(@Param("scorecardId") long scorecardId);

    @Query("SELECT COALESCE(AVG(s.agreedScore), 0.0) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Goal g ON t.goal = g WHERE g.scorecardId = :scorecardId")
    double averageAgreedScore(@Param("scorecardId") long scorecardId);

    @Query("SELECT COALESCE(AVG(s.moderatedScore), 0.0) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Goal g ON t.goal = g WHERE g.scorecardId = :scorecardId")
    double averageModeratedScore(@Param("scorecardId") long scorecardId);
}
