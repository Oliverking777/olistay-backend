package olistay.backend.exception;

/**
 * Thrown when a user who is already a HOST attempts the become-host flow.
 * Mapped to HTTP 409 by GlobalExceptionHandler.
 */
public class AlreadyHostException extends RuntimeException {
    public AlreadyHostException(String message) {
        super(message);
    }
}
