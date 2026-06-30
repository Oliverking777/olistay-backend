package olistay.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to RentPredictionRequest in ml_models/rent_predictor.py.
 * Used for POST /ml/predict-rent — a planning tool (no property_id needed,
 * since this can be called before a listing is even saved, e.g. while a
 * HOST is still filling out the create-property form).
 */
public record RentPredictRequestDTO(

        @JsonProperty("property_type")
        String propertyType,

        @JsonProperty("neighbourhood")
        String neighbourhood,

        @JsonProperty("city")
        String city,

        @JsonProperty("gps_lat")
        Double gpsLat,

        @JsonProperty("gps_lon")
        Double gpsLon,

        @JsonProperty("infra_zone")
        String infraZone,

        @JsonProperty("length_m")
        Double lengthM,

        @JsonProperty("width_m")
        Double widthM,

        @JsonProperty("num_bedrooms")
        Integer numBedrooms,

        @JsonProperty("num_bathrooms")
        Integer numBathrooms,

        @JsonProperty("floor_level")
        Integer floorLevel,

        @JsonProperty("shared_wc")
        Boolean sharedWc,

        @JsonProperty("has_parking")
        Boolean hasParking,

        @JsonProperty("has_generator")
        Boolean hasGenerator,

        @JsonProperty("has_water_meter")
        Boolean hasWaterMeter,

        @JsonProperty("fiber_internet")
        Boolean fiberInternet,

        @JsonProperty("security_gate")
        Boolean securityGate,

        @JsonProperty("road_frontage_m")
        Double roadFrontageM,

        @JsonProperty("shopfront_quality")
        Integer shopfrontQuality,

        @JsonProperty("loading_bay")
        Boolean loadingBay,

        @JsonProperty("standby_power_kva")
        Double standbyPowerKva,

        @JsonProperty("near_school")
        Boolean nearSchool,

        @JsonProperty("near_market")
        Boolean nearMarket,

        @JsonProperty("near_hospital")
        Boolean nearHospital,

        @JsonProperty("near_highway")
        Boolean nearHighway,

        @JsonProperty("near_university")
        Boolean nearUniversity,

        @JsonProperty("structural_quality")
        Integer structuralQuality,

        @JsonProperty("condition_score")
        Integer conditionScore,

        @JsonProperty("build_year")
        Integer buildYear,

        @JsonProperty("flood_risk")
        Boolean floodRisk,

        @JsonProperty("noise_level")
        Integer noiseLevel,

        @JsonProperty("title_type")
        String titleType,

        @JsonProperty("advance_months")
        Integer advanceMonths

) {
    /**
     * Builds a rent-prediction request from the ML feature DTO — reuses the
     * exact same field values already on PropertyMlFeaturesDTO (already
     * correctly cased: lowercase property_type/title_type, uppercase
     * infra_zone) so the two requests stay in sync by construction rather
     * than by two independently-maintained mapping functions.
     */
    public static RentPredictRequestDTO fromMlFeatures(PropertyMlFeaturesDTO f) {
        return new RentPredictRequestDTO(
                f.propertyType(),
                f.neighbourhood(),
                f.city(),
                f.gpsLat(),
                f.gpsLon(),
                f.infraZone(),
                f.lengthM(),
                f.widthM(),
                f.numBedrooms(),
                f.numBathrooms(),
                f.floorLevel(),
                f.sharedWc(),
                f.hasParking(),
                f.hasGenerator(),
                f.hasWaterMeter(),
                f.fiberInternet(),
                f.securityGate(),
                f.roadFrontageM(),
                f.shopfrontQuality(),
                f.loadingBay(),
                f.standbyPowerKva(),
                f.nearSchool(),
                f.nearMarket(),
                f.nearHospital(),
                f.nearHighway(),
                f.nearUniversity(),
                f.structuralQuality(),
                f.conditionScore(),
                f.buildYear(),
                f.floodRisk(),
                f.noiseLevel(),
                f.titleType(),
                f.advanceMonths()
        );
    }
}