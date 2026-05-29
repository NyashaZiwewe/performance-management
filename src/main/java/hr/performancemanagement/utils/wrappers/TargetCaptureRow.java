package hr.performancemanagement.utils.wrappers;

import hr.performancemanagement.entities.Target;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TargetCaptureRow {

    private Target target;
    private boolean showPerspective;
    private int perspectiveRowspan = 1;
    private boolean showStrategicObjective;
    private int strategicObjectiveRowspan = 1;
    private boolean showGoal;
    private int goalRowspan = 1;

    public TargetCaptureRow(Target target) {
        this.target = target;
    }
}
