package ua.readmania.common;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a domain business rule is violated (mapped to 400 Bad Request or 409 Conflict depending on context).
 */
public class BusinessRuleException extends RuntimeException {

    private final HttpStatus status;

    public BusinessRuleException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public BusinessRuleException(String message, HttpStatus status) {
        super(message);
        this.status = status != null ? status : HttpStatus.BAD_REQUEST;
    }

    public BusinessRuleException(String message, Throwable cause) {
        this(message, HttpStatus.BAD_REQUEST, cause);
    }

    public BusinessRuleException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status != null ? status : HttpStatus.BAD_REQUEST;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
