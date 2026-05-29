package hr.performancemanagement.exception.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends RuntimeException {

    private Map<String, String> fieldErrors = new HashMap<>();

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String field, String errorMessage) {
        super(String.format("Validation failed for field '%s': %s", field, errorMessage));
        this.fieldErrors.put(field, errorMessage);
    }

    public ValidationException(Map<String, String> fieldErrors) {
        super("Validation failed for multiple fields");
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void addFieldError(String field, String errorMessage) {
        this.fieldErrors.put(field, errorMessage);
    }
}
