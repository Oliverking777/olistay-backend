package olistay.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Payload for PATCH /users/me/become-host.
 *
 * Collects the minimum information needed to qualify a GUEST for HOST
 * promotion in the Cameroonian rental market context. This is not a full
 * KYC flow — it establishes intent and basic identity so the platform has
 * something to reference if a host's listings are disputed.
 */
public record BecomeHostRequestDTO(

        @NotBlank(message = "National ID number is required")
        @Pattern(
                regexp = "^[A-Z0-9]{6,20}$",
                message = "National ID must be 6–20 alphanumeric characters"
        )
        String nationalIdNumber,

        @NotBlank(message = "City of operation is required")
        String cityOfOperation,

        @NotNull(message = "Intended property count is required")
        @Min(value = 1, message = "You must intend to list at least one property")
        Integer intendedPropertyCount

) {}
