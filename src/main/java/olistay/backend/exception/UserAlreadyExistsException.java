package olistay.backend.exception;

/**
 * Thrown during registration when the supplied email already belongs to an
 * existing account. Mapped to HTTP 409 by GlobalExceptionHandler.
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
