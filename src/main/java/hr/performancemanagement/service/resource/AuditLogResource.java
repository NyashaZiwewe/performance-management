package hr.performancemanagement.service.resource;

import hr.performancemanagement.entities.AuditLog;
import hr.performancemanagement.exception.BadRequestException;
import hr.performancemanagement.service.api.AuditLogService;
import hr.performancemanagement.utils.dto.CommonResponse;
import hr.performancemanagement.utils.wrappers.AuditLogFilterWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Calendar;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogResource {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<CommonResponse<List<AuditLog>>> searchLogs(@ModelAttribute AuditLogFilterWrapper wrapper) {
        applyDefaultDateRange(wrapper);
        validateDateRange(wrapper);

        List<AuditLog> auditLogs = auditLogService.searchLogs(wrapper);
        return ResponseEntity.ok(CommonResponse.<List<AuditLog>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Audit logs retrieved successfully")
                .data(auditLogs)
                .build());
    }

    @PostMapping("/search")
    public ResponseEntity<CommonResponse<List<AuditLog>>> searchLogsByBody(@RequestBody AuditLogFilterWrapper wrapper) {
        applyDefaultDateRange(wrapper);
        validateDateRange(wrapper);

        List<AuditLog> auditLogs = auditLogService.searchLogs(wrapper);
        return ResponseEntity.ok(CommonResponse.<List<AuditLog>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Audit logs retrieved successfully")
                .data(auditLogs)
                .build());
    }

    private void validateDateRange(AuditLogFilterWrapper wrapper) {
        if (auditLogService.hasInvalidDateRange(wrapper.getStartDate(), wrapper.getEndDate())) {
            throw new BadRequestException("Invalid date range: start date cannot be after end date.");
        }
    }

    private void applyDefaultDateRange(AuditLogFilterWrapper wrapper) {
        if (wrapper == null) {
            return;
        }
        if (wrapper.getStartDate() != null || wrapper.getEndDate() != null) {
            return;
        }

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startCalendar.set(Calendar.MINUTE, 0);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.set(Calendar.HOUR_OF_DAY, 23);
        endCalendar.set(Calendar.MINUTE, 59);
        endCalendar.set(Calendar.SECOND, 59);
        endCalendar.set(Calendar.MILLISECOND, 999);

        wrapper.setStartDate(startCalendar.getTime());
        wrapper.setEndDate(endCalendar.getTime());
    }
}
