package olistay.backend.exception;

/**
 * Thrown when a refresh token is missing, expired, revoked, or — in the
 * theft-detection case — reused outside the grace window. Mapped to HTTP 401
 * by GlobalExceptionHandler; the frontend should treat this as "force logout,
 * redirect to login" rather than retry.
 */
public class TokenRefreshException extends RuntimeException {
    public TokenRefreshException(String message) {
        super(message);
    }
}
