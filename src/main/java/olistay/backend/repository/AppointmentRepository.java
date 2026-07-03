package olistay.backend.repository;

import olistay.backend.entity.Appointment;
import olistay.backend.entity.Property;
import olistay.backend.entity.User;
import olistay.backend.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // ── Tenant queries ────────────────────────────────────────────────────────

    /**
     * All appointments the tenant has ever made, paginated, newest first.
     */
    Page<Appointment> findAllByTenantOrderByCreatedAtDesc(User tenant, Pageable pageable);

    /**
     * Tenant's appointments filtered by status — e.g. all PENDING requests.
     */
    List<Appointment> findAllByTenantAndStatus(User tenant, AppointmentStatus status);

    /**
     * Guard: prevent the same tenant from making multiple PENDING or
     * CONFIRMED requests for the same property simultaneously.
     */
    boolean existsByTenantAndPropertyAndStatusIn(
            User tenant,
            Property property,
            List<AppointmentStatus> statuses
    );

    // ── Host queries ──────────────────────────────────────────────────────────

    /**
     * All appointment requests across all the host's properties, paginated.
     */
    Page<Appointment> findAllByHostOrderByCreatedAtDesc(User host, Pageable pageable);

    /**
     * Host's incoming requests filtered by status — e.g. all PENDING
     * requests that need action.
     */
    List<Appointment> findAllByHostAndStatus(User host, AppointmentStatus status);

    /**
     * All appointments for a specific property — useful for the host's
     * per-listing view.
     */
    Page<Appointment> findAllByPropertyOrderByScheduledDateAscCreatedAtDesc(
            Property property, Pageable pageable
    );

    // ── Shared query ──────────────────────────────────────────────────────────

    /**
     * Count active (non-cancelled) appointments for a property — informational
     * for the host dashboard.
     */
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.property = :property
              AND a.status != olistay.backend.enums.AppointmentStatus.CANCELLED
            """)
    long countActiveByProperty(@Param("property") Property property);
}