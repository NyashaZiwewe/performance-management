package hr.performancemanagement.service.resource;

import hr.performancemanagement.entities.Division;
import hr.performancemanagement.service.api.DivisionService;
import hr.performancemanagement.utils.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/divisions")
@RequiredArgsConstructor
public class DivisionResource {

    private final DivisionService divisionService;

    @GetMapping("/client/{clientId}")
    public ResponseEntity<CommonResponse<List<Division>>> listByClientId(@PathVariable long clientId) {
        List<Division> divisions = divisionService.listAllDivisions(clientId);
        return ResponseEntity.ok(CommonResponse.<List<Division>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Divisions retrieved successfully")
                .data(divisions)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<Division>> getById(@PathVariable long id) {
        Division division = divisionService.getDivisionById(id);
        return ResponseEntity.ok(CommonResponse.<Division>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Division retrieved successfully")
                .data(division)
                .build());
    }

    @PostMapping
    public ResponseEntity<CommonResponse<Division>> save(@RequestBody Division division) {
        Division savedDivision = divisionService.saveDivision(division);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Division>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Division saved successfully")
                .data(savedDivision)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<Division>> update(@PathVariable long id, @RequestBody Division division) {
        Division existingDivision = divisionService.getDivisionById(id);
        existingDivision.setName(division.getName());
        existingDivision.setCode(division.getCode());
        existingDivision.setAddress(division.getAddress());
        existingDivision.setEmail(division.getEmail());
        existingDivision.setPhone(division.getPhone());
        existingDivision.setWebsite(division.getWebsite());
        existingDivision.setColorCode(division.getColorCode());
        if (division.getClientId() > 0) {
            existingDivision.setClientId(division.getClientId());
        }
        Division updatedDivision = divisionService.saveDivision(existingDivision);
        return ResponseEntity.ok(CommonResponse.<Division>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Division updated successfully")
                .data(updatedDivision)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable long id) {
        Division division = divisionService.getDivisionById(id);
        divisionService.deleteDivision(division);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Division deleted successfully")
                .data(null)
                .build());
    }
}
