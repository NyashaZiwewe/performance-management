package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.ReportingDate;
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
//    @Query(value = "SELECT ReportingDate FROM ReportingDate rd LEFT JOIN ReportingPeriod rp ON rd.reportingPeriod = rp WHERE rp.clientId=:clientId AND rd.status = :status")
//    ReportingDate findReportingDateByStatus(@Param("clientId") Long clientId, @Param("status") String status);
}
