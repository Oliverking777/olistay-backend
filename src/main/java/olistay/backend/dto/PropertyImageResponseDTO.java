package olistay.backend.dto;

import olistay.backend.entity.PropertyImage;

/**
 * Returned inside PropertyResponseDTO.images (detail view) and standalone
 * from image upload/delete endpoints.
 */
public record PropertyImageResponseDTO(
        Long id,
        String imageUrl,
        Boolean isPrimary,
        Integer uploadOrder
) {
    public static PropertyImageResponseDTO fromEntity(PropertyImage image) {
        return new PropertyImageResponseDTO(
                image.getId(),
                image.getImageUrl(),
                image.getIsPrimary(),
                image.getUploadOrder()
        );
    }
}
