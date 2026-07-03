package olistay.backend.service.impl;

import lombok.RequiredArgsConstructor;
import olistay.backend.dto.AppointmentRequestDTO;
import olistay.backend.dto.AppointmentResponseDTO;
import olistay.backend.dto.AppointmentStatusUpdateDTO;
import olistay.backend.entity.Appointment;
import olistay.backend.entity.Property;
import olistay.backend.entity.User;
import olistay.backend.enums.AppointmentStatus;
import olistay.backend.enums.PropertyStatus;
import olistay.backend.exception.AppointmentAccessException;
import olistay.backend.exception.AppointmentConflictException;
import olistay.backend.exception.InvalidStateException;
import olistay.backend.exception.ResourceNotFoundException;
import olistay.backend.repository.AppointmentRepository;
import olistay.backend.repository.PropertyRepository;
import olistay.backend.repository.UserRepository;
import olistay.backend.service.AppointmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AppointmentResponseDTO requestAppointment(String tenantEmail, AppointmentRequestDTO request) {
        User tenant = findUserOrThrow(tenantEmail);
        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property not found with id: " + request.propertyId()
                ));

        // Only AVAILABLE properties can be visited
        if (property.getStatus() != PropertyStatus.AVAILABLE) {
            throw new InvalidStateException(
                    "This property is not currently available for visit requests"
            );
        }

        // A host cannot book their own listing
        if (property.getHost().getId().equals(tenant.getId())) {
            throw new AppointmentAccessException(
                    "You cannot request a visit for your own property"
            );
        }

        // Prevent duplicate active appointments for the same property
        boolean alreadyActive = appointmentRepository.existsByTenantAndPropertyAndStatusIn(
                tenant, property,
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)
        );
        if (alreadyActive) {
            throw new AppointmentConflictException(
                    "You already have an active appointment request for this property. " +
                            "Cancel the existing one before making a new request."
            );
        }

        User host = property.getHost();

        Appointment appointment = Appointment.builder()
                .tenant(tenant)
                .host(host)
                .property(property)
                .scheduledDate(request.scheduledDate())
                .scheduledTime(request.scheduledTime())
                .message(request.message())
                .status(AppointmentStatus.PENDING)
                .build();

        return AppointmentResponseDTO.fromEntity(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDTO getAppointmentById(String currentUserEmail, Long appointmentId) {
        User caller = findUserOrThrow(currentUserEmail);
        Appointment appointment = findAppointmentOrThrow(appointmentId);
        assertIsParty(caller, appointment);
        return AppointmentResponseDTO.fromEntity(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDTO> getMyAppointmentsAsTenant(String tenantEmail, Pageable pageable) {
        User tenant = findUserOrThrow(tenantEmail);
        return appointmentRepository
                .findAllByTenantOrderByCreatedAtDesc(tenant, pageable)
                .map(AppointmentResponseDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDTO> getMyAppointmentsAsHost(String hostEmail, Pageable pageable) {
        User host = findUserOrThrow(hostEmail);
        return appointmentRepository
                .findAllByHostOrderByCreatedAtDesc(host, pageable)
                .map(AppointmentResponseDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDTO> getAppointmentsForProperty(
            String currentUserEmail, Long propertyId, Pageable pageable
    ) {
        User caller = findUserOrThrow(currentUserEmail);
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Property not found with id: " + propertyId
                ));

        // Only the owning host can see all appointments for a listing
        if (!property.getHost().getId().equals(caller.getId())) {
            throw new AppointmentAccessException(
                    "Only the property owner can view its appointment list"
            );
        }

        return appointmentRepository
                .findAllByPropertyOrderByScheduledDateAscCreatedAtDesc(property, pageable)
                .map(AppointmentResponseDTO::fromEntity);
    }

    @Override
    @Transactional
    public AppointmentResponseDTO updateStatus(
            String currentUserEmail, Long appointmentId, AppointmentStatusUpdateDTO request
    ) {
        User caller = findUserOrThrow(currentUserEmail);
        Appointment appointment = findAppointmentOrThrow(appointmentId);

        // Caller must be a party to this appointment
        assertIsParty(caller, appointment);

        // Cannot change status of an already-cancelled appointment
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new InvalidStateException("This appointment has already been cancelled");
        }

        // PENDING is never a valid target status via this endpoint
        if (request.status() == AppointmentStatus.PENDING) {
            throw new InvalidStateException("PENDING is the initial state and cannot be set explicitly");
        }

        boolean callerIsTenant = appointment.getTenant().getId().equals(caller.getId());
        boolean callerIsHost = appointment.getHost().getId().equals(caller.getId());

        switch (request.status()) {
            case CONFIRMED -> {
                // Only the HOST may confirm
                if (!callerIsHost) {
                    throw new AppointmentAccessException(
                            "Only the host can confirm an appointment request"
                    );
                }
                if (appointment.getStatus() != AppointmentStatus.PENDING) {
                    throw new InvalidStateException(
                            "Only PENDING appointments can be confirmed. " +
                                    "Current status: " + appointment.getStatus()
                    );
                }
                appointment.setStatus(AppointmentStatus.CONFIRMED);
            }
            case CANCELLED -> {
                // Either party may cancel — no further role check needed
                // beyond assertIsParty() above
                appointment.setStatus(AppointmentStatus.CANCELLED);
                appointment.setCancellationReason(request.cancellationReason());
            }
        }

        return AppointmentResponseDTO.fromEntity(appointmentRepository.save(appointment));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Appointment findAppointmentOrThrow(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Appointment not found with id: " + id
                ));
    }

    /**
     * Asserts the caller is either the tenant or the host on this appointment.
     * Prevents a third party (e.g. a different tenant) from reading or
     * modifying someone else's appointment.
     */
    private void assertIsParty(User caller, Appointment appointment) {
        boolean isTenant = appointment.getTenant().getId().equals(caller.getId());
        boolean isHost = appointment.getHost().getId().equals(caller.getId());
        if (!isTenant && !isHost) {
            throw new AppointmentAccessException(
                    "You do not have access to this appointment"
            );
        }
    }
}