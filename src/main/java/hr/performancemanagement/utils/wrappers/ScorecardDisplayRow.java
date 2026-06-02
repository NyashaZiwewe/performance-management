package hr.performancemanagement.utils.wrappers;

import hr.performancemanagement.entities.Target;
import hr.performancemanagement.entities.Score;
import hr.performancemanagement.entities.Evidence;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class ScorecardDisplayRow {
    private Target target;

    private String stage2Value;
    private String stage3Value;
    private String stage4Value;
    private String outputValue;

    private boolean showStage2 = true;
    private int stage2Rowspan = 1;

    private boolean showStage3 = true;
    private int stage3Rowspan = 1;

    private boolean showStage4 = true;
    private int stage4Rowspan = 1;

    private boolean showOutput = true;
    private int outputRowspan = 1;

    private Map<Long, Score> scoresByReportingDate = new HashMap<Long, Score>();
    private Map<Long, Evidence> evidenceByReportingDate = new HashMap<Long, Evidence>();

    public ScorecardDisplayRow(Target target) {
        this.target = target;
    }
}
