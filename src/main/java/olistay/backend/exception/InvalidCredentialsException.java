package olistay.backend.exception;

/**
 * Thrown when login fails due to a wrong email/password combination.
 * Deliberately generic (doesn't say which of the two is wrong) to avoid
 * leaking whether an email is registered. Mapped to HTTP 401 by
 * GlobalExceptionHandler.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}