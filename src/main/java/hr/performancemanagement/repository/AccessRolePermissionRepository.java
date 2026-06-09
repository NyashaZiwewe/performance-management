package hr.performancemanagement.repository;

import hr.performancemanagement.entities.AccessRole;
import hr.performancemanagement.entities.AccessRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessRolePermissionRepository extends JpaRepository<AccessRolePermission, Long> {
    List<AccessRolePermission> findAccessRolePermissionsByAccessRoleOrderByPermission_ModuleAscPermission_NameAsc(AccessRole role);
    boolean existsAccessRolePermissionByAccessRoleAndPermission_Code(AccessRole role, String permissionCode);
    void deleteAccessRolePermissionsByAccessRole(AccessRole role);
}
