package olistay.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores landlord-specific qualification data collected during the
 * "Become a Landlord" flow. Kept in a separate table from {@link User}
 * so the users table stays lean — most users will never have a host profile.
 *
 * Created when a GUEST successfully promotes to HOST. If a HOST is ever
 * demoted (admin action), this record is retained for audit purposes.
 */
@Entity
@Table(name = "host_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true)
    private String nationalIdNumber;

    @Column(nullable = false)
    private String cityOfOperation;

    @Column(nullable = false)
    private Integer intendedPropertyCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime promotedAt;

    @PrePersist
    protected void onCreate() {
        this.promotedAt = LocalDateTime.now();
    }
}
