package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.entities.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportingDateRepository extends JpaRepository<ReportingDate, Long> {

    List<ReportingDate> findReportingDatesByReportingPeriod(ReportingPeriod period);
    ReportingDate findReportingDateById(long id);
    ReportingDate findReportingDateByStatusAndAndReportingPeriod_ClientId(String status, long clientId);
    @Query(value = "SELECT rd FROM ReportingDate rd WHERE rd.reportingPeriod = :reportingPeriod AND rd.id = (SELECT MAX(rd2.id) FROM ReportingDate rd2 WHERE rd2.reportingPeriod = :reportingPeriod)")
    ReportingDate findLastReportingDateByScorecard(@Param("reportingPeriod") ReportingPeriod reportingPeriod);
    @Query(value = "SELECT s FROM Scorecard s WHERE s.reportingPeriod = :reportingPeriod")
    List<Scorecard> findScorecardsByReportingPeriod(@Param("reportingPeriod") ReportingPeriod reportingPeriod);

}
