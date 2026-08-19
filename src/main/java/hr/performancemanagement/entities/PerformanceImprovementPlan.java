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
    @ManyToOne
    @JoinColumn(name = "scorecard_id")
    private Scorecard scorecard;
    @ManyToOne
    @JoinColumn(name = "reporting_date_id")
    private ReportingDate reportingDate;
    @ManyToOne
    @JoinColumn(name = "target_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Target target;

    private String targetArea;
    private String concern;
    private String expectedStandard;
    private String agreedAction;
    private String requiredSupport;
    private String reviewNotes;
    private String source;
    @Column(name = "not_applicable", nullable = false)
    private Boolean notApplicable = Boolean.FALSE;
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
    @OneToMany(mappedBy = "performanceImprovementPlan", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<PIPNote> noteList;

    public boolean isNotApplicable() {
        return Boolean.TRUE.equals(notApplicable);
    }

    public Boolean getNotApplicable() {
        return Boolean.TRUE.equals(notApplicable);
    }

    public void setNotApplicable(Boolean notApplicable) {
        this.notApplicable = Boolean.TRUE.equals(notApplicable);
    }

    @PrePersist
    @PreUpdate
    private void normalizeNullableFlags() {
        if (notApplicable == null) {
            notApplicable = Boolean.FALSE;
        }
    }

}
