package hr.performancemanagement.entities;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    private String fullName;
    @Transient
    private String initials;
    @Column(unique=true)
    private String email;
    private String password;
    @Nullable
    private String resetPassword;

//    @ManyToMany(fetch = FetchType.EAGER, cascade=CascadeType.ALL)
//    @JoinTable(
//            name="ACCOUNTS_ROLES",
//            joinColumns={@JoinColumn(name="ACCOUNT_ID", referencedColumnName="ID")},
//            inverseJoinColumns={@JoinColumn(name="ROLE_ID", referencedColumnName="ID")})
//    private List<Role> roles = new ArrayList<>();

    private String role = "NONE";
    @ManyToOne
    @JoinColumn(name = "supervisor_id", referencedColumnName = "id")
    private Account supervisor;
    @ManyToOne
    @JoinColumn(name = "department_id", referencedColumnName = "id")
    private Department department;
    private String position;
    private String accountType;
    @ManyToOne
    @JoinColumn(name = "division_id", referencedColumnName = "id")
    private Division division;
    private String special;
    private String admin;
    private String accounts;
    private String status;
    @CreationTimestamp()
    private Date date;

}
