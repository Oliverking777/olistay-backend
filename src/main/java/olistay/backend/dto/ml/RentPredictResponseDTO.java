package olistay.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Maps to RentPredictionResponse in ml_models/rent_predictor.py.
 * @JsonIgnoreProperties(ignoreUnknown = true) — defensive: if the Python
 * side adds a field later (it has, multiple times, across this project's
 * history), deserialization shouldn't break the Spring side.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RentPredictResponseDTO(

        @JsonProperty("predicted_rent")
        Double predictedRent,

        @JsonProperty("rent_range_min")
        Double rentRangeMin,

        @JsonProperty("rent_range_max")
        Double rentRangeMax,

        @JsonProperty("area_m2")
        Double areaM2,

        @JsonProperty("property_type")
        String propertyType,

        @JsonProperty("neighbourhood")
        String neighbourhood,

        @JsonProperty("neighbourhood_known")
        Boolean neighbourhoodKnown,

        @JsonProperty("model_confidence")
        String modelConfidence,

        @JsonProperty("r2_score")
        Double r2Score,

        @JsonProperty("cv_mae_cfa")
        Double cvMaeCfa,

        @JsonProperty("narration")
        String narration,

        @JsonProperty("top_drivers")
        Map<String, Object> topDrivers

) {}
