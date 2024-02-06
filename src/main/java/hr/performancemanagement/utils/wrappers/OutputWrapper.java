package hr.performancemanagement.utils.wrappers;
import hr.performancemanagement.entities.Gear;
import hr.performancemanagement.entities.Goal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OutputWrapper {

    private long outcomeId;
    private long scorecardId;
    private long gearId;
    private long goalId;
    private String name;
    private long outputId;
    private long targetId;
    private String measure;
    private String unit;
    private Double allocatedWeight;
    private Double normalTarget;
    private Double baseTarget;
    private Double stretchTarget;
}
