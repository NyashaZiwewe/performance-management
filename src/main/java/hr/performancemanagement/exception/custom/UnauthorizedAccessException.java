package hr.performancemanagement.exception.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(String user, String resource) {
        super(String.format("User '%s' is not authorized to access resource: '%s'", user, resource));
    }

    public UnauthorizedAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
