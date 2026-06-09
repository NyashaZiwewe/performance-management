package hr.performancemanagement.utils.PortletUtils;

import hr.performancemanagement.entities.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.UUID;

public class PortletUtils {
    public static final String ERROR_MSGS = "errorMsgs";
    public static final String INFO_MSGS = "infoMsgs";
    private static final Logger log = LoggerFactory.getLogger(PortletUtils.class);
    private static final String ERROR_TRACE_ID = "errorTraceId";



    public static String getUsername(HttpServletRequest request) {
        return request.getUserPrincipal().getName().toLowerCase();
    }

    public static void addErrorMsg(String msg, HttpServletRequest request) {
        logError(msg, request, null);
        HttpSession session = request.getSession();
        List<String> errorMsgs = (List<String>) session.getAttribute(ERROR_MSGS);
        if (errorMsgs == null) {
            errorMsgs = new ArrayList<>();
            session.setAttribute(ERROR_MSGS, errorMsgs);
        }
        errorMsgs.add(sanitiseUserErrorMessage(msg));
    }

    public static void addErrorMsg(String msg, HttpServletRequest request, Throwable throwable) {
        logError(msg, request, throwable);
        addErrorMessageToSession(msg, request);
    }

    public static void addValidationErrorMsg(String msg, HttpServletRequest request) {
        log.warn("Validation rejected method={} uri={} message={}",
                request == null ? null : request.getMethod(),
                request == null ? null : request.getRequestURI(),
                sanitiseUserErrorMessage(msg));
        addErrorMessageToSession(msg, request);
    }

    private static void addErrorMessageToSession(String msg, HttpServletRequest request) {
        HttpSession session = request.getSession();
        List<String> errorMsgs = (List<String>) session.getAttribute(ERROR_MSGS);
        if (errorMsgs == null) {
            errorMsgs = new ArrayList<>();
            session.setAttribute(ERROR_MSGS, errorMsgs);
        }
        errorMsgs.add(sanitiseUserErrorMessage(msg));
    }

    public static String getRequestTraceId(HttpServletRequest request) {
        if (request == null) {
            return "NO_REQUEST";
        }
        Object existingTraceId = request.getAttribute(ERROR_TRACE_ID);
        if (existingTraceId != null) {
            return String.valueOf(existingTraceId);
        }
        String traceId = UUID.randomUUID().toString();
        request.setAttribute(ERROR_TRACE_ID, traceId);
        return traceId;
    }

    public static void addInfoMsg(String msg, HttpServletRequest request) {
        HttpSession session = request.getSession();
        List<String> infoMsgs = (List<String>) session.getAttribute(INFO_MSGS);
        if (infoMsgs == null) {
            infoMsgs = new ArrayList<>();
            session.setAttribute(INFO_MSGS, infoMsgs);
        }
        infoMsgs.add(msg);
    }

    public static String sanitiseUserErrorMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return "The action could not be completed. Please check the details and try again.";
        }

        String message = rawMessage.trim();
        String lower = message.toLowerCase(Locale.ENGLISH);

        if ((lower.contains("required request parameter") && lower.contains("not present"))
                || lower.contains("required request part")
                || lower.contains("missing request parameter")) {
            return "Complete all required fields before continuing.";
        }

        if (lower.contains("cannot delete or update a parent row")
                || lower.contains("foreign key constraint fails")
                || lower.contains("referential integrity constraint")
                || lower.contains("violates foreign key constraint")) {
            return "This record cannot be changed because it is linked to other records.";
        }

        if (lower.contains("not-null property references a null or transient value")
                || lower.contains("cannot be null")
                || lower.contains("not null constraint")
                || lower.contains("null value in column")) {
            return "The action could not be completed because required information is missing.";
        }

        if (lower.contains("data too long")
                || lower.contains("value too long")
                || lower.contains("string or binary data would be truncated")) {
            return "The action could not be completed because one of the values is too long.";
        }

        if (lower.contains("date must be")
                || lower.contains("could not parse")
                || lower.contains("failed to convert")
                || lower.contains("for input string")
                || lower.contains("type mismatch")) {
            return "The action could not be completed because one or more values are in the wrong format.";
        }

        if (lower.contains("constraint [")
                || lower.contains("could not execute statement")
                || lower.contains("duplicate entry")
                || lower.contains("duplicate key")
                || lower.contains("unique constraint")
                || lower.contains("data integrity")
                || lower.contains("sql [")
                || lower.contains("sqlexception")
                || lower.contains("constraintviolationexception")
                || lower.contains("violates")) {
            return "This record could not be saved because it conflicts with existing data. Please check for duplicate values in fields that must be unique.";
        }

        if (lower.contains("nullpointerexception")
                || lower.contains("indexoutofboundsexception")
                || lower.contains("no value present")
                || lower.contains("could not initialize proxy")
                || lower.contains("lazyinitializationexception")
                || lower.contains("nested exception is")
                || lower.contains("java.lang.")
                || lower.contains("org.hibernate")
                || lower.contains("org.springframework")) {
            return "The action could not be completed. Please check the details and try again.";
        }

        return message;
    }


    public static String getMessages(HttpServletRequest request) {
        StringBuilder buffer = new StringBuilder();
        HttpSession session = request.getSession();
        List<String> infoMsgs = (List<String>) session.getAttribute(INFO_MSGS);
        List<String> errorMsgs = (List<String>) session.getAttribute(ERROR_MSGS);

        if (infoMsgs != null) {

            for (String msg : infoMsgs) {
                buffer.append(msg + "\n");
            }
        }

        if (errorMsgs != null) {

            for (String msg : errorMsgs) {
                buffer.append(msg + "\n");
            }

        }
        session.removeAttribute(ERROR_MSGS);
        session.removeAttribute(INFO_MSGS);
        return buffer.toString();
    }


    public static void addMessagesToPage(ModelAndView modelAndView, HttpServletRequest request) {
        HttpSession session = request.getSession();
        List<String> infoMsgs = (List<String>) session.getAttribute(INFO_MSGS);
        List<String> errorMsgs = (List<String>) session.getAttribute(ERROR_MSGS);
//        Set<String> notifications = CacheService.getNotificationMsgs(getUsername(request));
//        if (notifications != null) {
//            if (infoMsgs == null) {
//                infoMsgs = new ArrayList<>();
//            }
//            infoMsgs.addAll(notifications);
//        }
        if (infoMsgs != null) {
            modelAndView.addObject(INFO_MSGS, infoMsgs);
        }

        if (errorMsgs != null) {
            modelAndView.addObject(ERROR_MSGS, errorMsgs);
        }
        session.removeAttribute(ERROR_MSGS);
        session.removeAttribute(INFO_MSGS);
    }

    private static void logError(String msg, HttpServletRequest request, Throwable throwable) {
        if (request == null) {
            if (throwable == null) {
                log.error("Application error without request context: {}", msg);
            } else {
                log.error("Application error without request context: {}", msg, throwable);
            }
            return;
        }

        String traceId = getRequestTraceId(request);
        HttpSession session = request.getSession(false);
        Account loggedUser = session == null ? null : (Account) session.getAttribute("loggedUser");
        String userId = loggedUser == null ? null : String.valueOf(loggedUser.getId());
        String clientId = loggedUser == null ? null : String.valueOf(loggedUser.getClientId());
        String originalUri = valueOf(request.getAttribute("javax.servlet.error.request_uri"));

        if (throwable == null) {
            log.error("Application error traceId={} method={} uri={} originalUri={} userId={} clientId={} message={}",
                    traceId,
                    request.getMethod(),
                    request.getRequestURI(),
                    originalUri,
                    userId,
                    clientId,
                    msg);
        } else {
            log.error("Application error traceId={} method={} uri={} originalUri={} userId={} clientId={} message={}",
                    traceId,
                    request.getMethod(),
                    request.getRequestURI(),
                    originalUri,
                    userId,
                    clientId,
                    msg,
                    throwable);
        }
    }

    private static String valueOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }


}
