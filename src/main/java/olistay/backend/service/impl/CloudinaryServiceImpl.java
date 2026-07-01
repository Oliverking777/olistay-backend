package olistay.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import olistay.backend.exception.ImageUploadException;
import olistay.backend.service.CloudinaryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/jpeg", "image/png", "image/webp");

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    @Override
    public CloudinaryUploadResult upload(MultipartFile file, String folder) {
        validate(file);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            // Cap dimensions on upload so a host uploading a
                            // 12MP photo straight from a phone doesn't bloat
                            // storage/bandwidth; Cloudinary handles the resize.
                            // NOTE: must be a Transformation object, not a raw
                            // Map — the SDK can't serialize a plain Map into a
                            // valid transformation string (causes "Invalid
                            // transformation component" errors).
                            "transformation", new Transformation()
                                    .width(2000)
                                    .height(2000)
                                    .crop("limit")
                    )
            );

            String secureUrl = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");

            if (secureUrl == null || publicId == null) {
                throw new ImageUploadException("Cloudinary upload did not return a valid result");
            }

            return new CloudinaryUploadResult(secureUrl, publicId);

        } catch (IOException e) {
            throw new ImageUploadException("Failed to read uploaded file", e);
        } catch (Exception e) {
            // Cloudinary SDK throws plain RuntimeException/IOException for
            // network/API errors — wrap uniformly so callers only ever see
            // ImageUploadException.
            throw new ImageUploadException("Image upload to Cloudinary failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (Exception e) {
            // Deletion failures shouldn't block the surrounding DB transaction
            // (e.g. deleting a Property whose images fail to delete from
            // Cloudinary shouldn't leave the Property un-deletable). Log and
            // move on — an orphaned Cloudinary asset is a minor cleanup issue,
            // not a correctness issue.
            System.err.println("[CloudinaryService] Failed to delete asset " + publicId + ": " + e.getMessage());
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageUploadException("Uploaded file is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ImageUploadException("Image exceeds maximum allowed size of 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ImageUploadException(
                    "Unsupported image type. Allowed: JPEG, PNG, WEBP"
            );
        }
    }
}