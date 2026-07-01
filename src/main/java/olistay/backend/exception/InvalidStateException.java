package olistay.backend.exception;

/**
 * Thrown when an operation is attempted on a resource that is in the wrong
 * state for that operation — e.g. approving a listing that isn't UNDER_REVIEW,
 * or demoting a user who isn't a HOST.
 * Mapped to HTTP 409 (Conflict) by GlobalExceptionHandler.
 */
public class InvalidStateException extends RuntimeException {
    public InvalidStateException(String message) {
        super(message);
    }
}