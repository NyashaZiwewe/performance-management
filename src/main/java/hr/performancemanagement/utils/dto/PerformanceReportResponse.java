package hr.performancemanagement.utils.dto;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.ActionPlan;
import hr.performancemanagement.entities.Goal;
import hr.performancemanagement.entities.PerformanceImprovementPlan;
import hr.performancemanagement.entities.Scorecard;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReportResponse {

    private Scorecard scorecard;
    private List<Goal> goals;
    private List<PerformanceImprovementPlan> performanceImprovementPlans;
    private List<ActionPlan> actionPlans;
    private Account owner;
    private String startDate;
    private String endDate;
    private double averageModeratedScore;
    private double weightedScore;
}
