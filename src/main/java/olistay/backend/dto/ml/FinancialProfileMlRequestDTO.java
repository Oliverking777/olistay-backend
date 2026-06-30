package olistay.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Maps to TenantProfileRequest in financial/profiler.py.
 * POST /financial/profile.
 */
public record FinancialProfileMlRequestDTO(

        @JsonProperty("tenant_id")
        String tenantId,

        @JsonProperty("monthly_income")
        Double monthlyIncome,

        @JsonProperty("savings_goal")
        Double savingsGoal,

        @JsonProperty("additional_income_sources")
        List<IncomeSourceMlDTO> additionalIncomeSources,

        @JsonProperty("income_stability")
        String incomeStability,

        @JsonProperty("job_sector")
        String jobSector,

        @JsonProperty("employer_name")
        String employerName,

        @JsonProperty("job_title")
        String jobTitle,

        @JsonProperty("current_city")
        String currentCity,

        @JsonProperty("current_neighbourhood")
        String currentNeighbourhood,

        @JsonProperty("gps_lat")
        Double gpsLat,

        @JsonProperty("gps_lon")
        Double gpsLon,

        @JsonProperty("household_size")
        Integer householdSize,

        @JsonProperty("has_dependents")
        Boolean hasDependents,

        @JsonProperty("num_dependents")
        Integer numDependents,

        @JsonProperty("num_roommates")
        Integer numRoommates,

        @JsonProperty("shares_housing_costs")
        Boolean sharesHousingCosts,

        @JsonProperty("fixed_obligations")
        Double fixedObligations,

        @JsonProperty("expense_breakdown")
        ExpenseBreakdownMlDTO expenseBreakdown,

        @JsonProperty("goal_timeline_months")
        Integer goalTimelineMonths,

        @JsonProperty("current_savings")
        Double currentSavings,

        @JsonProperty("available_funds_breakdown")
        AvailableFundsBreakdownMlDTO availableFundsBreakdown,

        @JsonProperty("has_financial_emergency")
        Boolean hasFinancialEmergency

) {
    public record IncomeSourceMlDTO(
            @JsonProperty("income_type") String incomeType,
            @JsonProperty("description") String description,
            @JsonProperty("monthly_amount") Double monthlyAmount
    ) {}

    public record ExpenseBreakdownMlDTO(
            @JsonProperty("housing_utilities") Double housingUtilities,
            @JsonProperty("food_household_supplies") Double foodHouseholdSupplies,
            @JsonProperty("transportation") Double transportation,
            @JsonProperty("personal_health_insurance") Double personalHealthInsurance,
            @JsonProperty("debt_repayments") Double debtRepayments,
            @JsonProperty("dependents_support") Double dependentsSupport,
            @JsonProperty("other") Double other
    ) {}

    public record AvailableFundsBreakdownMlDTO(
            @JsonProperty("checking_account") Double checkingAccount,
            @JsonProperty("savings_account") Double savingsAccount,
            @JsonProperty("cash_on_hand") Double cashOnHand,
            @JsonProperty("mobile_money") Double mobileMoney,
            @JsonProperty("other") Double other
    ) {}
}
