package hr.performancemanagement.utils.wrappers;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ScorecardDisplaySection {
    private String sectionName;
    private Double sectionWeight;
    private List<ScorecardDisplayRow> rows = new ArrayList<ScorecardDisplayRow>();
}
