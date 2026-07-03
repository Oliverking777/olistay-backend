package olistay.backend.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Payload for POST /appointments — tenant requests a property visit.
 * propertyId is in the request body rather than the URL path since the
 * tenant is creating an appointment, not acting on an existing one.
 */
public record AppointmentRequestDTO(

        @NotNull(message = "Property ID is required")
        Long propertyId,

        @NotNull(message = "Preferred visit date is required")
        @Future(message = "Scheduled date must be in the future")
        LocalDate scheduledDate,

        /**
         * Optional preferred time — host may confirm a different time
         * if the slot isn't available.
         */
        LocalTime scheduledTime,

        @Size(max = 1000, message = "Message cannot exceed 1000 characters")
        String message

) {}
