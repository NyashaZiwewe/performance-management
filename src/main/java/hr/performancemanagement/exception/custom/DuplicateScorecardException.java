package hr.performancemanagement.exception.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateScorecardException extends RuntimeException {

    public DuplicateScorecardException(String message) {
        super(message);
    }

    public DuplicateScorecardException(String ownerName, String reportingPeriod) {
        super(String.format("Scorecard already exists for owner '%s' in reporting period '%s'", ownerName, reportingPeriod));
    }

    public DuplicateScorecardException(String message, Throwable cause) {
        super(message, cause);
    }
}
