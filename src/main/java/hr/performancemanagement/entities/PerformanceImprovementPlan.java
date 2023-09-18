package hr.performancemanagement.entities;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Date;
import java.util.List;
//import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PerformanceImprovementPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Account employee;
    @ManyToOne
    @JoinColumn(name = "reporting_period_id")
    private ReportingPeriod reportingPeriod;

    private String targetArea;
    private String concern;
    private String expectedStandard;
    private String agreedAction;
    private String requiredSupport;
    private String reviewNotes;
    private double progress;
    private String status;
    private String endDate;
    @CreationTimestamp
    @Column(updatable = false)
    private Date date;
    @OneToMany(mappedBy = "performanceImprovementPlan", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<PIPTask> taskList;
    @OneToMany(mappedBy = "performanceImprovementPlan", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<PIPIssue> issueList;

}
