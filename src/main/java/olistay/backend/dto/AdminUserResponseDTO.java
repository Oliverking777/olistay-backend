package olistay.backend.dto;

import olistay.backend.entity.User;
import olistay.backend.enums.Role;

import java.time.LocalDateTime;

/**
 * Admin-only user view — extends the public UserResponseDTO shape with
 * account status fields (enabled, accountNonLocked) and createdAt that
 * the public API should never expose but admin needs for moderation.
 */
public record AdminUserResponseDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Role role,
        boolean enabled,
        boolean accountNonLocked,
        LocalDateTime createdAt
) {
    public static AdminUserResponseDTO fromEntity(User u) {
        return new AdminUserResponseDTO(
                u.getId(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                u.getPhoneNumber(),
                u.getRole(),
                u.isEnabled(),
                u.isAccountNonLocked(),
                u.getCreatedAt()
        );
    }
}