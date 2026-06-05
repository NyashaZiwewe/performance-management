package hr.performancemanagement.service.impl;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.Department;
import hr.performancemanagement.exception.ResourceNotFoundException;
import hr.performancemanagement.repository.DepartmentRepository;
import hr.performancemanagement.service.api.CommonService;
import hr.performancemanagement.service.api.DepartmentService;
import hr.performancemanagement.utils.constants.Client;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CommonService commonService;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository, CommonService commonService) {
        this.departmentRepository = departmentRepository;
        this.commonService = commonService;
    }

    @Override
    public List<Department> listAllDepartments() {
        Account loggedUser = commonService.getLoggedUser();
        long clientId = loggedUser != null ? loggedUser.getClientId() : Client.CLIENT_ID;

        if (!commonService.isAdmin() && !commonService.hasSpecialRights()) {
            if (loggedUser == null || loggedUser.getDepartment() == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(loggedUser.getDepartment());
        }

        List<Department> departments = new ArrayList<Department>();
        departments.addAll(departmentRepository.findDepartmentsByClientId(clientId));
        return departments;
    }

    @Override
    public List<Department> listAllDepartments(long clientId) {
        List<Department> departments = new ArrayList<Department>();
        departments.addAll(departmentRepository.findDepartmentsByClientId(clientId));
        return departments;
    }

    @Override
    public Department getDepartmentById(long id) {
        Department department = departmentRepository.findDepartmentById(id);
        if (department == null) {
            throw new ResourceNotFoundException("Department not found with id " + id);
        }
        return department;
    }

    @Override
    public Department saveDepartment(Department department) {
        if (department == null) {
            throw new IllegalArgumentException("Department cannot be null");
        }
        return departmentRepository.save(department);
    }

    @Override
    @Transactional
    public void deleteDepartment(Department department) {
        if (department == null) {
            throw new IllegalArgumentException("Department cannot be null");
        }
        departmentRepository.delete(department);
    }
}
