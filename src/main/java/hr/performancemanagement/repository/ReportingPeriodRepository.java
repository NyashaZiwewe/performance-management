package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ReportingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportingPeriodRepository extends JpaRepository<ReportingPeriod, Long> {

    List<ReportingPeriod> findAllReportingPeriodsByClientId(long clientId);
    ReportingPeriod findReportingPeriodById(long id);

    ReportingPeriod findReportingPeriodByStatus(String status);
}
