package olistay.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import olistay.backend.enums.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A tenant's request to visit a property, accepted or declined by the HOST.
 *
 * Flow: tenant creates (PENDING) → host confirms or cancels.
 * Either party may cancel at any point while status is PENDING or CONFIRMED.
 *
 * host is stored directly (not derived via property.getHost() on every
 * query) so that host-side "my appointment requests" queries are a single
 * indexed FK lookup rather than a join through properties.
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Parties ──────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    // ── Scheduling ───────────────────────────────────────────────────────────

    /**
     * The date the tenant wants to visit — date only, no timezone
     * assumption (both parties are in Cameroon; WAT is implicit).
     */
    @Column(nullable = false)
    private LocalDate scheduledDate;

    /**
     * Preferred visit time — optional at request time; the host may
     * request a specific time when confirming (not modelled in this
     * 3-state flow — kept as an optional hint from the tenant).
     */
    private LocalTime scheduledTime;

    // ── Content ───────────────────────────────────────────────────────────────

    /**
     * Optional message from the tenant to the host at request time —
     * e.g. "I'll be coming with my family, please allow 30 minutes."
     */
    @Column(length = 1000)
    private String message;

    /**
     * Reason for cancellation — written by whichever party cancels.
     * Null while PENDING or CONFIRMED.
     */
    @Column(length = 1000)
    private String cancellationReason;

    // ── Status ────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING;

    // ── Audit ────────────────────────────────────────────────────────────────

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}