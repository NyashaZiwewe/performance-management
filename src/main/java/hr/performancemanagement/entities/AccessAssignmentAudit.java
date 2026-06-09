package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "access_assignment_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccessAssignmentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "client_id", nullable = false, updatable = false)
    private long clientId;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne
    @JoinColumn(name = "access_role_id")
    private AccessRole accessRole;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(length = 1000)
    private String reason;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private Account actor;

    @CreationTimestamp
    @Column(updatable = false)
    private Date dateCreated;
}
