package olistay.backend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Wraps all interaction with Cloudinary. Nothing outside this interface
 * (and its impl) should import com.cloudinary.* directly — keeps the
 * provider swappable and keeps validation logic in one place.
 */
public interface CloudinaryService {

    /**
     * Uploads a single image file to Cloudinary and returns the result.
     * Validates content type and size before attempting the upload.
     *
     * @param file       the multipart file from the request
     * @param folder     Cloudinary folder to organize uploads (e.g. "olistay/properties/42")
     * @throws olistay.backend.exception.ImageUploadException on validation failure or upload error
     */
    CloudinaryUploadResult upload(MultipartFile file, String folder);

    /**
     * Deletes an asset from Cloudinary by its public_id.
     * Called when a PropertyImage row is removed, to avoid orphaned assets
     * accumulating in the Cloudinary account.
     */
    void delete(String publicId);

    /**
     * Result of a successful Cloudinary upload — just the two fields
     * PropertyImage needs to persist.
     */
    record CloudinaryUploadResult(String secureUrl, String publicId) {}
}
