package olistay.backend.service;

import olistay.backend.dto.AdminUserResponseDTO;
import olistay.backend.dto.HostProfileAdminResponseDTO;
import olistay.backend.dto.PropertyResponseDTO;
import olistay.backend.dto.PropertySummaryDTO;
import olistay.backend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Admin-only operations. Every method here assumes the caller has already
 * been verified as ADMIN by Spring Security (/admin/** route matcher in
 * SecurityConfig + @PreAuthorize on the controller for belt-and-suspenders).
 */
public interface AdminService {

    // ── Listing moderation ────────────────────────────────────────────────────

    /**
     * Returns all listings pending admin review, paginated.
     */
    Page<PropertySummaryDTO> getPendingListings(Pageable pageable);

    /**
     * Approves a UNDER_REVIEW listing — sets status to AVAILABLE.
     */
    PropertyResponseDTO approveListing(Long propertyId);

    /**
     * Rejects a UNDER_REVIEW listing — sets status to ARCHIVED with a
     * reason that is stored as a note (logged server-side for now; a
     * rejection notification flow can be added later).
     */
    PropertyResponseDTO rejectListing(Long propertyId, String reason);

    /**
     * Forces any listing to ARCHIVED, regardless of its current status.
     * Used when admin removes a listing that violates platform rules.
     */
    PropertyResponseDTO archiveListing(Long propertyId);

    // ── User management ───────────────────────────────────────────────────────

    /**
     * Returns all users, paginated. Optionally filtered by role.
     * Passing null for role returns every user.
     */
    Page<AdminUserResponseDTO> getUsers(Role roleFilter, Pageable pageable);

    /**
     * Returns a single user by ID.
     */
    AdminUserResponseDTO getUserById(Long userId);

    /**
     * Locks a user account (accountNonLocked = false). The user can no
     * longer log in; existing sessions remain valid until the access token
     * expires (short-lived by design). For immediate lock-out, combine with
     * revokeAllSessions().
     */
    AdminUserResponseDTO lockUser(Long userId);

    /**
     * Unlocks a previously locked user account.
     */
    AdminUserResponseDTO unlockUser(Long userId);

    /**
     * Disables a user account entirely (enabled = false). Stronger than
     * lock — Spring Security's isEnabled() check fires before
     * isAccountNonLocked().
     */
    AdminUserResponseDTO disableUser(Long userId);

    /**
     * Re-enables a disabled user account.
     */
    AdminUserResponseDTO enableUser(Long userId);

    /**
     * Revokes ALL refresh tokens for a user — forces all open sessions to
     * expire at their next refresh cycle. Use after locking/disabling for
     * immediate effect.
     */
    void revokeAllSessions(Long userId);

    // ── Host management ───────────────────────────────────────────────────────

    /**
     * Returns all host profiles, paginated — the admin host management view.
     */
    Page<HostProfileAdminResponseDTO> getHosts(Pageable pageable);

    /**
     * Returns a single host profile by user ID.
     */
    HostProfileAdminResponseDTO getHostByUserId(Long userId);

    /**
     * Demotes a HOST back to GUEST role and revokes all their sessions.
     * The HostProfile row is retained for audit purposes — the host profile
     * is NOT deleted, only the role is downgraded.
     */
    AdminUserResponseDTO demoteHost(Long userId);
}