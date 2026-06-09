package hr.performancemanagement.exception;

import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;

@ControllerAdvice(basePackages = "hr.performancemanagement.controllers")
public class WebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler({
            BadRequestException.class,
            ResourceNotFoundException.class,
            IllegalArgumentException.class,
            hr.performancemanagement.exception.custom.DuplicateScorecardException.class,
            hr.performancemanagement.exception.custom.InvalidScoreException.class,
            hr.performancemanagement.exception.custom.InvalidWorkflowStateException.class,
            hr.performancemanagement.exception.custom.ResourceNotFoundException.class,
            hr.performancemanagement.exception.custom.UnauthorizedAccessException.class,
            hr.performancemanagement.exception.custom.ValidationException.class
    })
    public String handleUserActionException(RuntimeException ex, HttpServletRequest request) {
        log.warn("User action could not be completed: {}", ex.getMessage());
        PortletUtils.addErrorMsg(PortletUtils.sanitiseUserErrorMessage(ex.getMessage()), request);
        return "redirect:" + redirectTarget(request);
    }

    @ExceptionHandler({
            DataIntegrityViolationException.class,
            org.hibernate.exception.ConstraintViolationException.class,
            javax.validation.ConstraintViolationException.class
    })
    public String handleDataIntegrity(Exception ex, HttpServletRequest request) {
        PortletUtils.addErrorMsg(userFriendlyDataMessage(ex), request, ex);
        return "redirect:" + redirectTarget(request);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            BindException.class,
            MethodArgumentNotValidException.class
    })
    public String handleRequestBinding(Exception ex, HttpServletRequest request) {
        String message = requestBindingMessage(ex);
        log.warn("Invalid web request method={} uri={} message={}",
                request == null ? null : request.getMethod(),
                request == null ? null : request.getRequestURI(),
                message);
        PortletUtils.addValidationErrorMsg(message, request);
        return "redirect:" + redirectTarget(request);
    }

    @ExceptionHandler(Exception.class)
    public String handleUnhandled(Exception ex, HttpServletRequest request) {
        String traceId = PortletUtils.getRequestTraceId(request);
        log.error("Unhandled web exception traceId={}", traceId, ex);
        PortletUtils.addErrorMsg("An unexpected error occurred. Reference: " + traceId, request, ex);
        return "redirect:" + redirectTarget(request);
    }

    private String redirectTarget(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.trim().isEmpty()) {
            return "/";
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getPath();
            if (path == null || path.trim().isEmpty()) {
                return "/";
            }
            String query = uri.getQuery();
            return query == null || query.trim().isEmpty() ? path : path + "?" + query;
        } catch (IllegalArgumentException ignored) {
            return "/";
        }
    }

    private String userFriendlyDataMessage(Exception exception) {
        String message = exception == null ? null : exception.getMessage();
        String friendlyMessage = PortletUtils.sanitiseUserErrorMessage(message);
        if (friendlyMessage != null && !friendlyMessage.trim().isEmpty()) {
            return friendlyMessage;
        }
        return "The record could not be saved because some information is missing or conflicts with existing records.";
    }

    private String requestBindingMessage(Exception exception) {
        if (exception instanceof MissingServletRequestParameterException) {
            String parameterName = ((MissingServletRequestParameterException) exception).getParameterName();
            return "Provide a value for " + humanizeParameterName(parameterName) + " before continuing.";
        }
        if (exception instanceof MethodArgumentTypeMismatchException) {
            String parameterName = ((MethodArgumentTypeMismatchException) exception).getName();
            return "Provide a valid value for " + humanizeParameterName(parameterName) + " before continuing.";
        }
        FieldError fieldError = null;
        if (exception instanceof BindException) {
            fieldError = ((BindException) exception).getFieldError();
        } else if (exception instanceof MethodArgumentNotValidException) {
            fieldError = ((MethodArgumentNotValidException) exception).getBindingResult().getFieldError();
        }
        if (fieldError != null) {
            String defaultMessage = fieldError.getDefaultMessage();
            if (defaultMessage != null && !defaultMessage.trim().isEmpty()) {
                return humanizeParameterName(fieldError.getField()) + ": " + defaultMessage.trim();
            }
            return "Provide a valid value for " + humanizeParameterName(fieldError.getField()) + " before continuing.";
        }
        return "Complete all required fields with valid values before continuing.";
    }

    private String humanizeParameterName(String parameterName) {
        if (parameterName == null || parameterName.trim().isEmpty()) {
            return "the required field";
        }
        String label = parameterName.trim()
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ENGLISH);
        if (label.endsWith(" id")) {
            label = label.substring(0, label.length() - 3);
        }
        return label;
    }
}
