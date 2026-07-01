package olistay.backend.controller;

import lombok.RequiredArgsConstructor;
import olistay.backend.dto.*;
import olistay.backend.enums.Role;
import olistay.backend.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only endpoints, all under /admin/**.
 *
 * Two layers of protection:
 *   1. SecurityConfig: .requestMatchers("/admin/**").hasRole("ADMIN")
 *      — rejects non-ADMIN requests at the filter chain before they even
 *        reach the controller.
 *   2. @PreAuthorize("hasRole('ADMIN')") on the class — belt-and-suspenders
 *      so the protection is explicit and visible in the controller itself,
 *      not just inferred from routing config.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ── Listing moderation ────────────────────────────────────────────────────

    /**
     * GET /admin/listings/pending?page=0&size=20&sort=createdAt,desc
     * Returns all listings awaiting review, paginated.
     */
    @GetMapping("/listings/pending")
    public ResponseEntity<Page<PropertySummaryDTO>> getPendingListings(Pageable pageable) {
        return ResponseEntity.ok(adminService.getPendingListings(pageable));
    }

    /**
     * POST /admin/listings/{id}/approve
     * Approves a pending listing — sets status to AVAILABLE.
     * 409 if the listing is not currently in UNDER_REVIEW status.
     */
    @PostMapping("/listings/{id}/approve")
    public ResponseEntity<PropertyResponseDTO> approveListing(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveListing(id));
    }

    /**
     * POST /admin/listings/{id}/reject
     * Rejects a pending listing — sets status to ARCHIVED.
     * Body: { "reason": "..." } (optional but encouraged).
     * 409 if the listing is not currently in UNDER_REVIEW status.
     */
    @PostMapping("/listings/{id}/reject")
    public ResponseEntity<PropertyResponseDTO> rejectListing(
            @PathVariable Long id,
            @RequestBody(required = false) RejectionRequestDTO body
    ) {
        String reason = body != null ? body.reason() : null;
        return ResponseEntity.ok(adminService.rejectListing(id, reason));
    }

    /**
     * POST /admin/listings/{id}/archive
     * Forces any listing to ARCHIVED regardless of current status.
     * Used when a listing violates platform rules after it was already live.
     */
    @PostMapping("/listings/{id}/archive")
    public ResponseEntity<PropertyResponseDTO> archiveListing(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.archiveListing(id));
    }

    // ── User management ───────────────────────────────────────────────────────

    /**
     * GET /admin/users?role=GUEST&page=0&size=20
     * Returns all users, paginated. Optional role query param filters by role.
     * No role param returns every user.
     */
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponseDTO>> getUsers(
            @RequestParam(required = false) Role role,
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getUsers(role, pageable));
    }

    /**
     * GET /admin/users/{id}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    /**
     * POST /admin/users/{id}/lock
     * Locks the account — user cannot log in; existing short-lived access
     * tokens remain valid until expiry. Combine with /revoke-sessions for
     * immediate effect.
     */
    @PostMapping("/users/{id}/lock")
    public ResponseEntity<AdminUserResponseDTO> lockUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.lockUser(id));
    }

    /**
     * POST /admin/users/{id}/unlock
     */
    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<AdminUserResponseDTO> unlockUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.unlockUser(id));
    }

    /**
     * POST /admin/users/{id}/disable
     * Disables the account entirely. Stronger than lock.
     */
    @PostMapping("/users/{id}/disable")
    public ResponseEntity<AdminUserResponseDTO> disableUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.disableUser(id));
    }

    /**
     * POST /admin/users/{id}/enable
     */
    @PostMapping("/users/{id}/enable")
    public ResponseEntity<AdminUserResponseDTO> enableUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.enableUser(id));
    }

    /**
     * POST /admin/users/{id}/revoke-sessions
     * Revokes all refresh tokens for a user — forces all open sessions to
     * expire at their next refresh cycle. Call after lock/disable for
     * immediate logout effect.
     */
    @PostMapping("/users/{id}/revoke-sessions")
    public ResponseEntity<Void> revokeAllSessions(@PathVariable Long id) {
        adminService.revokeAllSessions(id);
        return ResponseEntity.noContent().build();
    }

    // ── Host management ───────────────────────────────────────────────────────

    /**
     * GET /admin/hosts?page=0&size=20
     * Returns all host profiles, paginated.
     */
    @GetMapping("/hosts")
    public ResponseEntity<Page<HostProfileAdminResponseDTO>> getHosts(Pageable pageable) {
        return ResponseEntity.ok(adminService.getHosts(pageable));
    }

    /**
     * GET /admin/hosts/{userId}
     * Returns the host profile for a specific user by their user ID.
     */
    @GetMapping("/hosts/{userId}")
    public ResponseEntity<HostProfileAdminResponseDTO> getHostByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getHostByUserId(userId));
    }

    /**
     * POST /admin/hosts/{userId}/demote
     * Demotes a HOST back to GUEST role and revokes all their sessions.
     * The HostProfile row is retained for audit — only the role changes.
     */
    @PostMapping("/hosts/{userId}/demote")
    public ResponseEntity<AdminUserResponseDTO> demoteHost(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.demoteHost(userId));
    }
}