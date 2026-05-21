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
public class ProbationWorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    private String name;
    private Integer stepOrder;
    private String approverMode;
    private String approverAccountType;
    private String approverRole;
    private Boolean sameDivisionOnly;
    private String status;
    @ManyToOne
    @JoinColumn(name = "approver_account_id")
    private Account approverAccount;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp date;
}
