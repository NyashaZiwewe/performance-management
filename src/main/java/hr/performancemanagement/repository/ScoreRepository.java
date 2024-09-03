package hr.performancemanagement.repository;

import hr.performancemanagement.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {

    List<Score> findScoresByOutput(Target target);
    Score findScoreById(long id);
    boolean existsScoresByOutputAndReportingDate(Output output, ReportingDate reportingDate);
    Score findScoreByOutputAndReportingDate(Output output, ReportingDate reportingDate);

//    @Query("SELECT coalesce(AVG(employeeScore), 0) FROM Score WHERE target = :target")
//    double averageEmployeeScore(@Param("target") long target);
//
//    @Query("SELECT coalesce(AVG(s.managerScore), 0) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Goal g ON t.goal = g WHERE g.scorecardId = :scorecardId")
//    double averageManagerScore(@Param("scorecardId") long scorecardId);
//
//    @Query(value = "SELECT coalesce(AVG(s.actualScore), 0) FROM Score s LEFT JOIN Target t ON s.target = t LEFT JOIN Goal g ON t.goal = g WHERE g.scorecardId = :scorecardId")
//    double averageActualScore(@Param("scorecardId") long scorecardId);
//
//    @Query(value = "SELECT coalesce(AVG(s.employeeScore), 0) FROM Score s WHERE s.target = :target")
//    double averageEmployeeScoreForTarget(@Param("target") Target target);
//
//    @Query(value = "SELECT coalesce(AVG(s.managerScore), 0) FROM Score s WHERE s.target = :target")
//    double averageManagerScoreForTarget(@Param("target") Target target);
//
//    @Query(value = "SELECT coalesce(AVG(s.actualScore), 0) FROM Score s WHERE s.target = :target")
//    double averageActualScoreForTarget(@Param("target") Target target);


    @Query(value = "SELECT coalesce(SUM(s.weightedScore), 0) FROM Score s WHERE s.output = :output")
    double totalWeightedScoreByOutput(@Param("output") Output output);

    @Query(value = "SELECT coalesce(AVG(s.actual), 0) FROM Score s WHERE s.output = :output")
    double averageActualByOutput(@Param("output") Output output);

    @Query(value = "SELECT coalesce(SUM(s.actual), 0) FROM Score s WHERE s.output = :output")
    double sumActualByOutput(@Param("output") Output output);

    @Query(value = "SELECT coalesce(SUM(s.agreedScore), 0) FROM Score s WHERE s.output = :output")
    double averageAgreedScoreByOutput(@Param("output") Output output);
    @Query(value = "SELECT coalesce(SUM(s.moderatedScore), 0) FROM Score s WHERE s.output = :output")
    double averageModeratedScoreByOutput(@Param("output") Output output);
    @Query(value = "SELECT coalesce(SUM(s.managerScore), 0) FROM Score s WHERE s.output = :output")
    double averageManagerScoreByOutput(@Param("output") Output output);
    @Query(value = "SELECT coalesce(SUM(s.employeeScore), 0) FROM Score s WHERE s.output = :output")
    double averageEmployeeScoreByOutput(@Param("output") Output output);

    @Query(value = "SELECT coalesce(SUM(s.employeeScore * s.output.allocatedWeight) * 0.01, 0) FROM Score s WHERE s.output.scorecard = :scorecard AND s.reportingDate = :reportingDate")
    double weightedEmployeeScore(@Param("scorecard") Scorecard scorecard, @Param("reportingDate") ReportingDate reportingDate);

    @Query(value = "SELECT coalesce(SUM(s.managerScore * s.output.allocatedWeight) * 0.01, 0) FROM Score s WHERE s.output.scorecard = :scorecard AND s.reportingDate = :reportingDate")
    double weightedManagerScore(@Param("scorecard") Scorecard scorecard, @Param("reportingDate") ReportingDate reportingDate);

    @Query(value = "SELECT coalesce(SUM(s.agreedScore * s.output.allocatedWeight) * 0.01, 0) FROM Score s WHERE s.output.scorecard = :scorecard AND s.reportingDate = :reportingDate")
    double weightedAgreedScore(@Param("scorecard") Scorecard scorecard, @Param("reportingDate") ReportingDate reportingDate);


}
