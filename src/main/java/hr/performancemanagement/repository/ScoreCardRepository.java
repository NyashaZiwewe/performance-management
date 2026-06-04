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
    List<Scorecard> findScorecardsByClient_ClientId(long clientId);
    List<Scorecard> findScorecardsByClient_ClientIdAndReportingPeriod_Id(long clientId, long reportingPeriodId);
    List<Scorecard> findScorecardsByClient_ClientIdAndStatus(long clientId, String status);
    List<Scorecard> findScorecardsByOwner(Account owner);
    List<Scorecard> findScorecardsByReportingPeriod(ReportingPeriod reportingPeriod);
    List<Scorecard> findScorecardsByReportingPeriodAndClient_ClientId(ReportingPeriod reportingPeriod, long clientId);
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
    @Query("SELECT coalesce(AVG(t.allocatedWeight), 0) " +
            "FROM Target t " +
            "LEFT JOIN t.goal directGoal " +
            "LEFT JOIN t.output targetOutput " +
            "LEFT JOIN targetOutput.outcome targetOutcome " +
            "LEFT JOIN targetOutcome.goal outputGoal " +
            "LEFT JOIN targetOutcome.pillar outputPillar " +
            "LEFT JOIN outputPillar.goal pillarGoal " +
            "WHERE directGoal.strategicObjective = :strategicObjective " +
            "OR outputGoal.strategicObjective = :strategicObjective " +
            "OR pillarGoal.strategicObjective = :strategicObjective")
    Double findAverageAllocatedWeightPerStrategicObjective(@Param("strategicObjective") StrategicObjective strategicObjective);

    @Nullable
    @Query("SELECT coalesce(AVG(s.weightedScore), 0) " +
            "FROM Score s " +
            "LEFT JOIN s.target t " +
            "LEFT JOIN t.goal directGoal " +
            "LEFT JOIN t.output targetOutput " +
            "LEFT JOIN targetOutput.outcome targetOutcome " +
            "LEFT JOIN targetOutcome.goal targetOutputGoal " +
            "LEFT JOIN targetOutcome.pillar targetOutputPillar " +
            "LEFT JOIN targetOutputPillar.goal targetPillarGoal " +
            "LEFT JOIN s.output scoreOutput " +
            "LEFT JOIN scoreOutput.outcome scoreOutcome " +
            "LEFT JOIN scoreOutcome.goal scoreOutputGoal " +
            "LEFT JOIN scoreOutcome.pillar scoreOutputPillar " +
            "LEFT JOIN scoreOutputPillar.goal scorePillarGoal " +
            "WHERE directGoal.strategicObjective = :strategicObjective " +
            "OR targetOutputGoal.strategicObjective = :strategicObjective " +
            "OR targetPillarGoal.strategicObjective = :strategicObjective " +
            "OR scoreOutputGoal.strategicObjective = :strategicObjective " +
            "OR scorePillarGoal.strategicObjective = :strategicObjective")
    Double findAverageWeightedScorePerStrategicObjective(@Param("strategicObjective") StrategicObjective strategicObjective);

    @Query("SELECT COALESCE(directGoal.strategicObjective.id, outputGoal.strategicObjective.id, pillarGoal.strategicObjective.id), " +
            "coalesce(AVG(t.allocatedWeight), 0) " +
            "FROM Target t " +
            "LEFT JOIN t.goal directGoal " +
            "LEFT JOIN t.output targetOutput " +
            "LEFT JOIN targetOutput.outcome targetOutcome " +
            "LEFT JOIN targetOutcome.goal outputGoal " +
            "LEFT JOIN targetOutcome.pillar outputPillar " +
            "LEFT JOIN outputPillar.goal pillarGoal " +
            "WHERE COALESCE(directGoal.strategicObjective.id, outputGoal.strategicObjective.id, pillarGoal.strategicObjective.id) IN :strategicObjectiveIds " +
            "GROUP BY COALESCE(directGoal.strategicObjective.id, outputGoal.strategicObjective.id, pillarGoal.strategicObjective.id)")
    List<Object[]> findAverageAllocatedWeightPerStrategicObjectiveIds(@Param("strategicObjectiveIds") List<Long> strategicObjectiveIds);

    @Query("SELECT COALESCE(directGoal.strategicObjective.id, targetOutputGoal.strategicObjective.id, targetPillarGoal.strategicObjective.id, scoreOutputGoal.strategicObjective.id, scorePillarGoal.strategicObjective.id), " +
            "coalesce(AVG(s.weightedScore), 0) " +
            "FROM Score s " +
            "LEFT JOIN s.target t " +
            "LEFT JOIN t.goal directGoal " +
            "LEFT JOIN t.output targetOutput " +
            "LEFT JOIN targetOutput.outcome targetOutcome " +
            "LEFT JOIN targetOutcome.goal targetOutputGoal " +
            "LEFT JOIN targetOutcome.pillar targetOutputPillar " +
            "LEFT JOIN targetOutputPillar.goal targetPillarGoal " +
            "LEFT JOIN s.output scoreOutput " +
            "LEFT JOIN scoreOutput.outcome scoreOutcome " +
            "LEFT JOIN scoreOutcome.goal scoreOutputGoal " +
            "LEFT JOIN scoreOutcome.pillar scoreOutputPillar " +
            "LEFT JOIN scoreOutputPillar.goal scorePillarGoal " +
            "WHERE COALESCE(directGoal.strategicObjective.id, targetOutputGoal.strategicObjective.id, targetPillarGoal.strategicObjective.id, scoreOutputGoal.strategicObjective.id, scorePillarGoal.strategicObjective.id) IN :strategicObjectiveIds " +
            "GROUP BY COALESCE(directGoal.strategicObjective.id, targetOutputGoal.strategicObjective.id, targetPillarGoal.strategicObjective.id, scoreOutputGoal.strategicObjective.id, scorePillarGoal.strategicObjective.id)")
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
