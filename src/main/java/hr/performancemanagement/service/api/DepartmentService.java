package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Department;
import java.util.List;

public interface DepartmentService {

    List<Department> listAllDepartments();

    List<Department> listAllDepartments(long clientId);

    Department getDepartmentById(long id);

    Department saveDepartment(Department department);

    void deleteDepartment(Department department);
}
