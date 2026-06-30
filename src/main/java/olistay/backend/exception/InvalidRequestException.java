package olistay.backend.exception;

/**
 * Thrown when a request payload is structurally invalid — e.g. malformed
 * JSON in a multipart "property" part that can't be deserialized.
 * Mapped to HTTP 400 by GlobalExceptionHandler.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
