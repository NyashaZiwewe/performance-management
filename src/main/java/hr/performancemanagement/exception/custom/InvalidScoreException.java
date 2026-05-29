package hr.performancemanagement.exception.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidScoreException extends RuntimeException {

    public InvalidScoreException(String message) {
        super(message);
    }

    public InvalidScoreException(double score, double min, double max) {
        super(String.format("Invalid score: %.2f. Score must be between %.2f and %.2f", score, min, max));
    }

    public InvalidScoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
