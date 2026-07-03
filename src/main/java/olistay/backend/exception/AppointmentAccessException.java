package olistay.backend.exception;

/**
 * Thrown when a user attempts an action on an appointment they are not
 * a party to, or attempts a status transition they are not permitted to
 * make (e.g. a tenant trying to confirm their own request).
 * Mapped to HTTP 403 by GlobalExceptionHandler.
 */
public class AppointmentAccessException extends RuntimeException {
    public AppointmentAccessException(String message) {
        super(message);
    }
}
