package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.PerformanceImprovementPlan;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.Scorecard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceImprovementPlanRepository extends JpaRepository<PerformanceImprovementPlan, Long> {
    List<PerformanceImprovementPlan> findPerformanceImprovementPlansByClientId(long clientId);
    List<PerformanceImprovementPlan> findPerformanceImprovementPlansByClientIdAndReportingPeriod(long clientId, ReportingPeriod reportingPeriod);
    List<PerformanceImprovementPlan> findPerformanceImprovementPlansByEmployeeAndReportingPeriod(Account account, ReportingPeriod reportingPeriod);
    List<PerformanceImprovementPlan> findPerformanceImprovementPlansByEmployee(Account account);
    List<PerformanceImprovementPlan> findPerformanceImprovementPlansByScorecardAndReportingDateAndSourceOrderByIdAsc(
            Scorecard scorecard,
            ReportingDate reportingDate,
            String source
    );
    boolean existsPerformanceImprovementPlanByScorecardAndReportingDateAndSource(
            Scorecard scorecard,
            ReportingDate reportingDate,
            String source
    );
    PerformanceImprovementPlan findPerformanceImprovementPlanById(long id);

}
