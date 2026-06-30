package olistay.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a single refresh token issued to a user, stored server-side so that
 * rotation and theft-detection can be enforced.
 *
 * Fields beyond the bare minimum (token, user, expiry) exist specifically to support
 * the grace-window fix for the double-refresh race condition:
 *   - {@code used}            marks a token as already consumed once rotated.
 *   - {@code usedAt}          when it was consumed; the grace window is measured from here.
 *   - {@code replacedByToken} points to the token that superseded this one, so a
 *                              near-simultaneous second request within the grace
 *                              window can be detected as a retry rather than theft.
 *   - {@code revoked}         hard-invalidates a token (e.g. on logout or detected theft).
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    private LocalDateTime usedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "replaced_by_token", length = 512)
    private String replacedByToken;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}