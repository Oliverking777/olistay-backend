package olistay.backend.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record ExpenseBreakdownDTO(
        @PositiveOrZero Double housingUtilities,
        @PositiveOrZero Double foodHouseholdSupplies,
        @PositiveOrZero Double transportation,
        @PositiveOrZero Double personalHealthInsurance,
        @PositiveOrZero Double debtRepayments,
        @PositiveOrZero Double dependentsSupport,
        @PositiveOrZero Double other
) {}
