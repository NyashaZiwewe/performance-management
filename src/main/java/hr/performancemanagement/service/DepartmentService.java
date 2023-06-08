package hr.performancemanagement.service;

import hr.performancemanagement.entities.Department;
import hr.performancemanagement.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
@Service
public class DepartmentService {

    @Autowired
    DepartmentRepository departmentRepository;
    @Autowired
    HttpSession session;

    public List<Department> listAllDepartments()
    {
        Long clientId = (Long) session.getAttribute("clientId");
        List<Department> departmentList = new ArrayList<>();
        String admin = (String) session.getAttribute("admin");
        String special = (String) session.getAttribute("special");

        if(!admin.equalsIgnoreCase("IS_ADMIN") || !special.equalsIgnoreCase("HAS_SPECIAL_RIGHTS")){

            Department myDepartment = (Department) session.getAttribute("department");
            departmentList.add(myDepartment);
        }else{
            departmentRepository.findDepartmentsByClientId(clientId).forEach(department -> departmentList.add(department));
        }

        return departmentList;
    }

    public void addDepartment(Department department) {
        departmentRepository.save(department);
    }
}
