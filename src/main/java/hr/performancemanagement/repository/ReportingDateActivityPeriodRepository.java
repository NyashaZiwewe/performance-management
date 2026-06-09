package hr.performancemanagement.repository;

import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingDateActivityPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportingDateActivityPeriodRepository extends JpaRepository<ReportingDateActivityPeriod, Long> {

    ReportingDateActivityPeriod findReportingDateActivityPeriodById(long id);

    List<ReportingDateActivityPeriod> findReportingDateActivityPeriodsByReportingDateOrderByStartDateAscEndDateAsc(
            ReportingDate reportingDate
    );

    boolean existsReportingDateActivityPeriodByReportingDate(ReportingDate reportingDate);

    void deleteReportingDateActivityPeriodsByReportingDate(ReportingDate reportingDate);
}
