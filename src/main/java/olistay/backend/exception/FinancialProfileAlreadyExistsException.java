package olistay.backend.exception;

/**
 * Thrown when a user who already has a TenantFinancialProfile attempts to
 * create a new one via createProfile(). Mapped to HTTP 409 by
 * GlobalExceptionHandler — they should use updateProfile() instead.
 */
public class FinancialProfileAlreadyExistsException extends RuntimeException {
    public FinancialProfileAlreadyExistsException(String message) {
        super(message);
    }
}
