package olistay.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import olistay.backend.dto.AppointmentRequestDTO;
import olistay.backend.dto.AppointmentResponseDTO;
import olistay.backend.dto.AppointmentStatusUpdateDTO;
import olistay.backend.service.AppointmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Appointment endpoints. All routes require authentication — appointments
 * are always scoped to the authenticated caller (as tenant or host).
 *
 * Route design:
 *   POST   /appointments                        — tenant creates a request
 *   GET    /appointments/{id}                   — tenant or host views one
 *   GET    /appointments/tenant/me              — tenant's own history
 *   GET    /appointments/host/me                — host's incoming requests
 *   GET    /appointments/property/{id}          — host's per-listing view
 *   PATCH  /appointments/{id}/status            — host confirms; either cancels
 */
@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * POST /appointments
     * Tenant requests a visit for a property.
     * 409 if the tenant already has an active request for the same property.
     * 422 if the property is not AVAILABLE.
     * 403 if the caller is the property's own host.
     */
    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> requestAppointment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AppointmentRequestDTO request
    ) {
        AppointmentResponseDTO created = appointmentService.requestAppointment(
                userDetails.getUsername(), request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /appointments/{id}
     * Returns a single appointment. Caller must be either the tenant or
     * host on that appointment — 403 otherwise.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> getAppointmentById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                appointmentService.getAppointmentById(userDetails.getUsername(), id)
        );
    }

    /**
     * GET /appointments/tenant/me?page=0&size=10&sort=createdAt,desc
     * The authenticated tenant's full appointment history, paginated.
     */
    @GetMapping("/tenant/me")
    public ResponseEntity<Page<AppointmentResponseDTO>> getMyAppointmentsAsTenant(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                appointmentService.getMyAppointmentsAsTenant(userDetails.getUsername(), pageable)
        );
    }

    /**
     * GET /appointments/host/me?page=0&size=10
     * All visit requests across the authenticated host's properties,
     * paginated. Used to populate the host's "Requests" dashboard.
     */
    @GetMapping("/host/me")
    public ResponseEntity<Page<AppointmentResponseDTO>> getMyAppointmentsAsHost(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                appointmentService.getMyAppointmentsAsHost(userDetails.getUsername(), pageable)
        );
    }

    /**
     * GET /appointments/property/{propertyId}?page=0&size=10
     * All appointments for one specific property. Caller must be the
     * owning host — 403 otherwise.
     */
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<Page<AppointmentResponseDTO>> getAppointmentsForProperty(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long propertyId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                appointmentService.getAppointmentsForProperty(
                        userDetails.getUsername(), propertyId, pageable
                )
        );
    }

    /**
     * PATCH /appointments/{id}/status
     * Host confirms a PENDING appointment (CONFIRMED).
     * Either party cancels a PENDING or CONFIRMED appointment (CANCELLED).
     * Body: { "status": "CONFIRMED" | "CANCELLED", "cancellationReason": "..." }
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentResponseDTO> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusUpdateDTO request
    ) {
        return ResponseEntity.ok(
                appointmentService.updateStatus(userDetails.getUsername(), id, request)
        );
    }
}