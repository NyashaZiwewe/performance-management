package hr.performancemanagement.exception;

import hr.performancemanagement.utils.PortletUtils.PortletUtils;
import hr.performancemanagement.utils.dto.CommonResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "hr.performancemanagement.service.resource")
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<CommonResponse<Object>> handleNotFound(ResourceNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<CommonResponse<Object>> handleBadRequest(BadRequestException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({
            DataIntegrityViolationException.class,
            org.hibernate.exception.ConstraintViolationException.class
    })
    public ResponseEntity<CommonResponse<Object>> handleDataIntegrity(Exception exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, PortletUtils.sanitiseUserErrorMessage(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Object>> handleGeneral(Exception exception) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
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
}
