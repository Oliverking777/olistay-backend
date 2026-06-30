package olistay.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Maps to TenantData in scoring/optimality.py. Used inside ScoringRequestDTO
 * and BulkScoringRequestDTO.
 */
public record ScoringTenantDataDTO(

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
        String incomeStability,

        /**
         * Optional override — must sum to 1.0 across the six category keys
         * (financial, goal_alignment, household, lifestyle, safety, stability)
         * if provided. Null lets the ML engine use its expert-default or
         * learned weights instead.
         */
        @JsonProperty("custom_weights")
        Map<String, Double> customWeights

) {}