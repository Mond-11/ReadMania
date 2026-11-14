package ua.readmania.common;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public class LoginRateLimitedException extends RuntimeException {

    private final Instant retryAfter;
    private final HttpStatus status;

    public LoginRateLimitedException(String message, Instant retryAfter) {
        this(message, retryAfter, HttpStatus.FORBIDDEN);
    }

    public LoginRateLimitedException(String message, Instant retryAfter, HttpStatus status) {
        super(message);
        this.retryAfter = retryAfter;
        this.status = status;
    }

    public Instant getRetryAfter() {
        return retryAfter;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
