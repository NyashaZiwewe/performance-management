package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "access_role_permission", uniqueConstraints = {
        @UniqueConstraint(name = "uk_access_role_permission", columnNames = {"access_role_id", "permission_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccessRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "access_role_id")
    private AccessRole accessRole;

    @ManyToOne(optional = false)
    @JoinColumn(name = "permission_id")
    private AccessPermission permission;
}
