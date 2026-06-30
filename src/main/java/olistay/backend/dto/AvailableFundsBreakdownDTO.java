package olistay.backend.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record AvailableFundsBreakdownDTO(
        @PositiveOrZero Double checkingAccount,
        @PositiveOrZero Double savingsAccount,
        @PositiveOrZero Double cashOnHand,
        @PositiveOrZero Double mobileMoney,
        @PositiveOrZero Double other
) {}
