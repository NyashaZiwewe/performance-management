package hr.performancemanagement.utils.wrappers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProbationDimensionResponseWrapper {

    private long dimensionTemplateId;
    private String strengths;
    private String areasForImprovement;
}
