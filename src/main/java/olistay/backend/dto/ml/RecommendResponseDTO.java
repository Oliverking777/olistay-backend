package olistay.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Maps to RecommendResponse in recommender/pipeline.py.
 *
 * "recommendations" entries are raw, highly dynamic dicts (each candidate
 * property's original fields plus content_similarity_score,
 * collaborative_score, hybrid_score, feature_contributions, property_vector
 * grafted on). Modeling that exhaustively as a typed record would tightly
 * couple Spring to content_based.py's internal feature engineering, which
 * changes more often than the contract should. Kept as Map<String,Object>
 * deliberately — callers needing specific fields can pull them by key.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendResponseDTO(

        @JsonProperty("tenant_id")
        String tenantId,

        @JsonProperty("stage1_candidates_in")
        Integer stage1CandidatesIn,

        @JsonProperty("stage1_candidates_out")
        Integer stage1CandidatesOut,

        @JsonProperty("stage1_rejected")
        List<Map<String, Object>> stage1Rejected,

        @JsonProperty("pipeline_method")
        String pipelineMethod,

        @JsonProperty("cold_start_active")
        Boolean coldStartActive,

        @JsonProperty("cf_augmentation_weight")
        Double cfAugmentationWeight,

        @JsonProperty("top_n")
        Integer topN,

        @JsonProperty("recommendations")
        List<Map<String, Object>> recommendations

) {}