package hr.performancemanagement.repository;

import hr.performancemanagement.entities.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
//
//
    List<Department> findDepartmentsByClientId(long clientId);
    Department findDepartmentByManager(long manager);
    Department findDepartmentById(long id);
}
