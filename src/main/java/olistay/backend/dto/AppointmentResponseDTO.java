package olistay.backend.dto;

import olistay.backend.entity.Appointment;
import olistay.backend.enums.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Returned from every appointment endpoint. Contains enough context for
 * both the tenant view (my appointments) and the host view (incoming
 * requests) without exposing full nested user/property objects — callers
 * that need full property detail can call GET /properties/{id} separately.
 */
public record AppointmentResponseDTO(

        Long id,

        // ── Property context ─────────────────────────────────────────────────
        Long propertyId,
        String propertyTitle,
        String propertyCity,
        String propertyNeighbourhood,

        // ── Tenant context ───────────────────────────────────────────────────
        Long tenantId,
        String tenantName,
        String tenantEmail,

        // ── Host context ─────────────────────────────────────────────────────
        Long hostId,
        String hostName,
        String hostEmail,

        // ── Scheduling ───────────────────────────────────────────────────────
        LocalDate scheduledDate,
        LocalTime scheduledTime,

        // ── Content ──────────────────────────────────────────────────────────
        String message,
        String cancellationReason,

        // ── Status ───────────────────────────────────────────────────────────
        AppointmentStatus status,

        // ── Audit ────────────────────────────────────────────────────────────
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
    public static AppointmentResponseDTO fromEntity(Appointment a) {
        return new AppointmentResponseDTO(
                a.getId(),

                a.getProperty().getId(),
                a.getProperty().getTitle(),
                a.getProperty().getCity(),
                a.getProperty().getNeighbourhood(),

                a.getTenant().getId(),
                a.getTenant().getFirstName() + " " + a.getTenant().getLastName(),
                a.getTenant().getEmail(),

                a.getHost().getId(),
                a.getHost().getFirstName() + " " + a.getHost().getLastName(),
                a.getHost().getEmail(),

                a.getScheduledDate(),
                a.getScheduledTime(),
                a.getMessage(),
                a.getCancellationReason(),
                a.getStatus(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
