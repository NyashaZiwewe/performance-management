package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ActionPlan;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.repository.ActionPlanRepository;
import java.util.ArrayList;
import java.util.List;

public interface ActionPlanService {
    List<ActionPlan> listAllActionPlans(ReportingPeriod reportingPeriod);
    List<ActionPlan> listAllActionPlansByClientId(long clientId);
    List<ActionPlan> listAllActionPlansByReportingPeriod(long clientId, ReportingPeriod reportingPeriod);
    List<ActionPlan> listActionPlansByManagerAndReportingPeriod(Account manager, ReportingPeriod reportingPeriod);
    List<ActionPlan> listAllUserActionPlans(Account manager);
    ActionPlan getActionPlanById(long id);
    void addActionPlan(ActionPlan actionPlan);
    ActionPlan saveActionPlan(ActionPlan actionPlan);
    void deleteActionPlan(ActionPlan actionPlan);
}
