package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProbationAssessmentApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "assessment_id")
    private ProbationAssessment assessment;
    private String workflowStepName;
    private Integer stepOrder;
    private String action;
    @ManyToOne
    @JoinColumn(name = "actor_id")
    private Account actor;
    @Column(length = 3000)
    private String remarks;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp date;
}
