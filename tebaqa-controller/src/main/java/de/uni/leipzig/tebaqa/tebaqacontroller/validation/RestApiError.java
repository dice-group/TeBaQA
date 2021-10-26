package de.uni.leipzig.tebaqa.tebaqacontroller.validation;

//public class RestApiError {
//}

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

class RestApiError {

    private HttpStatus status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private LocalDateTime timestamp;
    private String message;
    @JsonIgnore
    private String debugMessage;

    private RestApiError() {
        timestamp = LocalDateTime.now();
    }

    public RestApiError(HttpStatus status) {
        this();
        this.status = status;
    }

    public RestApiError(HttpStatus status, Throwable ex) {
        this();
        this.status = status;
        this.message = "Unexpected error";
        this.debugMessage = ex.getLocalizedMessage();
    }

    public RestApiError(HttpStatus status, String message) {
        this();
        this.status = status;
        this.message = message;
    }

    public RestApiError(HttpStatus status, String message, Throwable ex) {
        this();
        this.status = status;
        this.message = message;
        this.debugMessage = ex.getLocalizedMessage();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getDebugMessage() {
        return debugMessage;
    }
}