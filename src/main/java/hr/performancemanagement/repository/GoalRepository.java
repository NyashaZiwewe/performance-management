package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.hibernate.loader.Loader.SELECT;
import static org.springframework.http.HttpHeaders.FROM;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findGoalsByPerspectiveId(long perspectiveId);
    Goal findGoalById(long id);

    List<Goal> findGoalsByScorecardIdOrderByPerspectiveAscStrategicObjective(long scorecardId);

    @Query("SELECT SUM(allocatedWeight) FROM Goal WHERE scorecardId = :scorecardId")
    double sumAllocatedWeight(@Param("scorecardId") long scorecardId);

    @Query("SELECT AVG(employeeScore) FROM Goal WHERE scorecardId = :scorecardId")
    double averageEmployeeScore(@Param("scorecardId") long scorecardId);

    @Query("SELECT AVG(managerScore) FROM Goal WHERE scorecardId = :scorecardId")
    double averageManagerScore(@Param("scorecardId") long scorecardId);

    @Query("SELECT AVG(actualScore) FROM Goal WHERE scorecardId = :scorecardId")
    double averageModeratedScore(@Param("scorecardId") long scorecardId);
}
