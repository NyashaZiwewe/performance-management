package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Gear;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.entities.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GearRepository extends JpaRepository<Gear, Long> {

    List<Gear> findGearsByClientId(long clientId);

    @Query(value = "SELECT DISTINCT(g) FROM Gear g LEFT JOIN Goal gl ON gl.gear = g LEFT JOIN Outcome oc ON oc.goal = gl LEFT JOIN Output op ON op.outcome = oc WHERE op.scorecard = :scorecard")
    List<Gear> selectedGearsByScorecard(@Param("scorecard") Scorecard scorecard);

    @Query("SELECT SUM(t.allocatedWeight) FROM Target t LEFT JOIN Output o ON t.output = o WHERE o.scorecard.id = :scorecardId AND o.outcome.goal.gear = :gear")
    double sumGearAllocatedWeight(@Param("scorecardId") long scorecardId, @Param("gear") Gear gear);

    Gear findGearById(long id);
}
