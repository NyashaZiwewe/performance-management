package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "scorecard_reporting_date_stage",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_scorecard_reporting_date_stage",
                columnNames = {"scorecard_id", "reporting_date_id"}
        )
)
public class ScorecardReportingDateStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(updatable = false)
    @Positive(message = "Client ID must be positive")
    private long clientId;

    @ManyToOne
    @JoinColumn(name = "scorecard_id")
    private Scorecard scorecard;

    @ManyToOne
    @JoinColumn(name = "reporting_date_id")
    private ReportingDate reportingDate;

    @ManyToOne
    @JoinColumn(name = "approval_stage_id")
    private ScorecardWorkflowStage approvalStage;

    @Pattern(regexp = "ACTIVE|IN_ACTIVE|INACTIVE|ARCHIVED|DELETED", message = "Invalid status")
    private String status;

    @CreationTimestamp
    @Column(updatable = false)
    private Date dateCreated;

    @UpdateTimestamp
    private Date dateUpdated;
}

