package olistay.backend.exception;

/**
 * Thrown when a tenant tries to create a second PENDING or CONFIRMED
 * appointment for a property they already have an active request on.
 * Mapped to HTTP 409 by GlobalExceptionHandler.
 */
public class AppointmentConflictException extends RuntimeException {
    public AppointmentConflictException(String message) {
        super(message);
    }
}