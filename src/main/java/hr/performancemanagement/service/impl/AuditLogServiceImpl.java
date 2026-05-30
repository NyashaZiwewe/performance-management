package hr.performancemanagement.service.impl;

import org.springframework.stereotype.Service;
import hr.performancemanagement.service.api.*;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.AuditLog;
import hr.performancemanagement.repository.AuditLogRepository;
import hr.performancemanagement.utils.wrappers.AuditLogFilterWrapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@Service
public class AuditLogServiceImpl implements hr.performancemanagement.service.api.AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void logSuccess(String action, String tableName, Object[] args, Object result) {
        AuditLog log = new AuditLog();
        log.setTimestamp(new Date());
        log.setUserName(resolveUserName());
        log.setAction(action);
        log.setTable(resolveTableName(tableName, action, result, args));
        log.setRecordId(resolveRecordId(result, args));
        log.setResult(buildPayload("SUCCESS", args, result, null));
        auditLogRepository.save(log);
    }

    @Override
    public void logFailure(String action, String tableName, Object[] args, Throwable throwable) {
        AuditLog log = new AuditLog();
        log.setTimestamp(new Date());
        log.setUserName(resolveUserName());
        log.setAction(action + " FAILED");
        log.setTable(resolveTableName(tableName, action, null, args));
        log.setRecordId(resolveRecordId(null, args));
        log.setResult(buildPayload("FAILED", args, null, throwable));
        auditLogRepository.save(log);
    }

    @Override
    public List<AuditLog> searchLogs(AuditLogFilterWrapper wrapper) {
        if (wrapper != null && hasInvalidDateRange(wrapper.getStartDate(), wrapper.getEndDate())) {
            return Collections.emptyList();
        }
        return auditLogRepository.findAll(buildSpecification(wrapper));
    }

    @Override
    public boolean hasInvalidDateRange(Date startDate, Date endDate) {
        return startDate != null && endDate != null && startDate.after(endDate);
    }

    private Specification<AuditLog> buildSpecification(AuditLogFilterWrapper wrapper) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (wrapper != null) {
                if (StringUtils.hasText(wrapper.getUserName())) {
                    predicates.add(cb.like(cb.lower(root.get("userName")),
                            "%" + wrapper.getUserName().trim().toLowerCase(Locale.ENGLISH) + "%"));
                }
                if (StringUtils.hasText(wrapper.getAction())) {
                    predicates.add(cb.like(cb.lower(root.get("action")),
                            "%" + wrapper.getAction().trim().toLowerCase(Locale.ENGLISH) + "%"));
                }
                if (StringUtils.hasText(wrapper.getTableName())) {
                    predicates.add(cb.like(cb.lower(root.get("table")),
                            "%" + wrapper.getTableName().trim().toLowerCase(Locale.ENGLISH) + "%"));
                }
                if (StringUtils.hasText(wrapper.getRecordId())) {
                    predicates.add(cb.like(cb.lower(root.get("recordId")),
                            "%" + wrapper.getRecordId().trim().toLowerCase(Locale.ENGLISH) + "%"));
                }
                if (StringUtils.hasText(wrapper.getKeyword())) {
                    String keyword = "%" + wrapper.getKeyword().trim().toLowerCase(Locale.ENGLISH) + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("result")), keyword),
                            cb.like(cb.lower(root.get("action")), keyword),
                            cb.like(cb.lower(root.get("table")), keyword),
                            cb.like(cb.lower(root.get("userName")), keyword)
                    ));
                }
                if (wrapper.getStartDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), wrapper.getStartDate()));
                }
                if (wrapper.getEndDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), wrapper.getEndDate()));
                }
            }

            query.orderBy(cb.desc(root.get("timestamp")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String resolveUserName() {
        Account loggedUser = resolveLoggedUserFromRequest();
        return loggedUser != null ? loggedUser.getFullName() : "System";
    }

    private Account resolveLoggedUserFromRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object candidate = session.getAttribute("loggedUser");
        if (candidate instanceof Account) {
            return (Account) candidate;
        }
        return null;
    }

    private String resolveRecordId(Object result, Object[] args) {
        String recordId = tryExtractId(result);
        if (recordId != null) {
            return recordId;
        }
        if (args != null) {
            for (Object arg : args) {
                recordId = tryExtractId(arg);
                if (recordId != null) {
                    return recordId;
                }
            }
        }
        return null;
    }

    private String tryExtractId(Object candidate) {
        if (candidate == null) {
            return null;
        }
        if (candidate instanceof Number || candidate instanceof CharSequence) {
            return String.valueOf(candidate);
        }
        try {
            Method getIdMethod = candidate.getClass().getMethod("getId");
            Object idValue = getIdMethod.invoke(candidate);
            return idValue != null ? String.valueOf(idValue) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveTableName(String tableName, String action, Object result, Object[] args) {
        if (StringUtils.hasText(tableName) && !"Common".equalsIgnoreCase(tableName.trim())) {
            return tableName;
        }
        String resolved = tryExtractTypeName(result);
        if (resolved != null) {
            return resolved;
        }
        if (args != null) {
            for (Object arg : args) {
                resolved = tryExtractTypeName(arg);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        if (StringUtils.hasText(action)) {
            if (action.startsWith("save") || action.startsWith("add")) {
                return action.replaceFirst("^(save|add)", "");
            }
            if (action.startsWith("update")) {
                return action.replaceFirst("^update", "");
            }
            if (action.startsWith("delete")) {
                return action.replaceFirst("^delete", "");
            }
            if (action.startsWith("create")) {
                return action.replaceFirst("^create", "");
            }
        }
        return "Transaction";
    }

    private String tryExtractTypeName(Object candidate) {
        if (candidate == null) {
            return null;
        }
        if (candidate instanceof Number || candidate instanceof CharSequence || candidate instanceof Date) {
            return null;
        }
        if (candidate.getClass().isArray()) {
            int length = Array.getLength(candidate);
            for (int i = 0; i < length; i++) {
                String typeName = tryExtractTypeName(Array.get(candidate, i));
                if (typeName != null) {
                    return typeName;
                }
            }
            return null;
        }
        if (candidate instanceof Collection<?>) {
            for (Object item : (Collection<?>) candidate) {
                String typeName = tryExtractTypeName(item);
                if (typeName != null) {
                    return typeName;
                }
            }
            return null;
        }
        return candidate.getClass().getSimpleName();
    }

    private String buildPayload(String status, Object[] args, Object result, Throwable throwable) {
        StringBuilder payload = new StringBuilder();
        payload.append("Status: ").append(status);
        String formattedArgs = formatValue(args);
        if (StringUtils.hasText(formattedArgs)) {
            payload.append("\nInput: ").append(formattedArgs);
        }
        if (result != null) {
            payload.append("\nOutput: ").append(formatValue(result));
        }
        if (throwable != null) {
            payload.append("\nError: ").append(throwable.getClass().getSimpleName())
                    .append(": ").append(throwable.getMessage());
        }
        return payload.toString();
    }

    private String formatValue(Object value) {
        Object simplified = simplifyValue(value);
        if (simplified == null) {
            return "";
        }
        if (simplified instanceof Collection<?>) {
            List<String> items = new ArrayList<>();
            for (Object item : (Collection<?>) simplified) {
                items.add(formatValue(item));
            }
            return String.join("; ", items);
        }
        if (simplified instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) simplified;
            List<String> parts = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getValue() != null && StringUtils.hasText(String.valueOf(entry.getValue()))) {
                    parts.add(readableLabel(String.valueOf(entry.getKey())) + ": " + entry.getValue());
                }
            }
            return String.join(", ", parts);
        }
        return String.valueOf(simplified);
    }

    private Object simplifyValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean || value instanceof Date) {
            return value;
        }
        if (value instanceof Map<?, ?>) {
            Map<String, Object> summary = new LinkedHashMap<>();
            int count = 0;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (count++ >= 10) {
                    break;
                }
                String key = String.valueOf(entry.getKey());
                Object nestedValue = simplifyValue(entry.getValue());
                if (nestedValue != null) {
                    summary.put(key, nestedValue);
                }
            }
            return summary;
        }
        if (value.getClass().isArray()) {
            List<Object> items = new ArrayList<>();
            int length = Array.getLength(value);
            for (int i = 0; i < Math.min(length, 10); i++) {
                items.add(simplifyValue(Array.get(value, i)));
            }
            return items;
        }
        if (value instanceof Collection<?>) {
            List<Object> items = new ArrayList<>();
            int count = 0;
            for (Object item : (Collection<?>) value) {
                if (count++ >= 10) {
                    break;
                }
                items.add(simplifyValue(item));
            }
            return items;
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("type", value.getClass().getSimpleName());

        String id = tryExtractId(value);
        if (id != null) {
            summary.put("id", id);
        }

        addPreferredField(summary, value, "name");
        addPreferredField(summary, value, "fullName");
        addPreferredField(summary, value, "email");
        addPreferredField(summary, value, "description");
        addPreferredField(summary, value, "status");

        for (Field field : value.getClass().getDeclaredFields()) {
            if (summary.size() >= 8) {
                break;
            }
            appendFieldSummary(summary, value, field);
        }
        return summary;
    }

    private void addPreferredField(Map<String, Object> summary, Object value, String fieldName) {
        if (summary.containsKey(fieldName)) {
            return;
        }
        try {
            Field field = value.getClass().getDeclaredField(fieldName);
            appendFieldSummary(summary, value, field);
        } catch (NoSuchFieldException ignored) {
            // Optional field.
        }
    }

    private void appendFieldSummary(Map<String, Object> summary, Object value, Field field) {
        try {
            field.setAccessible(true);
            Object fieldValue = field.get(value);
            if (fieldValue == null || fieldValue.getClass().getName().startsWith("java.lang.reflect")) {
                return;
            }
            if (fieldValue instanceof CharSequence || fieldValue instanceof Number || fieldValue instanceof Boolean || fieldValue instanceof Date) {
                summary.put(field.getName(), fieldValue);
                return;
            }
            String nestedId = tryExtractId(fieldValue);
            if (nestedId != null) {
                summary.put(field.getName(), fieldValue.getClass().getSimpleName() + "#" + nestedId);
            }
        } catch (RuntimeException | IllegalAccessException ignored) {
            // Ignore inaccessible fields in audit fallback.
        }
    }

    private String readableLabel(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        StringBuilder builder = new StringBuilder();
        char[] chars = value.toCharArray();
        builder.append(Character.toUpperCase(chars[0]));
        for (int i = 1; i < chars.length; i++) {
            char current = chars[i];
            if (Character.isUpperCase(current)) {
                builder.append(' ');
            }
            builder.append(current);
        }
        return builder.toString();
    }
}
