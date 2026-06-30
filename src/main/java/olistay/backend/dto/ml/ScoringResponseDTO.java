package olistay.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Maps to ScoringResponse in scoring/optimality.py.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScoringResponseDTO(

        @JsonProperty("tenant_id")
        String tenantId,

        @JsonProperty("property_id")
        String propertyId,

        @JsonProperty("total_score")
        Double totalScore,

        @JsonProperty("grade")
        String grade,

        @JsonProperty("recommendation")
        String recommendation,

        @JsonProperty("category_scores")
        CategoryScoresDTO categoryScores,

        @JsonProperty("weights_used")
        Map<String, Double> weightsUsed,

        @JsonProperty("weight_source")
        String weightSource,

        /**
         * Null when the rent predictor model isn't trained/available yet —
         * optimality.py wraps the ML call in a try/except and returns None
         * on any failure rather than erroring the whole scoring request.
         */
        @JsonProperty("ml_rent_signal")
        Map<String, Object> mlRentSignal,

        @JsonProperty("financial_summary")
        String financialSummary,

        @JsonProperty("tco_summary")
        String tcoSummary,

        @JsonProperty("flags")
        List<String> flags

) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CategoryScoresDTO(

            @JsonProperty("financial")
            Double financial,

            @JsonProperty("goal_alignment")
            Double goalAlignment,

            @JsonProperty("household")
            Double household,

            @JsonProperty("lifestyle")
            Double lifestyle,

            @JsonProperty("safety")
            Double safety,

            @JsonProperty("stability")
            Double stability

    ) {}
}