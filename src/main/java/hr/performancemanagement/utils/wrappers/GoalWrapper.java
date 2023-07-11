package hr.performancemanagement.utils.wrappers;
import hr.performancemanagement.entities.Perspective;
import hr.performancemanagement.entities.StrategicObjective;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GoalWrapper {

    private long goalId;
    private long scorecardId;
    private Perspective perspective;
    private StrategicObjective strategicObjective;
    private String goalName;

    private long targetId;
    private String measure;
    private String unit;
    private Double allocatedWeight;
    private Double normalTarget;
    private Double baseTarget;
    private Double stretchTarget;
}
