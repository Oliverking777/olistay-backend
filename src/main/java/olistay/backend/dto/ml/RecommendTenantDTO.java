package olistay.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps to PipelineTenant in recommender/pipeline.py.
 */
public record RecommendTenantDTO(

        @JsonProperty("tenant_id")
        String tenantId,

        @JsonProperty("monthly_income")
        Double monthlyIncome,

        @JsonProperty("fixed_obligations")
        Double fixedObligations,

        @JsonProperty("savings_goal")
        Double savingsGoal,

        @JsonProperty("goal_timeline_months")
        Integer goalTimelineMonths,

        @JsonProperty("household_size")
        Integer householdSize,

        @JsonProperty("current_savings")
        Double currentSavings,

        @JsonProperty("has_dependents")
        Boolean hasDependents,

        @JsonProperty("needs_parking")
        Boolean needsParking,

        @JsonProperty("needs_school_nearby")
        Boolean needsSchoolNearby,

        @JsonProperty("needs_hospital_nearby")
        Boolean needsHospitalNearby,

        @JsonProperty("needs_generator")
        Boolean needsGenerator,

        @JsonProperty("current_neighbourhood")
        String currentNeighbourhood,

        @JsonProperty("current_city")
        String currentCity,

        @JsonProperty("job_sector")
        String jobSector,

        @JsonProperty("income_stability")
        String incomeStability

) {
    /**
     * Convenience: build a RecommendTenantDTO from the same source data used
     * to build ScoringTenantDataDTO, since the two largely overlap. Kept as
     * separate records (not reused) because the Python endpoints they target
     * are independently versioned — pipeline.py's PipelineTenant doesn't
     * carry custom_weights, for instance.
     */
    public static RecommendTenantDTO fromScoringTenant(ScoringTenantDataDTO t) {
        return new RecommendTenantDTO(
                t.tenantId(),
                t.monthlyIncome(),
                t.fixedObligations(),
                t.savingsGoal(),
                t.goalTimelineMonths(),
                t.householdSize(),
                t.currentSavings(),
                t.hasDependents(),
                t.needsParking(),
                t.needsSchoolNearby(),
                t.needsHospitalNearby(),
                t.needsGenerator(),
                t.currentNeighbourhood(),
                t.currentCity(),
                t.jobSector(),
                t.incomeStability()
        );
    }
}