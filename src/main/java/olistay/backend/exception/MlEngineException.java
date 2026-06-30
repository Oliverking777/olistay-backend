package olistay.backend.exception;

/**
 * Thrown when a call to the FastAPI AI Engine fails — connection refused,
 * timeout, non-2xx response, or unparseable response body.
 *
 * Mapped to HTTP 503 by GlobalExceptionHandler (Service Unavailable) rather
 * than 500: the failure is in an external dependency the Spring backend
 * doesn't control, not a bug in the Spring backend itself. Callers (e.g.
 * PropertyServiceImpl when suggesting a rent estimate) should treat this as
 * "the ML feature is temporarily down" and degrade gracefully where
 * possible, rather than failing the whole request.
 */
public class MlEngineException extends RuntimeException {
    public MlEngineException(String message) {
        super(message);
    }

    public MlEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}