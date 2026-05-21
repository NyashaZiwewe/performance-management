package hr.performancemanagement.controllers.audit;

import hr.performancemanagement.entities.AuditLog;
import hr.performancemanagement.service.api.AuditLogService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.constants.Pages;
import hr.performancemanagement.utils.wrappers.AuditLogFilterWrapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping(value = "/audit-logs")
public class AuditLogController {
    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ModelAndView viewAuditLogs(@ModelAttribute("auditFilter") AuditLogFilterWrapper wrapper,
                                      HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView(Pages.AUDIT_REPORTS);
        applyDefaultDateRange(wrapper);
        List<AuditLog> auditLogs = new ArrayList<>();
        if (auditLogService.hasInvalidDateRange(wrapper.getStartDate(), wrapper.getEndDate())) {
            PortletUtils.addErrorMsg("Invalid date range: start date cannot be after end date.", request);
        } else {
            auditLogs = auditLogService.searchLogs(wrapper);
        }
        modelAndView.addObject("pageDomain", "Administration");
        modelAndView.addObject("pageName", "Audit Logs");
        modelAndView.addObject("pageTitle", "Transaction Audit Report");
        modelAndView.addObject("auditLogs", auditLogs);
        modelAndView.addObject("auditRows", buildAuditRows(auditLogs));
        PortletUtils.addMessagesToPage(modelAndView, request);
        return modelAndView;
    }

    @GetMapping("/download")
    public void downloadAuditLogs(@ModelAttribute AuditLogFilterWrapper wrapper,
                                  @RequestParam(value = "format", defaultValue = "csv") String format,
                                  HttpServletResponse response) throws IOException {
        applyDefaultDateRange(wrapper);
        if (auditLogService.hasInvalidDateRange(wrapper.getStartDate(), wrapper.getEndDate())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain");
            response.getWriter().write("Invalid date range: start date cannot be after end date.");
            return;
        }

        List<AuditLog> auditLogs = auditLogService.searchLogs(wrapper);
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=audit-report.csv");

        PrintWriter writer = response.getWriter();
        writer.println("Timestamp,User,Action,Entity,Record ID,Result");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
        for (AuditLog auditLog : auditLogs) {
            writer.println(csv(auditLog.getTimestamp() != null ? dateFormat.format(auditLog.getTimestamp()) : "") + ","
                    + csv(auditLog.getUserName()) + ","
                    + csv(auditLog.getAction()) + ","
                    + csv(auditLog.getTable()) + ","
                    + csv(auditLog.getRecordId()) + ","
                    + csv(auditLog.getResult()));
        }
        writer.flush();
    }

    private void applyDefaultDateRange(AuditLogFilterWrapper wrapper) {
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

    private List<AuditRow> buildAuditRows(List<AuditLog> auditLogs) {
        List<AuditRow> rows = new ArrayList<>();
        for (AuditLog auditLog : auditLogs) {
            rows.add(new AuditRow(auditLog, buildFriendlyResult(auditLog)));
        }
        return rows;
    }

    private String buildFriendlyResult(AuditLog auditLog) {
        String action = safeText(auditLog.getAction());
        String entity = safeText(auditLog.getTable());
        String recordId = safeText(auditLog.getRecordId());
        String result = safeText(auditLog.getResult());

        List<String> parts = new ArrayList<>();
        parts.add(actionToSentence(action, entity));

        String name = extractValue(result, "Name");
        if (!hasText(name)) {
            name = extractValue(result, "Full Name");
        }
        if (hasText(name)) {
            parts.add(name);
        }

        String description = extractValue(result, "Description");
        if (hasText(description)) {
            parts.add(description);
        }

        String status = extractValue(result, "Status");
        if (hasText(status) && !"SUCCESS".equalsIgnoreCase(status) && !"FAILED".equalsIgnoreCase(status)) {
            parts.add("Status " + status);
        }

        if (hasText(recordId)) {
            parts.add("ID " + recordId);
        }

        if (result.toUpperCase(Locale.ENGLISH).contains("FAILED")) {
            String error = extractAfter(result, "Error:");
            if (hasText(error)) {
                parts.add(error);
            }
        }

        return String.join(" | ", parts);
    }

    private String actionToSentence(String action, String entity) {
        String normalizedAction = action == null ? "" : action.trim();
        String normalizedEntity = hasText(entity) ? entity : "record";
        String lowerAction = normalizedAction.toLowerCase(Locale.ENGLISH);

        if (lowerAction.startsWith("save")) {
            return "Saved " + normalizedEntity;
        }
        if (lowerAction.startsWith("add")) {
            return "Added " + normalizedEntity;
        }
        if (lowerAction.startsWith("create")) {
            return "Created " + normalizedEntity;
        }
        if (lowerAction.startsWith("update")) {
            return "Updated " + normalizedEntity;
        }
        if (lowerAction.startsWith("delete")) {
            return "Deleted " + normalizedEntity;
        }
        return normalizedAction + " " + normalizedEntity;
    }

    private String extractValue(String source, String label) {
        if (!hasText(source) || !hasText(label)) {
            return null;
        }
        String marker = label + ": ";
        int start = source.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = source.indexOf(", ", start);
        int lineEnd = source.indexOf('\n', start);
        if (end < 0 || (lineEnd >= 0 && lineEnd < end)) {
            end = lineEnd;
        }
        if (end < 0) {
            end = source.length();
        }
        return source.substring(start, end).trim();
    }

    private String extractAfter(String source, String marker) {
        if (!hasText(source) || !hasText(marker)) {
            return null;
        }
        int start = source.indexOf(marker);
        if (start < 0) {
            return null;
        }
        return source.substring(start + marker.length()).trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ") + "\"";
    }

    public static class AuditRow {
        private final AuditLog auditLog;
        private final String friendlyResult;

        public AuditRow(AuditLog auditLog, String friendlyResult) {
            this.auditLog = auditLog;
            this.friendlyResult = friendlyResult;
        }

        public AuditLog getAuditLog() {
            return auditLog;
        }

        public String getFriendlyResult() {
            return friendlyResult;
        }
    }
}
