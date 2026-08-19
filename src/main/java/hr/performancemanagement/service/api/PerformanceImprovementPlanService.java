package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ActionPlan;
import hr.performancemanagement.entities.PerformanceImprovementPlan;
import hr.performancemanagement.entities.ReportingDate;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.Scorecard;
import hr.performancemanagement.repository.PerformanceImprovementPlanRepository;
import java.util.ArrayList;
import java.util.List;

public interface PerformanceImprovementPlanService {
    List<PerformanceImprovementPlan> listAllPerformanceImprovementPlans();
    List<PerformanceImprovementPlan> listAllPerformanceImprovementPlansByClientId(long clientId);
    List<PerformanceImprovementPlan> listAllPerformanceImprovementPlans(ReportingPeriod period);
    List<PerformanceImprovementPlan> listAllPerformanceImprovementPlans(long clientId, ReportingPeriod period);
    List<PerformanceImprovementPlan> listPerformanceImprovementPlansByEmployee(Account employee, ReportingPeriod reportingPeriod);
    List<PerformanceImprovementPlan> listAllPerformanceImprovementPlansByEmployee(Account employee);
    List<PerformanceImprovementPlan> listSelfAssessmentPerformanceImprovementPlans(Scorecard scorecard, ReportingDate reportingDate);
    boolean hasSelfAssessmentPerformanceImprovementPlan(Scorecard scorecard, ReportingDate reportingDate);
    PerformanceImprovementPlan getPerformanceImprovementPlanById(long id);
    void addPerformanceImprovementPlan(PerformanceImprovementPlan plan);
    PerformanceImprovementPlan savePerformanceImprovementPlan(PerformanceImprovementPlan plan);
    void deletePerformanceImprovementPlan(PerformanceImprovementPlan performanceImprovementPlan);
}
