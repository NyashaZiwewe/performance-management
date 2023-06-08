package hr.performancemanagement.utils.optionUtils;

import hr.performancemanagement.entities.Department;
import hr.performancemanagement.repository.DepartmentRepository;
import hr.performancemanagement.utils.messages.SelectOption;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class OptionUtils {

//    @Autowired
//    private static DepartmentRepository departmentRepository;
//
////    public OptionUtils(DepartmentRepository departmentRepository) {
////        this.departmentRepository = departmentRepository;
////    }
//
//    public static List<SelectOption> getAllDepartments(String placeholder) {
//        List<SelectOption> options = new ArrayList<>();
//        if (placeholder != null) {
//            options.add(new SelectOption(placeholder, placeholder));
//        }
//        List<Department> departments = departmentRepository.findDepartmentsByClientId(1);
//        List<String> departmentNames = new ArrayList<String>();
//
//        for (Department department : departments) {
//            departmentNames.add(department.getName());
//        }
//
//        for (String departmentName : departmentNames) {
//            options.add(new SelectOption(departmentName, departmentName));
//        }
//        return options;
//
//    }
}
