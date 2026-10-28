package ua.readmania.common;

/**
 * Exception thrown when a requested resource is not found (mapped to 404 Not Found).
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
