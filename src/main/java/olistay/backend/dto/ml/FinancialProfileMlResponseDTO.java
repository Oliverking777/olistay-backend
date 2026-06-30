package olistay.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Maps to FinancialProfileResponse in financial/profiler.py — the live
 * computed result. Never persisted in Spring; always fetched fresh from
 * FastAPI since the underlying calculation logic (Cameroonian affordability
 * rules, emergency fund sizing, etc.) can change independently of the
 * Spring schema.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinancialProfileMlResponseDTO(

        @JsonProperty("tenant_id")
        String tenantId,

        @JsonProperty("effective_monthly_income")
        Double effectiveMonthlyIncome,

        @JsonProperty("effective_fixed_obligations")
        Double effectiveFixedObligations,

        @JsonProperty("effective_current_savings")
        Double effectiveCurrentSavings,

        @JsonProperty("income_sources_count")
        Integer incomeSourcesCount,

        @JsonProperty("expense_breakdown_used")
        Boolean expenseBreakdownUsed,

        @JsonProperty("funds_breakdown_used")
        Boolean fundsBreakdownUsed,

        @JsonProperty("emergency_fund_months_used")
        Integer emergencyFundMonthsUsed,

        @JsonProperty("disposable_income")
        Double disposableIncome,

        @JsonProperty("monthly_savings_required")
        Double monthlySavingsRequired,

        @JsonProperty("true_disposable")
        Double trueDisposable,

        @JsonProperty("max_sustainable_rent")
        Double maxSustainableRent,

        @JsonProperty("recommended_rent_range_min")
        Double recommendedRentRangeMin,

        @JsonProperty("recommended_rent_range_max")
        Double recommendedRentRangeMax,

        @JsonProperty("emergency_fund_target")
        Double emergencyFundTarget,

        @JsonProperty("emergency_fund_status")
        String emergencyFundStatus,

        @JsonProperty("months_to_emergency_fund")
        Double monthsToEmergencyFund,

        @JsonProperty("typical_advance_amount")
        Double typicalAdvanceAmount,

        @JsonProperty("can_afford_typical_advance")
        Boolean canAffordTypicalAdvance,

        @JsonProperty("advance_shortfall")
        Double advanceShortfall,

        @JsonProperty("financial_health")
        String financialHealth,

        @JsonProperty("can_afford_advance")
        Boolean canAffordAdvance,

        @JsonProperty("advance_burden")
        String advanceBurden,

        @JsonProperty("job_sector")
        String jobSector,

        @JsonProperty("current_city")
        String currentCity,

        @JsonProperty("current_neighbourhood")
        String currentNeighbourhood,

        @JsonProperty("profile_completeness_pct")
        Double profileCompletenessPct,

        @JsonProperty("profile_confidence")
        String profileConfidence,

        @JsonProperty("data_quality_notes")
        List<String> dataQualityNotes,

        @JsonProperty("summary")
        String summary

) {}