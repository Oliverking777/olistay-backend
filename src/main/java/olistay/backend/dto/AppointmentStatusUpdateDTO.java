package olistay.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import olistay.backend.enums.AppointmentStatus;

/**
 * Payload for PATCH /appointments/{id}/status — host confirms or cancels
 * an appointment, or either party cancels.
 *
 * Only CONFIRMED and CANCELLED are valid target statuses via this endpoint
 * (PENDING is the initial state set at creation, never set via this DTO).
 * The service enforces this constraint.
 */
public record AppointmentStatusUpdateDTO(

        @NotNull(message = "Status is required")
        AppointmentStatus status,

        @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
        String cancellationReason

) {}
