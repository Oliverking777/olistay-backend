package olistay.backend.dto;

import olistay.backend.entity.Property;
import olistay.backend.enums.InfraZone;
import olistay.backend.enums.PropertyStatus;
import olistay.backend.enums.PropertyType;

/**
 * Lightweight property representation for list/browse/search views.
 * Carries only the primary image URL (not the full images collection) to
 * keep paginated list payloads small — a browse page showing 20 properties
 * shouldn't pull every image for every property.
 *
 * Use PropertyResponseDTO (full detail, all images) for GET /properties/{id}.
 */
public record PropertySummaryDTO(
        Long id,
        String title,
        String neighbourhood,
        String city,
        PropertyType propertyType,
        String unitType,
        Integer numBedrooms,
        Integer numBathrooms,
        Double areaM2,
        Double rentXaf,
        InfraZone infraZone,
        PropertyStatus status,
        String primaryImageUrl
) {
    /**
     * primaryImageUrl is resolved by the caller (service layer) via
     * PropertyImageRepository.findFirstByPropertyAndIsPrimaryTrue() rather
     * than navigating property.getImages() here, to avoid triggering a lazy
     * load of the entire images collection just to read one URL.
     */
    public static PropertySummaryDTO fromEntity(Property p, String primaryImageUrl) {
        return new PropertySummaryDTO(
                p.getId(),
                p.getTitle(),
                p.getNeighbourhood(),
                p.getCity(),
                p.getPropertyType(),
                p.getUnitType(),
                p.getNumBedrooms(),
                p.getNumBathrooms(),
                p.getAreaM2(),
                p.getRentXaf(),
                p.getInfraZone(),
                p.getStatus(),
                primaryImageUrl
        );
    }
}
