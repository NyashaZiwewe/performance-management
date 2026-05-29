package hr.performancemanagement.exception;

import hr.performancemanagement.exception.custom.*;
import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.dto.CommonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice(basePackages = "hr.performancemanagement.service.resource")
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<CommonResponse<Object>> handleNotFound(ResourceNotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    public ResponseEntity<CommonResponse<Object>> handleBadRequest(RuntimeException exception) {
        log.warn("Bad request: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidScoreException.class)
    public ResponseEntity<CommonResponse<Object>> handleInvalidScore(InvalidScoreException exception) {
        log.warn("Invalid score: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(DuplicateScorecardException.class)
    public ResponseEntity<CommonResponse<Object>> handleDuplicateScorecard(DuplicateScorecardException exception) {
        log.warn("Duplicate scorecard: {}", exception.getMessage());
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidWorkflowStateException.class)
    public ResponseEntity<CommonResponse<Object>> handleInvalidWorkflowState(InvalidWorkflowStateException exception) {
        log.warn("Invalid workflow state: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<CommonResponse<Object>> handleUnauthorizedAccess(UnauthorizedAccessException exception) {
        log.warn("Unauthorized access: {}", exception.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<CommonResponse<Object>> handleValidationException(ValidationException exception) {
        log.warn("Validation exception: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<CommonResponse<Object>> handleValidation(Exception exception) {
        BindingResult bindingResult = exception instanceof MethodArgumentNotValidException
                ? ((MethodArgumentNotValidException) exception).getBindingResult()
                : ((BindException) exception).getBindingResult();
        return buildResponse(HttpStatus.BAD_REQUEST, collectValidationMessage(bindingResult));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<CommonResponse<Object>> handleRequestBinding(Exception exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, PortletUtils.sanitiseUserErrorMessage(exception.getMessage()));
    }

    @ExceptionHandler({
            DataIntegrityViolationException.class,
            org.hibernate.exception.ConstraintViolationException.class,
            javax.validation.ConstraintViolationException.class
    })
    public ResponseEntity<CommonResponse<Object>> handleDataIntegrity(Exception exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, PortletUtils.sanitiseUserErrorMessage(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Object>> handleGeneral(Exception exception, HttpServletRequest request) {
        String traceId = PortletUtils.getRequestTraceId(request);
        log.error("Unhandled REST exception traceId={}", traceId, exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Reference: " + traceId);
    }

    private ResponseEntity<CommonResponse<Object>> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                CommonResponse.builder()
                        .isSuccess(false)
                        .statusCode(status.value())
                        .message(message)
                        .data(null)
                        .build()
        );
    }

    private String collectValidationMessage(BindingResult bindingResult) {
        if (bindingResult == null || !bindingResult.hasErrors()) {
            return "The request could not be validated.";
        }

        FieldError firstFieldError = bindingResult.getFieldError();
        if (firstFieldError != null && firstFieldError.getDefaultMessage() != null) {
            return firstFieldError.getField() + ": " + firstFieldError.getDefaultMessage();
        }

        if (bindingResult.getAllErrors().isEmpty()) {
            return "The request could not be validated.";
        }
        String message = bindingResult.getAllErrors().get(0).getDefaultMessage();
        return message == null || message.trim().isEmpty()
                ? "The request could not be validated."
                : message.trim();
    }
}
