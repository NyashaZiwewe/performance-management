package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScorecardWorkflowStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(updatable = false)
    private long clientId;

    private String name;

    private Integer stageOrder;

    private String roleKey;

    private String statusCode;

    @Column(length = 1000)
    private String statusCodes;

    private String statusLabel;

    private String actionButtonLabel;

    private String rejectionButtonLabel;

    private String status;

    @CreationTimestamp
    @Column(updatable = false)
    private Date dateCreated;

    @UpdateTimestamp
    private Date dateUpdated;
}
