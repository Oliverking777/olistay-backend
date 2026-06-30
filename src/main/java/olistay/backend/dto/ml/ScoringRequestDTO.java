package olistay.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to ScoringRequest in scoring/optimality.py. POST /scoring/score.
 */
public record ScoringRequestDTO(

        @JsonProperty("tenant")
        ScoringTenantDataDTO tenant,

        @JsonProperty("property")
        ScoringPropertyDataDTO property

) {}