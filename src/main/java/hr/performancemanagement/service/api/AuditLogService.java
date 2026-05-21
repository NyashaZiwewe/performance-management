package hr.performancemanagement.service.api;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.entities.AuditLog;
import hr.performancemanagement.repository.AuditLogRepository;
import hr.performancemanagement.utils.wrappers.AuditLogFilterWrapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import javax.persistence.criteria.Predicate;
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

public interface AuditLogService {
    void logSuccess(String action, String tableName, Object[] args, Object result);
    void logFailure(String action, String tableName, Object[] args, Throwable throwable);
    List<AuditLog> searchLogs(AuditLogFilterWrapper wrapper);
    boolean hasInvalidDateRange(Date startDate, Date endDate);
}
