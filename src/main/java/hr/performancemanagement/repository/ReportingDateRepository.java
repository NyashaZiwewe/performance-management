package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.ReportingDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportingDateRepository extends JpaRepository<ReportingDate, Long> {

    List<ReportingDate> findReportingDatesByReportingPeriod(ReportingPeriod period);
    ReportingDate findReportingDateById(long id);
}
