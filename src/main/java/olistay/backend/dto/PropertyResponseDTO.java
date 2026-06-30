package olistay.backend.dto;

import olistay.backend.entity.Property;
import olistay.backend.enums.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full property detail — returned for GET /properties/{id} and after
 * create/update. Contains every field the ML engine and frontend need.
 *
 * The static fromEntity() factory is the single source of truth for
 * entity → DTO mapping so field additions only need updating here.
 */
public record PropertyResponseDTO(

        Long id,

        // Ownership
        Long hostId,
        String hostName,

        // Listing meta
        String title,
        String description,
        PropertyStatus status,

        // Location
        String neighbourhood,
        String city,
        Double gpsLat,
        Double gpsLon,
        InfraZone infraZone,

        // Property type
        PropertyType propertyType,
        String unitType,

        // Dimensions
        Double lengthM,
        Double widthM,
        Double areaM2,

        // Rooms
        Integer numBedrooms,
        Integer numBathrooms,
        Integer floorLevel,
        Boolean sharedWc,

        // Universal amenities
        Boolean hasParking,
        Boolean hasGenerator,
        Boolean hasWaterMeter,
        Boolean fiberInternet,
        Boolean securityGate,
        Boolean hasGardien,

        // Commercial amenities
        Double roadFrontageM,
        Integer shopfrontQuality,
        Boolean loadingBay,
        Double standbyPowerKva,

        // Proximity
        Boolean nearSchool,
        Boolean nearMarket,
        Boolean nearHospital,
        Boolean nearHighway,
        Boolean nearUniversity,

        // Quality / age / risk
        Integer structuralQuality,
        Integer conditionScore,
        Integer buildYear,
        Boolean floodRisk,
        Integer noiseLevel,

        // Legal / contractual
        TitleType titleType,
        Integer advanceMonths,
        Integer cautionMonths,

        // Pricing
        Double rentXaf,

        // Scoring signals
        Integer landlordReputation,
        Integer leaseSecurity,
        Integer transportScore,

        // Audit
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        // Images — full collection, only populated for single-property detail view
        List<PropertyImageResponseDTO> images

) {
    /**
     * @param images explicitly passed in (mapped from PropertyImage entities
     *               by the service layer) rather than read from p.getImages()
     *               directly, so callers control when the lazy collection is
     *               actually fetched. Pass an empty list for contexts where
     *               images aren't needed.
     */
    public static PropertyResponseDTO fromEntity(Property p, List<PropertyImageResponseDTO> images) {
        return new PropertyResponseDTO(
                p.getId(),
                p.getHost().getId(),
                p.getHost().getFirstName() + " " + p.getHost().getLastName(),
                p.getTitle(),
                p.getDescription(),
                p.getStatus(),
                p.getNeighbourhood(),
                p.getCity(),
                p.getGpsLat(),
                p.getGpsLon(),
                p.getInfraZone(),
                p.getPropertyType(),
                p.getUnitType(),
                p.getLengthM(),
                p.getWidthM(),
                p.getAreaM2(),
                p.getNumBedrooms(),
                p.getNumBathrooms(),
                p.getFloorLevel(),
                p.getSharedWc(),
                p.getHasParking(),
                p.getHasGenerator(),
                p.getHasWaterMeter(),
                p.getFiberInternet(),
                p.getSecurityGate(),
                p.getHasGardien(),
                p.getRoadFrontageM(),
                p.getShopfrontQuality(),
                p.getLoadingBay(),
                p.getStandbyPowerKva(),
                p.getNearSchool(),
                p.getNearMarket(),
                p.getNearHospital(),
                p.getNearHighway(),
                p.getNearUniversity(),
                p.getStructuralQuality(),
                p.getConditionScore(),
                p.getBuildYear(),
                p.getFloodRisk(),
                p.getNoiseLevel(),
                p.getTitleType(),
                p.getAdvanceMonths(),
                p.getCautionMonths(),
                p.getRentXaf(),
                p.getLandlordReputation(),
                p.getLeaseSecurity(),
                p.getTransportScore(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                images
        );
    }
}