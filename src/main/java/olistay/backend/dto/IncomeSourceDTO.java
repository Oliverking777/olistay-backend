package olistay.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record IncomeSourceDTO(

        @NotBlank(message = "Income type is required")
        String incomeType,

        String description,

        @PositiveOrZero(message = "Monthly amount cannot be negative")
        Double monthlyAmount

) {}
