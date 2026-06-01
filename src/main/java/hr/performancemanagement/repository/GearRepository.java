package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Gear;
import hr.performancemanagement.entities.ReportingPeriod;
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
    List<Gear> findGearsByClientIdAndReportingPeriod(long clientId, ReportingPeriod reportingPeriod);
    List<Gear> findGearsByClientIdAndCategory(long clientId, String category);
    List<Gear> findGearsByClientIdAndCategoryAndReportingPeriod(long clientId, String category, ReportingPeriod reportingPeriod);
    @Query("SELECT g FROM Gear g WHERE g.clientId = :clientId AND g.category = :category " +
            "AND (g.reportingPeriod = :reportingPeriod OR g.reportingPeriod IS NULL)")
    List<Gear> findGearsByClientIdAndCategoryAndReportingPeriodOrUnassigned(@Param("clientId") long clientId,
                                                                             @Param("category") String category,
                                                                             @Param("reportingPeriod") ReportingPeriod reportingPeriod);

    @Query(value = "SELECT DISTINCT(g) FROM Gear g LEFT JOIN Goal gl ON gl.gear = g LEFT JOIN Outcome oc ON oc.goal = gl LEFT JOIN Output op ON op.outcome = oc WHERE op.scorecard = :scorecard AND g.category = :category")
    List<Gear> selectedGearsByScorecard(@Param("scorecard") Scorecard scorecard, @Param("category") String category);

    @Query(value = "SELECT DISTINCT(g) FROM Gear g LEFT JOIN Goal gl ON gl.gear = g LEFT JOIN Pillar p ON p.goal = gl LEFT JOIN Outcome oc ON oc.pillar = p LEFT JOIN Output op ON op.outcome = oc WHERE op.scorecard = :scorecard AND g.category = :category")
    List<Gear> selectedProgrammesByScorecard(@Param("scorecard") Scorecard scorecard, @Param("category") String category);

    @Query("SELECT SUM(o.allocatedWeight) FROM Output o WHERE o.scorecard.id = :scorecardId AND o.outcome.goal.gear = :gear")
    double sumGearAllocatedWeight(@Param("scorecardId") long scorecardId, @Param("gear") Gear gear);

    Gear findGearById(long id);
}
