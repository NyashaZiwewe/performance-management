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
    List<Scorecard> findScorecardsByOwner(Account owner);
    List<Scorecard> findScorecardsByReportingPeriodAndClientId(ReportingPeriod reportingPeriod, long clientId);
    List<Scorecard> findScorecardsByReportingPeriodAndOwner(ReportingPeriod reportingPeriod, Account account);
    List<Scorecard> findScorecardsByReportingPeriodAndOwner_Supervisor(ReportingPeriod reportingPeriod, Account account);
    List<Scorecard> findScorecardsByReportingPeriodAndOwner_Department(ReportingPeriod reportingPeriod, Department department);
    Scorecard findScorecardById(long id);
    int countScorecardsByOwnerAndReportingPeriod(Account owner, ReportingPeriod reportingPeriod);

    @Nullable
    @Query("SELECT coalesce(AVG(s.employeeScore), 0) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Goal g ON t.goal = g WHERE g.scorecardId = :scorecardId")
    Double findAverageEmployeeScore(@Param("scorecardId") long scorecardId);
    @Nullable
    @Query("SELECT coalesce(AVG(s.managerScore),0) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Goal g ON t.goal = g WHERE g.scorecardId = :scorecardId")
    Double findAverageManagerScore(@Param("scorecardId") long scorecardId);
    @Nullable
    @Query("SELECT coalesce(AVG(s.actualScore), 0) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Goal g ON t.goal = g WHERE g.scorecardId = :scorecardId")
    Double findAverageActualScore(@Param("scorecardId") long scorecardId);

    @Nullable
    @Query("SELECT coalesce(AVG(t.allocatedWeight), 0) FROM Target t LEFT JOIN Goal g ON t.goal = g WHERE g.strategicObjective = :strategicObjective")
    Double findAverageAllocatedWeightPerStrategicObjective(@Param("strategicObjective") StrategicObjective strategicObjective);

    @Nullable
    @Query("SELECT coalesce(AVG(s.weightedScore), 0) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Goal g ON t.goal = g WHERE g.strategicObjective = :strategicObjective")
    Double findAverageWeightedScorePerStrategicObjective(@Param("strategicObjective") StrategicObjective strategicObjective);

    @Nullable
    @Query("SELECT id FROM Scorecard WHERE owner = :owner AND status = 'ACTIVE'")
    Long findEmployeeActiveScorecardId(@Param("owner") Account owner);

}
