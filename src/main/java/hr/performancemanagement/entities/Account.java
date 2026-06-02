package hr.performancemanagement.entities;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.validation.constraints.*;
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", referencedColumnName = "client_id")
    private Client client;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Transient
    private String initials;

    @Column(unique=true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @JsonIgnore
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Nullable
    @JsonIgnore
    private String resetPassword;

//    @ManyToMany(fetch = FetchType.EAGER, cascade=CascadeType.ALL)
//    @JoinTable(
//            name="ACCOUNTS_ROLES",
//            joinColumns={@JoinColumn(name="ACCOUNT_ID", referencedColumnName="ID")},
//            inverseJoinColumns={@JoinColumn(name="ROLE_ID", referencedColumnName="ID")})
//    private List<Role> roles = new ArrayList<>();

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "NONE|USER|ADMIN|SUPERVISOR|HR|MODERATOR", message = "Invalid role")
    private String role = "NONE";

    @ManyToOne
    @JoinColumn(name = "supervisor_id", referencedColumnName = "id")
    private Account supervisor;

    @ManyToOne
    @JoinColumn(name = "department_id", referencedColumnName = "id")
    @NotNull(message = "Department is required")
    private Department department;

    @NotBlank(message = "Position is required")
    @Size(max = 100, message = "Position cannot exceed 100 characters")
    private String position;

    @NotBlank(message = "Account type is required")
    @Pattern(
            regexp = "EMPLOYEE|SUPERVISOR|DEPARTMENT_MANAGER|DIVISIONAL_DIRECTOR|ACTING_CEO|CEO",
            message = "Invalid account type"
    )
    private String accountType;

    @ManyToOne
    @JoinColumn(name = "division_id", referencedColumnName = "id")
    private Division division;

    private String special;
    private String admin;
    private String accounts;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|IN_ACTIVE|INACTIVE|SUSPENDED|DELETED", message = "Invalid status")
    private String status;
    @CreationTimestamp()
    private Date date;

    public long getClientId() {
        if (client != null && client.getClientId() > 0) {
            return client.getClientId();
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

    public void setClient(Client client) {
        this.client = client;
    }

}
