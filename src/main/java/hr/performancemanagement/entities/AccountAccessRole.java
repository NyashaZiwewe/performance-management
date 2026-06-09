package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "account_access_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountAccessRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "client_id", nullable = false, updatable = false)
    private long clientId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(optional = false)
    @JoinColumn(name = "access_role_id")
    private AccessRole accessRole;

    @Column(nullable = false, length = 30)
    private String scopeType;

    @ManyToOne
    @JoinColumn(name = "division_id")
    private Division division;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    private LocalDate effectiveFrom;

    private LocalDate expiresOn;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 1000)
    private String reason;

    @ManyToOne
    @JoinColumn(name = "assigned_by_id")
    private Account assignedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private Date dateCreated;

    @UpdateTimestamp
    private Date dateUpdated;
}
