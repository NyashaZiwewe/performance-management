package hr.performancemanagement.repository;
import hr.performancemanagement.entities.Output;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Score;
import hr.performancemanagement.entities.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TargetRepository extends JpaRepository<Target, Long> {
    List<Target> findTargetsByOutput(Output output);
    Target findTargetById(long id);
    int countTargetsByOutput(Output output);
    @Query("SELECT coalesce(s.actual, 0) FROM Score s WHERE s.target = :target AND s.reportingDate = :reportingDate")
    Double currentActual(@Param("target") Target target, @Param("reportingDate") ReportingDate reportingDate);
    @Query("SELECT coalesce(s.employeeScore, 0) FROM Score s WHERE s.target = :target AND s.reportingDate = :reportingDate")
    Double currentEmployeeScore(@Param("target") Target target, @Param("reportingDate") ReportingDate reportingDate);
    @Query("SELECT coalesce(s.managerScore, 0) FROM Score s WHERE s.target = :target AND s.reportingDate = :reportingDate")
    Double currentManagerScore(@Param("target") Target target, @Param("reportingDate") ReportingDate reportingDate);
    @Query("SELECT coalesce(s.agreedScore, 0) FROM Score s WHERE s.target = :target AND s.reportingDate = :reportingDate")
    Double currentAgreedScore(@Param("target") Target target, @Param("reportingDate") ReportingDate reportingDate);
    @Query("SELECT coalesce(s.moderatedScore, 0) FROM Score s WHERE s.target = :target AND s.reportingDate = :reportingDate")
    Double currentModeratedScore(@Param("target") Target target, @Param("reportingDate") ReportingDate reportingDate);
    @Query("SELECT coalesce(s.weightedScore, 0) FROM Score s WHERE s.target = :target AND s.reportingDate = :reportingDate")
    Double currentWeightedScore(@Param("target") Target target, @Param("reportingDate") ReportingDate reportingDate);
    @Query("SELECT s.evidence FROM Score s WHERE s.target = :target AND s.reportingDate = :reportingDate")
    String currentEvidence(@Param("target") Target target, @Param("reportingDate") ReportingDate reportingDate);
    @Query("SELECT s.justification FROM Score s WHERE s.target = :target AND s.reportingDate = :reportingDate")
    String currentJustification(@Param("target") Target target, @Param("reportingDate") ReportingDate reportingDate);

}
