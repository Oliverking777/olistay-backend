package olistay.backend.service;

import olistay.backend.dto.AppointmentRequestDTO;
import olistay.backend.dto.AppointmentResponseDTO;
import olistay.backend.dto.AppointmentStatusUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppointmentService {

    /**
     * Tenant creates a visit request for a property.
     * Throws AppointmentConflictException if the tenant already has a
     * PENDING or CONFIRMED appointment for the same property.
     * Throws ResourceNotFoundException if the property doesn't exist or
     * is not AVAILABLE.
     * Throws AppointmentAccessException if the requesting user is the
     * property's own HOST (a host cannot book their own listing).
     */
    AppointmentResponseDTO requestAppointment(String tenantEmail, AppointmentRequestDTO request);

    /**
     * Returns a single appointment by ID. The caller must be either the
     * tenant or the host on the appointment — throws
     * AppointmentAccessException otherwise.
     */
    AppointmentResponseDTO getAppointmentById(String currentUserEmail, Long appointmentId);

    /**
     * Tenant's own appointment history, paginated, newest first.
     */
    Page<AppointmentResponseDTO> getMyAppointmentsAsTenant(String tenantEmail, Pageable pageable);

    /**
     * All appointment requests across all of the host's properties,
     * paginated, newest first.
     */
    Page<AppointmentResponseDTO> getMyAppointmentsAsHost(String hostEmail, Pageable pageable);

    /**
     * All appointments for a specific property — for the owning host's
     * per-listing view.
     * Throws AppointmentAccessException if the caller is not the property's host.
     */
    Page<AppointmentResponseDTO> getAppointmentsForProperty(
            String currentUserEmail, Long propertyId, Pageable pageable
    );

    /**
     * Updates appointment status. Rules:
     *   - CONFIRMED: only the HOST may confirm a PENDING appointment.
     *   - CANCELLED: either the tenant or the host may cancel a PENDING
     *                or CONFIRMED appointment.
     *   - PENDING: never settable via this endpoint (creation-only state).
     * Throws AppointmentAccessException for rule violations.
     * Throws InvalidStateException if the appointment is already CANCELLED.
     */
    AppointmentResponseDTO updateStatus(
            String currentUserEmail, Long appointmentId, AppointmentStatusUpdateDTO request
    );
}