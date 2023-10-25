package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Score implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id")
    private Target target;
    @ManyToOne
    @JoinColumn(name = "reporting_date_id")
    private ReportingDate reportingDate;
    private double actual;
    private double employeeScore;
    private double managerScore;
    private double agreedScore;
    private double moderatedScore;
    private double weightedScore;
    private String evidence;
    private String justification;
    @CreationTimestamp
    private Date date;


}
