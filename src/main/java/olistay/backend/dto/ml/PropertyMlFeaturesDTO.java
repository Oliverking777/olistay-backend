package olistay.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import olistay.backend.entity.Property;

/**
 * Serializes a Property as the snake_case JSON shape the FastAPI ML engine
 * expects.
 *
 * WHY THIS DTO EXISTS:
 * Jackson serializes Java fields as camelCase by default (numBedrooms),
 * but every Python file in the ML engine (rent_predictor.py,
 * content_based.py, hidden_costs.py, occupancy_forecaster.py) reads
 * dict keys in snake_case (num_bedrooms) via prop.get("num_bedrooms").
 * Without explicit @JsonProperty annotations, every property field sent
 * to FastAPI would silently fail to match — Python's .get(key, default)
 * would always fall through to the default value instead of raising an
 * error, making the bug invisible until predictions looked suspiciously
 * generic.
 *
 * This DTO is the ONLY place in the codebase that should ever serialize a
 * Property toward the ML engine. PropertyResponseDTO (camelCase) is for
 * the frontend; this one is for FastAPI. Keep them separate — collapsing
 * them into one DTO would force a choice between breaking the frontend
 * contract or breaking the ML contract.
 *
 * Field names below are copied verbatim from the Python prop.get(...) calls
 * across content_based.py, rent_predictor.py, hidden_costs.py, and
 * occupancy_forecaster.py — see each @JsonProperty value's matching source
 * file in the inline comment.
 */
public record PropertyMlFeaturesDTO(

        @JsonProperty("property_id")
        String propertyId,

        // ── occupancy_forecaster.py + optimality.py: unit_type ──────────────
        // (chambre / T1 / T2 / T3 / T4 / T5) — distinct from property_type.
        @JsonProperty("unit_type")
        String unitType,

        // ── rent_predictor.py: property_type ────────────────────────────────
        @JsonProperty("property_type")
        String propertyType,

        // ── rent_predictor.py: neighbourhood, city ──────────────────────────
        @JsonProperty("neighbourhood")
        String neighbourhood,

        @JsonProperty("city")
        String city,

        @JsonProperty("gps_lat")
        Double gpsLat,

        @JsonProperty("gps_lon")
        Double gpsLon,

        // ── rent_predictor.py + content_based.py: infra_zone ────────────────
        @JsonProperty("infra_zone")
        String infraZone,

        // ── rent_predictor.py: length_m, width_m, area_m2 ───────────────────
        @JsonProperty("length_m")
        Double lengthM,

        @JsonProperty("width_m")
        Double widthM,

        @JsonProperty("area_m2")
        Double areaM2,

        // ── rent_predictor.py + content_based.py: size_m2 alias ─────────────
        // content_based.py reads prop.get("size_m2", 50) specifically —
        // a DIFFERENT key from area_m2 used by rent_predictor.py. Both are
        // sent, both point at the same value, to satisfy both call sites
        // without forcing a Python-side rename.
        @JsonProperty("size_m2")
        Double sizeM2,

        // ── rent_predictor.py: num_bedrooms, num_bathrooms, floor_level, shared_wc
        @JsonProperty("num_bedrooms")
        Integer numBedrooms,

        @JsonProperty("num_bathrooms")
        Integer numBathrooms,

        @JsonProperty("floor_level")
        Integer floorLevel,

        @JsonProperty("shared_wc")
        Boolean sharedWc,

        // ── rent_predictor.py: has_parking, has_generator, has_water_meter,
        //    fiber_internet, security_gate ────────────────────────────────────
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

        // ── hidden_costs.py: has_gardien (not has_generator — distinct field)
        @JsonProperty("has_gardien")
        Boolean hasGardien,

        // ── rent_predictor.py: road_frontage_m, shopfront_quality, loading_bay,
        //    standby_power_kva (commercial properties only) ───────────────────
        @JsonProperty("road_frontage_m")
        Double roadFrontageM,

        @JsonProperty("shopfront_quality")
        Integer shopfrontQuality,

        @JsonProperty("loading_bay")
        Boolean loadingBay,

        @JsonProperty("standby_power_kva")
        Double standbyPowerKva,

        // ── rent_predictor.py + content_based.py: near_school, near_market,
        //    near_hospital, near_highway, near_university ─────────────────────
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

        // ── rent_predictor.py + content_based.py: structural_quality,
        //    condition_score, build_year, flood_risk, noise_level ─────────────
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

        // ── rent_predictor.py + content_based.py: title_type ────────────────
        @JsonProperty("title_type")
        String titleType,

        // ── rent_predictor.py + content_based.py + hidden_costs.py: advance_months
        @JsonProperty("advance_months")
        Integer advanceMonths,

        // ── hidden_costs.py: caution_months ──────────────────────────────────
        @JsonProperty("caution_months")
        Integer cautionMonths,

        // ── rent_predictor.py training target / content_based.py rent ───────
        @JsonProperty("rent")
        Double rent,

        // ── content_based.py: landlord_reputation, lease_security ───────────
        @JsonProperty("landlord_reputation")
        Integer landlordReputation,

        @JsonProperty("lease_security")
        Integer leaseSecurity,

        // ── content_based.py: transport_score ────────────────────────────────
        @JsonProperty("transport_score")
        Integer transportScore

) {
        /**
         * Maps a Property entity to the ML-engine JSON shape.
         * Enum fields are serialized via .name() — Python's ordinal encoders
         * (PROPERTY_TYPE_ORDER, INFRA_ZONE_ORDER, TITLE_TYPE_ORDER) expect the
         * exact uppercase enum names for property_type, and the exact
         * "I"/"II"/.../"V" and "none"/"occupation"/"foncier" string forms for
         * infra_zone and title_type respectively — see TitleType/InfraZone
         * enum javadoc for the lowercase mapping rule applied below.
         */
        public static PropertyMlFeaturesDTO fromEntity(Property p) {
                return new PropertyMlFeaturesDTO(
                        String.valueOf(p.getId()),
                        p.getUnitType(),
                        // PropertyType.APARTMENT → "apartment" — matches PROPERTY_TYPE_ORDER
                        // (lowercase list) exactly; Python does .lower() on read too, but
                        // sending it pre-lowercased avoids relying on every call site
                        // remembering to normalize case.
                        p.getPropertyType() != null ? p.getPropertyType().name().toLowerCase() : null,
                        p.getNeighbourhood(),
                        p.getCity(),
                        p.getGpsLat(),
                        p.getGpsLon(),
                        p.getInfraZone() != null ? p.getInfraZone().name() : null,
                        p.getLengthM(),
                        p.getWidthM(),
                        p.getAreaM2(),
                        p.getAreaM2(), // size_m2 alias — same value, different key for content_based.py
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
                        // TitleType.NONE → "none", OCCUPATION → "occupation", FONCIER → "foncier"
                        p.getTitleType() != null ? p.getTitleType().name().toLowerCase() : null,
                        p.getAdvanceMonths(),
                        p.getCautionMonths(),
                        p.getRentXaf(),
                        p.getLandlordReputation(),
                        p.getLeaseSecurity(),
                        p.getTransportScore()
                );
        }
}