package hr.performancemanagement.entities;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.sql.Date;
import java.util.Locale;


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Scorecard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", referencedColumnName = "client_id")
    private Client client;

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

    @ManyToOne
    @JoinColumn(name = "approval_stage_id")
    private ScorecardWorkflowStage approvalStage;

    @Transient
    private String approvalStatus;

    @Column(name = "approval_status", insertable = false, updatable = false)
    @JsonIgnore
    private String legacyApprovalStatus;

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

    public String getApprovalStatus() {
        if (approvalStage != null && hasText(approvalStage.getStatusCode())) {
            return approvalStage.getStatusCode();
        }
        if (hasText(approvalStatus)) {
            return approvalStatus;
        }
        return legacyApprovalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = normalizeApprovalStatus(approvalStatus);
    }

    public void setApprovalStage(ScorecardWorkflowStage approvalStage) {
        this.approvalStage = approvalStage;
        if (approvalStage != null && hasText(approvalStage.getStatusCode())) {
            this.approvalStatus = normalizeApprovalStatus(approvalStage.getStatusCode());
        }
    }

    @PostLoad
    private void syncApprovalStatus() {
        if (approvalStage != null && hasText(approvalStage.getStatusCode())) {
            approvalStatus = normalizeApprovalStatus(approvalStage.getStatusCode());
            return;
        }
        if (!hasText(approvalStatus) && hasText(legacyApprovalStatus)) {
            approvalStatus = normalizeApprovalStatus(legacyApprovalStatus);
        }
    }

    private String normalizeApprovalStatus(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ENGLISH);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public long getClientId() {
        if (client != null && client.getClientId() > 0) {
            return client.getClientId();
        }
        if (owner != null && owner.getClientId() > 0) {
            return owner.getClientId();
        }
        return 0L;
    }

    public void setClientId(long clientId) {
        if (clientId <= 0) {
            this.client = null;
            return;
        }
        if (this.client != null && this.client.getClientId() == clientId) {
            return;
        }
        Client clientRef = new Client();
        clientRef.setClientId(clientId);
        this.client = clientRef;
    }
}
