package hr.performancemanagement.repository;

import hr.performancemanagement.entities.AccessRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessRoleRepository extends JpaRepository<AccessRole, Long> {
    AccessRole findAccessRoleByIdAndClientId(long id, long clientId);
    AccessRole findAccessRoleByClientIdAndNameIgnoreCase(long clientId, String name);
    List<AccessRole> findAccessRolesByClientIdOrderByNameAsc(long clientId);
    List<AccessRole> findAccessRolesByClientIdAndStatusOrderByNameAsc(long clientId, String status);
}
