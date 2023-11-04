package hr.performancemanagement.service;

import hr.performancemanagement.entities.Department;
import hr.performancemanagement.repository.DepartmentRepository;
import hr.performancemanagement.utils.constants.PMConstants;
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
    CommonService cs;
    @Autowired
    HttpSession session;

    public List<Department> listAllDepartments()
    {
        Long clientId = cs.getLoggedUser().getClientId();
        List<Department> departmentList = new ArrayList<>();

        if(!cs.isAdmin() && !cs.hasSpecialRights()){

            Department myDepartment = cs.getLoggedUser().getDepartment();
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
