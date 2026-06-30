package olistay.backend.exception;

/**
 * Thrown when an image upload to Cloudinary fails, or when an uploaded file
 * fails basic validation (wrong content type, too large, empty).
 * Mapped to HTTP 400 by GlobalExceptionHandler — these are caller-correctable
 * (re-select a valid image), not server faults.
 */
public class ImageUploadException extends RuntimeException {
    public ImageUploadException(String message) {
        super(message);
    }

    public ImageUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}