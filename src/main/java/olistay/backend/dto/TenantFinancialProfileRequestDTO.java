package olistay.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/**
 * Payload for POST /tenant/financial-profile (create) and
 * PUT /tenant/financial-profile (update).
 *
 * tenant_id is NOT included here — the authenticated user's identity
 * (from JWT) determines whose profile this is, same pattern as
 * UpdateProfileRequestDTO never accepting an email field.
 */
public record TenantFinancialProfileRequestDTO(

        @Positive(message = "Monthly income must be greater than zero")
        Double monthlyIncome,

        @PositiveOrZero(message = "Savings goal cannot be negative")
        Double savingsGoal,

        @Valid
        List<IncomeSourceDTO> additionalIncomeSources,

        String incomeStability,

        String jobSector,
        String employerName,
        String jobTitle,

        String currentCity,
        String currentNeighbourhood,
        Double gpsLat,
        Double gpsLon,

        @Min(value = 1, message = "Household size must be at least 1")
        Integer householdSize,

        Boolean hasDependents,

        @PositiveOrZero
        Integer numDependents,

        @PositiveOrZero
        Integer numRoommates,

        Boolean sharesHousingCosts,

        @PositiveOrZero(message = "Fixed obligations cannot be negative")
        Double fixedObligations,

        @Valid
        ExpenseBreakdownDTO expenseBreakdown,

        @Min(value = 1, message = "Goal timeline must be at least 1 month")
        Integer goalTimelineMonths,

        @PositiveOrZero(message = "Current savings cannot be negative")
        Double currentSavings,

        @Valid
        AvailableFundsBreakdownDTO availableFundsBreakdown,

        Boolean hasFinancialEmergency,

        // ── Housing preferences — affect ranking, not affordability ─────────
        Boolean needsParking,
        Boolean needsSchoolNearby,
        Boolean needsHospitalNearby,
        Boolean needsGenerator

) {}