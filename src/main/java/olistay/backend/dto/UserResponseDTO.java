package olistay.backend.dto;

import olistay.backend.entity.User;
import olistay.backend.enums.Role;

/**
 * Safe, outward-facing representation of a User. Never include the password
 * hash here — this DTO is what gets serialized in API responses.
 */
public record UserResponseDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Role role
) {

    /**
     * Maps an entity to its response DTO. Kept here as a static factory so
     * every call site (AuthService, future UserService, etc.) builds the
     * response the same way instead of re-deriving field mappings ad hoc.
     */
    public static UserResponseDTO fromEntity(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole()
        );
    }
}