package de.uni.leipzig.tebaqa.tebaqacontroller.validation;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TeBaQAException.class)
    protected ResponseEntity<Object> handleSystemException(TeBaQAException ex) {
        RestApiError restApiError = new RestApiError(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getE());
        return ResponseEntity.status(restApiError.getStatus()).body(restApiError);
    }

    @ExceptionHandler(KBNotFoundException.class)
    protected ResponseEntity<Object> handleSystemException(KBNotFoundException ex) {
        RestApiError restApiError = new RestApiError(HttpStatus.NOT_FOUND, ex.getMessage());
        return ResponseEntity.status(restApiError.getStatus()).body(restApiError);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        RestApiError restApiError = new RestApiError(HttpStatus.EXPECTATION_FAILED, "Uploaded files exceeds max size");
        return ResponseEntity.status(restApiError.getStatus()).body(restApiError);
    }


    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleSystemException(Exception ex) {
        RestApiError restApiError = new RestApiError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        return ResponseEntity.status(restApiError.getStatus()).body(restApiError);
    }
}