package olistay.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Maps to RecommendRequest in recommender/pipeline.py. POST /recommender/recommend.
 *
 * candidateProperties reuses PropertyMlFeaturesDTO — pipeline.py forwards
 * candidate_properties: List[dict] straight into content_based.py's
 * rank_properties_content_based(), which reads the exact same keys
 * (size_m2, rent, near_market, etc.) that PropertyMlFeaturesDTO already
 * serializes. No separate DTO needed here.
 */
public record RecommendRequestDTO(

        @JsonProperty("tenant")
        RecommendTenantDTO tenant,

        @JsonProperty("candidate_properties")
        List<PropertyMlFeaturesDTO> candidateProperties,

        @JsonProperty("top_n")
        Integer topN

) {
    public static RecommendRequestDTO of(
            RecommendTenantDTO tenant,
            List<PropertyMlFeaturesDTO> candidates,
            int topN
    ) {
        return new RecommendRequestDTO(tenant, candidates, topN);
    }
}
