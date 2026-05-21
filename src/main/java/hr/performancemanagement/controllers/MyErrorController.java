package hr.performancemanagement.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;

@Controller
public class MyErrorController implements ErrorController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String originalUri = String.valueOf(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        String referer = request.getHeader("Referer");

        if (isIgnoredMissingResource(statusCode, originalUri)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        String errorMessage = message;
        if (throwable != null) {
            errorMessage = extractRootCauseMessage(throwable);
        }
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            errorMessage = getFriendlyErrorMessage(statusCode);
        }
        PortletUtils.addErrorMsg("Error occurred: " + errorMessage, request, throwable);

        if (referer != null && !referer.trim().isEmpty()) {
            try {
                URI uri = URI.create(referer);
                return "redirect:" + uri.getPath();
            } catch (Exception ignored) {
                logger.warn("Failed to parse error referer {}", referer);
            }
        }
        return viewForStatus(statusCode);
    }

    private String viewForStatus(Integer statusCode) {
        if (statusCode == null) {
            return "error";
        }
        if (statusCode == HttpStatus.BAD_REQUEST.value()) {
            return "error-400";
        }
        if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
            return "error-401";
        }
        if (statusCode == HttpStatus.FORBIDDEN.value()) {
            return "error-403";
        }
        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            return "error-404";
        }
        if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            return "error-500";
        }
        return "error";
    }

    private boolean isIgnoredMissingResource(Integer statusCode, String originalUri) {
        if (statusCode == null || statusCode != HttpStatus.NOT_FOUND.value() || originalUri == null) {
            return false;
        }
        return originalUri.endsWith(".map")
                || originalUri.equals("/favicon.ico")
                || originalUri.startsWith("/.well-known/appspecific/")
                || originalUri.startsWith("/css/")
                || originalUri.startsWith("/js/")
                || originalUri.startsWith("/img/")
                || originalUri.startsWith("/font-awesome/");
    }

    private String extractRootCauseMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return PortletUtils.sanitiseUserErrorMessage(cause.getMessage());
    }

    private String getFriendlyErrorMessage(Integer statusCode) {
        if (statusCode == null) {
            return "An unexpected error occurred. Please try again.";
        }
        if (statusCode == HttpStatus.BAD_REQUEST.value()) {
            return "The request could not be processed. Please check the information you entered.";
        }
        if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
            return "You are not authorized to access this resource. Please login again.";
        }
        if (statusCode == HttpStatus.FORBIDDEN.value()) {
            return "You do not have permission to perform this action.";
        }
        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            return "The page or resource you are looking for could not be found.";
        }
        if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            return "A system error occurred. Please contact the administrator.";
        }
        if (statusCode == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            return "The service is currently unavailable. Please try again later.";
        }
        return "An unexpected system error occurred.";
    }
}
