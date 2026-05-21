package hr.performancemanagement.service.resource;

import hr.performancemanagement.entities.Department;
import hr.performancemanagement.service.api.DepartmentService;
import hr.performancemanagement.utils.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentResource {

    private final DepartmentService departmentService;

    @GetMapping("/client/{clientId}")
    public ResponseEntity<CommonResponse<List<Department>>> listByClientId(@PathVariable long clientId) {
        List<Department> departments = departmentService.listAllDepartments(clientId);
        return ResponseEntity.ok(CommonResponse.<List<Department>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Departments retrieved successfully")
                .data(departments)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<Department>> getById(@PathVariable long id) {
        Department department = departmentService.getDepartmentById(id);
        return ResponseEntity.ok(CommonResponse.<Department>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Department retrieved successfully")
                .data(department)
                .build());
    }

    @PostMapping
    public ResponseEntity<CommonResponse<Department>> save(@RequestBody Department department) {
        Department savedDepartment = departmentService.saveDepartment(department);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Department>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Department saved successfully")
                .data(savedDepartment)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<Department>> update(@PathVariable long id, @RequestBody Department department) {
        Department existingDepartment = departmentService.getDepartmentById(id);
        existingDepartment.setName(department.getName());
        existingDepartment.setManager(department.getManager());
        if (department.getClientId() > 0) {
            existingDepartment.setClientId(department.getClientId());
        }
        Department updatedDepartment = departmentService.saveDepartment(existingDepartment);
        return ResponseEntity.ok(CommonResponse.<Department>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Department updated successfully")
                .data(updatedDepartment)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable long id) {
        Department department = departmentService.getDepartmentById(id);
        departmentService.deleteDepartment(department);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Department deleted successfully")
                .data(null)
                .build());
    }
}
