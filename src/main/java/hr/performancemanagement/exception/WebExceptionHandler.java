package hr.performancemanagement.exception;

import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;

@ControllerAdvice(basePackages = "hr.performancemanagement.controllers")
public class WebExceptionHandler {

    @ExceptionHandler({
            DataIntegrityViolationException.class,
            org.hibernate.exception.ConstraintViolationException.class
    })
    public String handleDataIntegrity(Exception ex, HttpServletRequest request) {
        PortletUtils.addErrorMsg(PortletUtils.sanitiseUserErrorMessage(ex.getMessage()), request, ex);
        return "redirect:" + redirectTarget(request);
    }

    @ExceptionHandler(Exception.class)
    public String handleUnhandled(Exception ex, HttpServletRequest request) {
        PortletUtils.addErrorMsg("An unexpected error occurred. Please try again.", request, ex);
        return "redirect:" + redirectTarget(request);
    }

    private String redirectTarget(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.trim().isEmpty()) {
            return "/";
        }
        try {
            return URI.create(referer).getPath();
        } catch (Exception ignored) {
            return "/";
        }
    }
}
