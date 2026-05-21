package hr.performancemanagement.utils.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceLevelsResponse {

    private List<String> names;
    private List<Double> scores;
}
