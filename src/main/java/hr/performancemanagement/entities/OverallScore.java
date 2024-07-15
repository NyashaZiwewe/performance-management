package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OverallScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "scorecard_id")
    private Scorecard scorecard;
    @ManyToOne
    @JoinColumn(name = "reporting_date_id")
    private ReportingDate reportingDate;
    private Double employeeOverall;
    private Double managerOverall;
    private Double agreedOverall;
    private Double moderatedOverall;
    @CreationTimestamp
    private Date date;
}
