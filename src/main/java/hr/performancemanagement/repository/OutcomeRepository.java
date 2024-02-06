package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Gear;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.Outcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutcomeRepository extends JpaRepository<Outcome, Long> {
    List<Outcome> findOutcomesByGoal(Gear gear);
    Outcome findOutcomeById(long id);
    List<Outcome> findOutcomesByGoal(Goal goal);
    List<Outcome> findOutcomesByGoal(long scorecardId);

    @Query("SELECT SUM(t.allocatedWeight) FROM Target t LEFT JOIN Output o ON t.output = o WHERE o.scorecard.id = :scorecardId")
    double sumAllocatedWeight(@Param("scorecardId") long scorecardId);

    @Query("SELECT AVG(s.employeeScore) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Output o ON t.output = o WHERE o.scorecard.id = :scorecardId")
    double averageEmployeeScore(@Param("scorecardId") long scorecardId);

    @Query("SELECT AVG(s.managerScore) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Output o ON t.output = o WHERE o.scorecard.id = :scorecardId")
    double averageManagerScore(@Param("scorecardId") long scorecardId);

    @Query("SELECT AVG(s.agreedScore) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Output o ON t.output = o WHERE o.scorecard.id = :scorecardId")
    double averageAgreedScore(@Param("scorecardId") long scorecardId);

    @Query("SELECT AVG(s.moderatedScore) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Output o ON t.output = o WHERE o.scorecard.id = :scorecardId")
    double averageModeratedScore(@Param("scorecardId") long scorecardId);
}
