package hr.performancemanagement.utils.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityPeriodProgress {
    private String stageKey;
    private String stageLabel;
    private String startDate;
    private String cutoffDate;
    private int totalScorecards;
    private int completedScorecards;
    private int outstandingScorecards;
    private String message;
    private boolean configured;
    private boolean active;
}
