package hr.performancemanagement.repository;

import hr.performancemanagement.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoreCardRepository extends JpaRepository<Scorecard, Long> {
    List<Scorecard> findScorecardsByClientId(long clientId);
    List<Scorecard> findScorecardsByClientIdAndReportingPeriod_Id(long clientId, long reportingPeriodId);
    List<Scorecard> findScorecardsByClientIdAndStatus(long clientId, String status);
    List<Scorecard> findScorecardsByOwner(Account owner);
    List<Scorecard> findScorecardsByReportingPeriodAndClientId(ReportingPeriod reportingPeriod, long clientId);
    List<Scorecard> findScorecardsByReportingPeriodAndOwner(ReportingPeriod reportingPeriod, Account account);
    List<Scorecard> findScorecardsByReportingPeriodAndOwner_Supervisor(ReportingPeriod reportingPeriod, Account account);
    List<Scorecard> findScorecardsByReportingPeriodAndOwner_Department(ReportingPeriod reportingPeriod, Department department);
    Scorecard findScorecardById(long id);
    int countScorecardsByOwnerAndReportingPeriod(Account owner, ReportingPeriod reportingPeriod);

    @Nullable
    @Query("SELECT coalesce(AVG(s.employeeScore), 0) FROM Score s " +
            "LEFT JOIN Target t ON s.target = t " +
            "LEFT JOIN Goal g ON t.goal = g " +
            "LEFT JOIN Output o ON s.output = o " +
            "WHERE g.scorecardId = :scorecardId OR o.scorecard.id = :scorecardId")
    Double findAverageEmployeeScore(@Param("scorecardId") long scorecardId);

    @Nullable
    @Query("SELECT coalesce(AVG(s.managerScore),0) FROM Score s " +
            "LEFT JOIN Target t ON s.target = t " +
            "LEFT JOIN Goal g ON t.goal = g " +
            "LEFT JOIN Output o ON s.output = o " +
            "WHERE g.scorecardId = :scorecardId OR o.scorecard.id = :scorecardId")
    Double findAverageManagerScore(@Param("scorecardId") long scorecardId);

    @Nullable
    @Query("SELECT coalesce(AVG(s.agreedScore), 0) FROM Score s " +
            "LEFT JOIN Target t ON s.target = t " +
            "LEFT JOIN Goal g ON t.goal = g " +
            "LEFT JOIN Output o ON s.output = o " +
            "WHERE g.scorecardId = :scorecardId OR o.scorecard.id = :scorecardId")
    Double findAverageAgreedScore(@Param("scorecardId") long scorecardId);

    @Nullable
    @Query("SELECT coalesce(AVG(s.moderatedScore), 0) FROM Score s " +
            "LEFT JOIN Target t ON s.target = t " +
            "LEFT JOIN Goal g ON t.goal = g " +
            "LEFT JOIN Output o ON s.output = o " +
            "WHERE g.scorecardId = :scorecardId OR o.scorecard.id = :scorecardId")
    Double findAverageModeratedScore(@Param("scorecardId") long scorecardId);

    @Nullable
    @Query("SELECT coalesce(AVG(s.weightedScore), 0) FROM Score s " +
            "LEFT JOIN Target t ON s.target = t " +
            "LEFT JOIN Goal g ON t.goal = g " +
            "LEFT JOIN Output o ON s.output = o " +
            "WHERE g.scorecardId = :scorecardId OR o.scorecard.id = :scorecardId")
    Double findAverageWeightedScore(@Param("scorecardId") long scorecardId);

    @Nullable
    @Query("SELECT coalesce(AVG(s.weightedScore), 0) FROM Score s " +
            "LEFT JOIN Target t ON s.target = t " +
            "LEFT JOIN Goal g ON t.goal = g " +
            "LEFT JOIN Output o ON s.output = o " +
            "WHERE s.reportingDate = :date AND (g.scorecardId = :scorecardId OR o.scorecard.id = :scorecardId)")
    Double findAverageWeightedScoreByReportingDate(@Param("date") ReportingDate date, @Param("scorecardId") long scorecardId);

    @Query("SELECT COALESCE(g.scorecardId, o.scorecard.id), " +
            "coalesce(AVG(s.employeeScore), 0), " +
            "coalesce(AVG(s.managerScore), 0), " +
            "coalesce(AVG(s.agreedScore), 0), " +
            "coalesce(AVG(s.moderatedScore), 0), " +
            "coalesce(AVG(s.weightedScore), 0) " +
            "FROM Score s " +
            "LEFT JOIN s.target t " +
            "LEFT JOIN t.goal g " +
            "LEFT JOIN s.output o " +
            "WHERE (g.scorecardId IN :scorecardIds OR o.scorecard.id IN :scorecardIds) " +
            "GROUP BY COALESCE(g.scorecardId, o.scorecard.id)")
    List<Object[]> findScoreAveragesByScorecardIds(@Param("scorecardIds") List<Long> scorecardIds);

    @Query("SELECT COALESCE(g.scorecardId, o.scorecard.id), s.reportingDate.id, coalesce(AVG(s.weightedScore), 0) " +
            "FROM Score s " +
            "LEFT JOIN s.target t " +
            "LEFT JOIN t.goal g " +
            "LEFT JOIN s.output o " +
            "WHERE s.reportingDate.id IN :reportingDateIds " +
            "AND (g.scorecardId IN :scorecardIds OR o.scorecard.id IN :scorecardIds) " +
            "GROUP BY COALESCE(g.scorecardId, o.scorecard.id), s.reportingDate.id")
    List<Object[]> findAverageWeightedScoresByReportingDatesAndScorecardIds(
            @Param("reportingDateIds") List<Long> reportingDateIds,
            @Param("scorecardIds") List<Long> scorecardIds
    );

    @Nullable
    @Query("SELECT coalesce(AVG(t.allocatedWeight), 0) FROM Target t LEFT JOIN Goal g ON t.goal = g WHERE g.strategicObjective = :strategicObjective")
    Double findAverageAllocatedWeightPerStrategicObjective(@Param("strategicObjective") StrategicObjective strategicObjective);

    @Nullable
    @Query("SELECT coalesce(AVG(s.weightedScore), 0) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Goal g ON t.goal = g WHERE g.strategicObjective = :strategicObjective")
    Double findAverageWeightedScorePerStrategicObjective(@Param("strategicObjective") StrategicObjective strategicObjective);

    @Query("SELECT g.strategicObjective.id, coalesce(AVG(t.allocatedWeight), 0) " +
            "FROM Target t LEFT JOIN t.goal g " +
            "WHERE g.strategicObjective.id IN :strategicObjectiveIds " +
            "GROUP BY g.strategicObjective.id")
    List<Object[]> findAverageAllocatedWeightPerStrategicObjectiveIds(@Param("strategicObjectiveIds") List<Long> strategicObjectiveIds);

    @Query("SELECT g.strategicObjective.id, coalesce(AVG(s.weightedScore), 0) " +
            "FROM Score s LEFT JOIN s.target t LEFT JOIN t.goal g " +
            "WHERE g.strategicObjective.id IN :strategicObjectiveIds " +
            "GROUP BY g.strategicObjective.id")
    List<Object[]> findAverageWeightedScorePerStrategicObjectiveIds(@Param("strategicObjectiveIds") List<Long> strategicObjectiveIds);

    @Nullable
    @Query("SELECT coalesce(AVG(o.allocatedWeight), 0) FROM Output o WHERE o.outcome.goal = :goal")
    Double findAverageAllocatedWeightPerGoal(@Param("goal") Goal goal);

    @Nullable
    @Query("SELECT coalesce(AVG(s.weightedScore), 0) FROM Score s LEFT JOIN Output o ON s.output = o WHERE o.outcome.goal = :goal")
    Double findAverageWeightedScorePerGoal(@Param("goal") Goal goal);

    @Nullable
    @Query("SELECT id FROM Scorecard WHERE owner = :owner AND status = 'ACTIVE'")
    Long findEmployeeActiveScorecardId(@Param("owner") Account owner);

    @Nullable
    @Query("SELECT o FROM Output o WHERE o.scorecard = :scorecard")
    List<Output> findOutputsFromScorecard(@Param("scorecard") Scorecard scorecard);


}
