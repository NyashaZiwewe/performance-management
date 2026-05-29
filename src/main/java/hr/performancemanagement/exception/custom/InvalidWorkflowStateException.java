package hr.performancemanagement.exception.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidWorkflowStateException extends RuntimeException {

    public InvalidWorkflowStateException(String message) {
        super(message);
    }

    public InvalidWorkflowStateException(String currentState, String attemptedAction) {
        super(String.format("Cannot perform '%s' action in current state: '%s'", attemptedAction, currentState));
    }

    public InvalidWorkflowStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
