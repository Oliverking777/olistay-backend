package olistay.backend.dto;

import olistay.backend.entity.HostProfile;

import java.time.LocalDateTime;

/**
 * Returned from PATCH /users/me/become-host on success.
 * Includes the updated user (now with role=HOST) and the host profile data.
 */
public record HostProfileResponseDTO(
        UserResponseDTO user,
        String cityOfOperation,
        Integer intendedPropertyCount,
        LocalDateTime promotedAt
) {
    public static HostProfileResponseDTO fromEntity(HostProfile profile) {
        return new HostProfileResponseDTO(
                UserResponseDTO.fromEntity(profile.getUser()),
                profile.getCityOfOperation(),
                profile.getIntendedPropertyCount(),
                profile.getPromotedAt()
        );
    }
}
