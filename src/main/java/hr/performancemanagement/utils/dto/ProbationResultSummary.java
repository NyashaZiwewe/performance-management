package hr.performancemanagement.utils.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ProbationResultSummary {

    private final int totalKpis;
    private final int incumbentMarksCaptured;
    private final int supervisorMarksCaptured;
    private final int progressCaptured;
    private final int flaggedKpis;
    private final double averageIncumbentMark;
    private final double averageSupervisorMark;
    private final double averageProgress;
    private final double resultMark;
    private final String resultSource;
    private final boolean complete;
    private final String performanceBand;
    private final String recommendation;
}
