package olistay.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import olistay.backend.dto.BecomeHostRequestDTO;
import olistay.backend.dto.HostProfileResponseDTO;
import olistay.backend.dto.UpdateProfileRequestDTO;
import olistay.backend.dto.UserResponseDTO;
import olistay.backend.service.HostService;
import olistay.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * User profile endpoints. All routes require authentication — no permitAll()
 * entries for /users/** in SecurityConfig.
 *
 * The authenticated user's email is injected via @AuthenticationPrincipal,
 * which reads from the SecurityContext populated by JwtAuthenticationFilter.
 * The service layer never sees raw SecurityContext calls — email is passed
 * in as a plain String, keeping services testable without a security context.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final HostService hostService;

    /**
     * GET /users/me
     * Returns the authenticated user's own profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(userService.getCurrentUser(userDetails.getUsername()));
    }

    /**
     * PATCH /users/me
     * Partially updates the authenticated user's profile.
     * Only firstName, lastName, and phoneNumber can be changed here.
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponseDTO> updateCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequestDTO request
    ) {
        return ResponseEntity.ok(
                userService.updateCurrentUser(userDetails.getUsername(), request)
        );
    }

    /**
     * GET /users/{id}
     * Returns any user's public profile by ID.
     * Useful for displaying host info on property detail pages.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * DELETE /users/me
     * Permanently deletes the authenticated user's account.
     * All refresh tokens are revoked first to invalidate any open sessions.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.deleteCurrentUser(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /users/me/become-host
     * Promotes the authenticated GUEST to HOST role.
     * Collects national ID, city of operation, and intended property count.
     * Idempotency is enforced in the service layer — calling this twice throws 409.
     */
    @PatchMapping("/me/become-host")
    public ResponseEntity<HostProfileResponseDTO> becomeHost(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BecomeHostRequestDTO request
    ) {
        return ResponseEntity.ok(
                hostService.becomeHost(userDetails.getUsername(), request)
        );
    }
}