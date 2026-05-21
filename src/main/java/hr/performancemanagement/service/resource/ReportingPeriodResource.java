package hr.performancemanagement.service.resource;

import hr.performancemanagement.entities.ReportingPeriod;
import hr.performancemanagement.exception.ResourceNotFoundException;
import hr.performancemanagement.service.api.ReportingPeriodService;
import hr.performancemanagement.utils.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reporting-periods")
@RequiredArgsConstructor
public class ReportingPeriodResource {

    private final ReportingPeriodService reportingPeriodService;

    @GetMapping("/client/{clientId}")
    public ResponseEntity<CommonResponse<List<ReportingPeriod>>> listByClientId(@PathVariable long clientId) {
        List<ReportingPeriod> reportingPeriods = reportingPeriodService.listAllReportingPeriods(clientId);
        return ResponseEntity.ok(CommonResponse.<List<ReportingPeriod>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Reporting periods retrieved successfully")
                .data(reportingPeriods)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<ReportingPeriod>> getById(@PathVariable long id) {
        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(id);
        if (reportingPeriod == null) {
            throw new ResourceNotFoundException("Reporting period not found with id " + id);
        }
        return ResponseEntity.ok(CommonResponse.<ReportingPeriod>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Reporting period retrieved successfully")
                .data(reportingPeriod)
                .build());
    }

    @PostMapping
    public ResponseEntity<CommonResponse<ReportingPeriod>> save(@RequestBody ReportingPeriod reportingPeriod) {
        reportingPeriodService.saveReportingPeriod(reportingPeriod);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<ReportingPeriod>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Reporting period saved successfully")
                .data(reportingPeriod)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable long id) {
        ReportingPeriod reportingPeriod = reportingPeriodService.getReportingPeriodById(id);
        if (reportingPeriod == null) {
            throw new ResourceNotFoundException("Reporting period not found with id " + id);
        }
        reportingPeriodService.deleteReportingPeriod(reportingPeriod);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Reporting period deleted successfully")
                .data(null)
                .build());
    }
}
