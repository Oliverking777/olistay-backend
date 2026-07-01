package olistay.backend.dto;

import olistay.backend.entity.HostProfile;

import java.time.LocalDateTime;

/**
 * Admin view of a HostProfile — includes the national ID and promotion date
 * that the public HostProfileResponseDTO (returned after "Become a Landlord")
 * also carries, but here used in the admin context for host management.
 */
public record HostProfileAdminResponseDTO(
        Long id,
        Long userId,
        String hostName,
        String email,
        String nationalIdNumber,
        String cityOfOperation,
        Integer intendedPropertyCount,
        LocalDateTime promotedAt
) {
    public static HostProfileAdminResponseDTO fromEntity(HostProfile hp) {
        return new HostProfileAdminResponseDTO(
                hp.getId(),
                hp.getUser().getId(),
                hp.getUser().getFirstName() + " " + hp.getUser().getLastName(),
                hp.getUser().getEmail(),
                hp.getNationalIdNumber(),
                hp.getCityOfOperation(),
                hp.getIntendedPropertyCount(),
                hp.getPromotedAt()
        );
    }
}