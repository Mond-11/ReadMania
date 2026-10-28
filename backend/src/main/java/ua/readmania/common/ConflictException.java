package ua.readmania.common;

/**
 * Exception thrown when a state conflict or duplicate resource is detected (mapped to 409 Conflict).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
