package olistay.backend.exception;

/**
 * Thrown when a HOST attempts to update or delete a property they do not own.
 * Mapped to HTTP 403 by GlobalExceptionHandler — distinct from Spring
 * Security's AccessDeniedException, which fires for role-level checks
 * (e.g. GUEST hitting /host/**). This one fires for ownership-level checks
 * within an otherwise-permitted endpoint.
 */
public class PropertyOwnershipException extends RuntimeException {
    public PropertyOwnershipException(String message) {
        super(message);
    }
}
