package hr.performancemanagement.service.resource;

import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.entities.StrategicObjective;
import hr.performancemanagement.exception.ResourceNotFoundException;
import hr.performancemanagement.service.api.ReportingPeriodService;
import hr.performancemanagement.service.api.StrategicObjectiveService;
import hr.performancemanagement.utils.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/strategic-objectives")
@RequiredArgsConstructor
public class StrategicObjectiveResource {

    private final StrategicObjectiveService strategicObjectiveService;
    private final ReportingPeriodService reportingPeriodService;

    @GetMapping("/reporting-period/{reportingPeriodId}")
    public ResponseEntity<CommonResponse<List<StrategicObjective>>> listByReportingPeriod(@PathVariable long reportingPeriodId) {
        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(reportingPeriodId);
        if (reportingPeriod == null) {
            throw new ResourceNotFoundException("Reporting period not found with id " + reportingPeriodId);
        }

        List<StrategicObjective> objectives = strategicObjectiveService.listAllStrategicObjectives(reportingPeriodId);
        return ResponseEntity.ok(CommonResponse.<List<StrategicObjective>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Strategic objectives retrieved successfully")
                .data(objectives)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<StrategicObjective>> getById(@PathVariable long id) {
        StrategicObjective strategicObjective = strategicObjectiveService.getStrategicObjectiveById(id);
        if (strategicObjective == null) {
            throw new ResourceNotFoundException("Strategic objective not found with id " + id);
        }

        return ResponseEntity.ok(CommonResponse.<StrategicObjective>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Strategic objective retrieved successfully")
                .data(strategicObjective)
                .build());
    }

    @PostMapping
    public ResponseEntity<CommonResponse<StrategicObjective>> save(@RequestBody StrategicObjective strategicObjective) {
        StrategicObjective savedObjective = strategicObjectiveService.saveStrategicObjective(strategicObjective);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<StrategicObjective>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Strategic objective saved successfully")
                .data(savedObjective)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable long id) {
        StrategicObjective strategicObjective = strategicObjectiveService.getStrategicObjectiveById(id);
        if (strategicObjective == null) {
            throw new ResourceNotFoundException("Strategic objective not found with id " + id);
        }
        strategicObjectiveService.deleteStrategicObjective(strategicObjective);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Strategic objective deleted successfully")
                .data(null)
                .build());
    }
}
