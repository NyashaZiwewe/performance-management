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

    @ManyToOne
    @JoinColumn(name = "approval_stage_id")
    private ScorecardWorkflowStage approvalStage;

    @Transient
    private String approvalStatus;

    @UpdateTimestamp
    private Date lastUpdate;

    @CreationTimestamp
    private Date date;

    private String lockStatus;

    public String getApprovalStatus() {
        if (hasText(approvalStatus)) {
            return approvalStatus;
        }
        if (approvalStage != null && hasText(approvalStage.getStatusCode())) {
            return approvalStage.getStatusCode();
        }
        return null;
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
