package hr.performancemanagement.utils.dto;

import hr.performancemanagement.entities.Account;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndividualTrendResponse {

    private List<String> monthNames;
    private List<Double> scores;
    private Account employee;
}
