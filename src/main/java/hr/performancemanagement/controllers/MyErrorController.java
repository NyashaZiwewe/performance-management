package hr.performancemanagement.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

@Controller
public class MyErrorController implements ErrorController {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request){
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if(statusCode == HttpStatus.BAD_REQUEST.value()) {
                return "error-400";
            }
            else if(statusCode == HttpStatus.UNAUTHORIZED.value()) {
                return "error-401";
            }
            else if(statusCode == HttpStatus.FORBIDDEN.value()) {
                return "error-403";
            }
            else if(statusCode == HttpStatus.NOT_FOUND.value()) {
                return "error-404";
            }
            else if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
//                removeSessionAttributesAfterError(request);
                return "error-500";
            }
        }

        return "error";
    }

    private void removeSessionAttributesAfterError(HttpServletRequest request) {
        Enumeration<String> attributeNames = request.getSession().getAttributeNames();
        while (attributeNames.hasMoreElements()){
            String attribute = attributeNames.nextElement();
            logger.info(">>>> Session Attribute : "+ attribute);
            if(!attribute.equals("SPRING_SECURITY_CONTEXT")){
                request.getSession().removeAttribute(attribute);
                logger.error(">>>> Removed session Attribute : "+ attribute);
            }
        }
    }
}
