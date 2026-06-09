package hr.performancemanagement.repository;

import hr.performancemanagement.entities.AccessPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessPermissionRepository extends JpaRepository<AccessPermission, Long> {
    AccessPermission findAccessPermissionByCode(String code);
    List<AccessPermission> findAccessPermissionsByStatusOrderByModuleAscNameAsc(String status);
}
