package hr.performancemanagement.entities;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.sql.Date;


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Scorecard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Account owner;
    @ManyToOne
    @JoinColumn(name = "reporting_period_id")
    private ReportingPeriod reportingPeriod;
    @ManyToOne
    @JoinColumn(name = "scorecard_model_id")
    private ScorecardModel scorecardModel;
    private String status;
    private double employeeScore;
    private double managerScore;
    private double agreedScore;
    private double moderatedScore;
    private double weightedScore;
    @Column(columnDefinition = "varchar(50) default 'NEW'")
    private String approvalStatus;
    private String ownerComment;
    private String supervisorComment;
    private String moderatorComment;
    @UpdateTimestamp
    private Date lastUpdate;
    @CreationTimestamp
    private Date date;
    private String lockStatus;


}
