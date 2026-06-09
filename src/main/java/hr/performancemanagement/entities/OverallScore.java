package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uk_overall_score_scorecard_reporting_date",
                columnNames = {"scorecard_id", "reporting_date_id"}
        )
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OverallScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "scorecard_id", nullable = false)
    private Scorecard scorecard;
    @ManyToOne(optional = false)
    @JoinColumn(name = "reporting_date_id", nullable = false)
    private ReportingDate reportingDate;
    private Double employeeOverall;
    private Double managerOverall;
    private Double agreedOverall;
    private Double moderatedOverall;
    @CreationTimestamp
    private Date date;
}
