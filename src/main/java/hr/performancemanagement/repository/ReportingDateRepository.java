package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.ReportingDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportingDateRepository extends JpaRepository<ReportingDate, Long> {

    List<ReportingDate> findReportingDatesByReportingPeriod(ReportingPeriod period);
    List<ReportingDate> findReportingDatesByReportingPeriodAndStatus(ReportingPeriod period, String status);
    ReportingDate findReportingDateById(long id);
    ReportingDate findTopByReportingPeriodOrderByEndDateDesc(ReportingPeriod reportingPeriod);
    List<ReportingDate> findReportingDatesByReportingPeriod_ClientIdAndStatusInOrderByDateDescIdDesc(long clientId, List<String> statuses);
    long countByReportingPeriod_ClientIdAndStatusIn(long clientId, List<String> statuses);
    long countByReportingPeriod_Id(long reportingPeriodId);
//    @Query(value = "SELECT ReportingDate FROM ReportingDate rd LEFT JOIN ReportingPeriod rp ON rd.reportingPeriod = rp WHERE rp.clientId=:clientId AND rd.status = :status")
//    ReportingDate findReportingDateByStatus(@Param("clientId") Long clientId, @Param("status") String status);
}
