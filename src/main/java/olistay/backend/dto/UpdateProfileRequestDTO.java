package olistay.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for PATCH /users/me.
 *
 * All fields are optional — only non-null values are applied to the entity
 * (partial update / PATCH semantics). Email and role are intentionally
 * excluded: email changes need their own verification flow, and role
 * promotion happens via the dedicated "Become a Landlord" endpoint, never
 * through a general profile update.
 */
public record UpdateProfileRequestDTO(

        @Size(min = 1, message = "First name cannot be blank")
        String firstName,

        @Size(min = 1, message = "Last name cannot be blank")
        String lastName,

        @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Phone number must be valid")
        String phoneNumber

) {}
