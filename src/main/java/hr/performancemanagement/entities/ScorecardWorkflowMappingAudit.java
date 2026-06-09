package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "scorecard_workflow_mapping_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScorecardWorkflowMappingAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "client_id", nullable = false, updatable = false)
    private long clientId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "scorecard_id", nullable = false)
    private Scorecard scorecard;

    @ManyToOne
    @JoinColumn(name = "previous_stage_id")
    private ScorecardWorkflowStage previousStage;

    @ManyToOne(optional = false)
    @JoinColumn(name = "new_stage_id", nullable = false)
    private ScorecardWorkflowStage newStage;

    @Column(nullable = false, length = 1000)
    private String reason;

    private boolean initializeReportingDateStages;

    private int reportingDateStagesInitialized;

    @ManyToOne(optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private Account actor;

    @CreationTimestamp
    @Column(updatable = false)
    private Date dateCreated;
}
