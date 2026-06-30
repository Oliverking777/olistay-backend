package olistay.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to PropertyData in scoring/optimality.py — a DIFFERENT shape from
 * PropertyMlFeaturesDTO (rent_predictor's shape uses length_m/width_m;
 * this one uses size_m2 directly, and includes scoring-specific fields like
 * landlord_reputation that rent_predictor doesn't need). Kept as its own
 * record rather than reusing PropertyMlFeaturesDTO to avoid silently
 * breaking one endpoint's contract while fixing the other's.
 */
public record ScoringPropertyDataDTO(

        @JsonProperty("property_id")
        String propertyId,

        @JsonProperty("rent")
        Double rent,

        @JsonProperty("neighbourhood")
        String neighbourhood,

        @JsonProperty("unit_type")
        String unitType,

        @JsonProperty("infra_zone")
        String infraZone,

        @JsonProperty("num_bedrooms")
        Integer numBedrooms,

        @JsonProperty("num_bathrooms")
        Integer numBathrooms,

        @JsonProperty("shared_wc")
        Boolean sharedWc,

        @JsonProperty("size_m2")
        Double sizeM2,

        @JsonProperty("has_generator")
        Boolean hasGenerator,

        @JsonProperty("has_parking")
        Boolean hasParking,

        @JsonProperty("has_water_meter")
        Boolean hasWaterMeter,

        @JsonProperty("near_school")
        Boolean nearSchool,

        @JsonProperty("near_market")
        Boolean nearMarket,

        @JsonProperty("near_hospital")
        Boolean nearHospital,

        @JsonProperty("flood_risk")
        Boolean floodRisk,

        @JsonProperty("structural_quality")
        Integer structuralQuality,

        @JsonProperty("noise_level")
        Integer noiseLevel,

        @JsonProperty("landlord_reputation")
        Integer landlordReputation,

        @JsonProperty("lease_security")
        Integer leaseSecurity,

        @JsonProperty("title_type")
        String titleType,

        @JsonProperty("advance_months")
        Integer advanceMonths,

        @JsonProperty("caution_months")
        Integer cautionMonths,

        @JsonProperty("transport_score")
        Integer transportScore,

        @JsonProperty("floor_level")
        Integer floorLevel,

        @JsonProperty("has_gardien")
        Boolean hasGardien

) {
    /**
     * Builds a scoring request property from the ML feature DTO. Note the
     * field-name divergence handled here: rent_predictor's area_m2 maps to
     * this DTO's size_m2 (PropertyData reads prop.size_m2, not area_m2).
     */
    public static ScoringPropertyDataDTO fromMlFeatures(PropertyMlFeaturesDTO f) {
        return new ScoringPropertyDataDTO(
                f.propertyId(),
                f.rent(),
                f.neighbourhood(),
                f.unitType(),
                f.infraZone(),
                f.numBedrooms(),
                f.numBathrooms(),
                f.sharedWc(),
                f.sizeM2(),
                f.hasGenerator(),
                f.hasParking(),
                f.hasWaterMeter(),
                f.nearSchool(),
                f.nearMarket(),
                f.nearHospital(),
                f.floodRisk(),
                f.structuralQuality(),
                f.noiseLevel(),
                f.landlordReputation(),
                f.leaseSecurity(),
                f.titleType(),
                f.advanceMonths(),
                f.cautionMonths(),
                f.transportScore(),
                f.floorLevel(),
                f.hasGardien()
        );
    }
}