package olistay.backend.exception;

/**
 * Thrown when a national ID number submitted during host promotion is already
 * registered to another account. Mapped to HTTP 409 by GlobalExceptionHandler.
 */
public class DuplicateNationalIdException extends RuntimeException {
    public DuplicateNationalIdException(String message) {
        super(message);
    }
}
