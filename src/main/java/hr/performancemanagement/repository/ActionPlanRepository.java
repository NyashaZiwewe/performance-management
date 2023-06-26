package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ActionPlan;
import hr.performancemanagement.entities.Department;
import hr.performancemanagement.entities.ReportingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionPlanRepository extends JpaRepository<ActionPlan, Long> {
    List<ActionPlan> findActionPlansByClientId(long clientId);
    List<ActionPlan> findActionPlansByManager(Account manager);
    List<ActionPlan> findActionPlanByStatus(String status);
    List<ActionPlan> findActionPlansByReportingPeriodAndClientId(ReportingPeriod reportingPeriod, long clientId);
    List<ActionPlan> findActionPlansByReportingPeriodAndManager(ReportingPeriod reportingPeriod, Account account);
    List<ActionPlan> findActionPlansByReportingPeriodAndManager_Supervisor(ReportingPeriod reportingPeriod, Account account);
    List<ActionPlan> findActionPlansByReportingPeriodAndManager_Department(ReportingPeriod reportingPeriod, Department department);
    ActionPlan findActionPlanById(long id);
}
