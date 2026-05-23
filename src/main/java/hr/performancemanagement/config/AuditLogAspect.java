package hr.performancemanagement.config;

import hr.performancemanagement.entities.Account;
import hr.performancemanagement.service.api.AuditLogService;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
public class AuditLogAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);
    private final AuditLogService auditLogService;

    public AuditLogAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Pointcut("execution(* hr.performancemanagement.service..*(..)) && " +
            "(execution(* add*(..)) || execution(* save*(..)) || execution(* update*(..)) || execution(* delete*(..)) || execution(* create*(..)) || execution(* upgrade*(..)))")
    public void serviceMethods() {
    }

    @Pointcut("execution(* hr.performancemanagement.controllers..*(..))")
    public void controllerMethods() {
    }

    @Pointcut("execution(* hr.performancemanagement.repository..*.*(..))")
    public void repositoryMethods() {
    }

    @Around("controllerMethods()")
    public Object logWebTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (!isTransactionalRequest(request)) {
            return joinPoint.proceed();
        }

        long startedAt = System.currentTimeMillis();
        String traceId = PortletUtils.getRequestTraceId(request);
        String action = joinPoint.getSignature().toShortString();
        log.info("Web transaction started traceId={} action={} request={} user={} clientId={}",
                traceId,
                action,
                requestSummary(request),
                userSummary(request),
                clientId(request));
        try {
            Object result = joinPoint.proceed();
            log.info("Web transaction succeeded traceId={} action={} durationMs={} result={}",
                    traceId,
                    action,
                    System.currentTimeMillis() - startedAt,
                    summarizeValue(result));
            return result;
        } catch (Throwable throwable) {
            log.error("Web transaction failed traceId={} action={} durationMs={} errorType={} errorMessage={}",
                    traceId,
                    action,
                    System.currentTimeMillis() - startedAt,
                    throwable.getClass().getName(),
                    throwable.getMessage(),
                    throwable);
            throw throwable;
        }
    }

    @Around("repositoryMethods()")
    public Object logRepositoryFailure(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            HttpServletRequest request = currentRequest();
            String traceId = request == null ? "SYSTEM-" + System.currentTimeMillis() : PortletUtils.getRequestTraceId(request);
            String repositoryName = joinPoint.getSignature().getDeclaringTypeName();
            log.error("Repository access failed traceId={} repository={} method={} criteria={} user={} clientId={} errorType={} errorMessage={}",
                    traceId,
                    repositoryName,
                    joinPoint.getSignature().getName(),
                    summarizeArgs(joinPoint.getArgs()),
                    userSummary(request),
                    clientId(request),
                    throwable.getClass().getName(),
                    throwable.getMessage(),
                    throwable);
            throw throwable;
        }
    }

    @Around("serviceMethods()")
    public Object auditTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        String action = joinPoint.getSignature().getName();
        String tableName = joinPoint.getTarget().getClass().getSimpleName().replace("Service", "");
        long startedAt = System.currentTimeMillis();
        HttpServletRequest request = currentRequest();
        String traceId = request == null ? "SYSTEM-" + startedAt : PortletUtils.getRequestTraceId(request);
        log.info("Transaction started traceId={} action={} table={} user={} clientId={} args={}",
                traceId,
                action,
                tableName,
                userSummary(request),
                clientId(request),
                summarizeArgs(joinPoint.getArgs()));
        try {
            Object result = joinPoint.proceed();
            try {
                auditLogService.logSuccess(action, tableName, joinPoint.getArgs(), result);
            } catch (Exception exception) {
                log.warn("Could not persist success audit log for action={} table={} because {}", action, tableName, exception.getMessage());
            }
            log.info("Transaction succeeded traceId={} action={} table={} durationMs={}",
                    traceId,
                    action,
                    tableName,
                    System.currentTimeMillis() - startedAt);
            return result;
        } catch (Throwable throwable) {
            try {
                auditLogService.logFailure(action, tableName, joinPoint.getArgs(), throwable);
            } catch (Exception exception) {
                log.warn("Could not persist failure audit log for action={} table={} because {}", action, tableName, exception.getMessage());
            }
            log.error("Transaction failed traceId={} action={} table={} durationMs={} errorType={} errorMessage={}",
                    traceId,
                    action,
                    tableName,
                    System.currentTimeMillis() - startedAt,
                    throwable.getClass().getName(),
                    throwable.getMessage(),
                    throwable);
            throw throwable;
        }
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    private boolean isTransactionalRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String method = request.getMethod();
        return method != null
                && !"GET".equalsIgnoreCase(method)
                && !"HEAD".equalsIgnoreCase(method)
                && !"OPTIONS".equalsIgnoreCase(method);
    }

    private String requestSummary(HttpServletRequest request) {
        if (request == null) {
            return "system";
        }
        String query = request.getQueryString();
        return request.getMethod() + " " + request.getRequestURI() + (query == null ? "" : "?" + query)
                + " remoteAddr=" + request.getRemoteAddr();
    }

    private String userSummary(HttpServletRequest request) {
        Account loggedUser = loggedUser(request);
        if (loggedUser == null) {
            return "anonymous";
        }
        return "Account#" + loggedUser.getId() + " " + loggedUser.getFullName() + " <" + loggedUser.getEmail() + ">";
    }

    private String clientId(HttpServletRequest request) {
        Account loggedUser = loggedUser(request);
        return loggedUser == null ? null : String.valueOf(loggedUser.getClientId());
    }

    private Account loggedUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("loggedUser") instanceof Account)) {
            return null;
        }
        return (Account) session.getAttribute("loggedUser");
    }

    private String summarizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        List<String> summaries = new ArrayList<>();
        for (Object arg : args) {
            summaries.add(summarizeValue(arg));
        }
        return summaries.toString();
    }

    private String summarizeValue(Object value) {
        if (value == null) {
            return "null";
        }
        String text = value.toString();
        if (text.length() > 200) {
            return text.substring(0, 200) + "...";
        }
        return text;
    }
}
