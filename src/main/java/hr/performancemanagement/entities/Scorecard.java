package hr.performancemanagement.entities;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.*;
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
    @Positive(message = "Client ID must be positive")
    private long clientId;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @NotNull(message = "Owner is required")
    private Account owner;

    @ManyToOne
    @JoinColumn(name = "reporting_period_id")
    @NotNull(message = "Reporting period is required")
    private ReportingPeriod reportingPeriod;

    @ManyToOne
    @JoinColumn(name = "scorecard_model_id")
    @NotNull(message = "Scorecard model is required")
    private ScorecardModel scorecardModel;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|IN_ACTIVE|INACTIVE|ARCHIVED|DELETED", message = "Invalid status")
    private String status;

    @DecimalMin(value = "0.0", message = "Employee score must be non-negative")
    @DecimalMax(value = "5.0", message = "Employee score cannot exceed 5.0")
    private double employeeScore;

    @DecimalMin(value = "0.0", message = "Manager score must be non-negative")
    @DecimalMax(value = "5.0", message = "Manager score cannot exceed 5.0")
    private double managerScore;

    @DecimalMin(value = "0.0", message = "Agreed score must be non-negative")
    @DecimalMax(value = "5.0", message = "Agreed score cannot exceed 5.0")
    private double agreedScore;

    @DecimalMin(value = "0.0", message = "Moderated score must be non-negative")
    @DecimalMax(value = "5.0", message = "Moderated score cannot exceed 5.0")
    private double moderatedScore;

    @DecimalMin(value = "0.0", message = "Weighted score must be non-negative")
    @DecimalMax(value = "100.0", message = "Weighted score cannot exceed 100")
    private double weightedScore;

    @Column(columnDefinition = "varchar(50) default 'NEW'")
    @NotBlank(message = "Approval status is required")
    @Pattern(
            regexp = "NEW|PENDING_APPROVAL|APPROVED_BY_SUPERVISOR|REJECTED_BY_SUPERVISOR|APPROVED_BY_HR|REJECTED_BY_HR|SCORED_BY_EMPLOYEE|SCORED_BY_SUPERVISOR|AGREED_BY_TWO|MODERATED_BY_HR|CLOSED|PENDING|APPROVED|REJECTED|RETURNED",
            message = "Invalid approval status"
    )
    private String approvalStatus;

    @Size(max = 1000, message = "Owner comment cannot exceed 1000 characters")
    private String ownerComment;

    @Size(max = 1000, message = "Supervisor comment cannot exceed 1000 characters")
    private String supervisorComment;

    @Size(max = 1000, message = "Moderator comment cannot exceed 1000 characters")
    private String moderatorComment;
    @UpdateTimestamp
    private Date lastUpdate;

    @CreationTimestamp
    private Date date;

    @NotBlank(message = "Lock status is required")
    @Pattern(regexp = "OPEN|LOCKED|CLOSED", message = "Invalid lock status")
    private String lockStatus;

    @Transient
    private OverallScore overallScore;
}
