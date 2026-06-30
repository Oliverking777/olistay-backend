package olistay.backend.service.impl;

import lombok.RequiredArgsConstructor;
import olistay.backend.dto.BecomeHostRequestDTO;
import olistay.backend.dto.HostProfileResponseDTO;
import olistay.backend.entity.HostProfile;
import olistay.backend.entity.User;
import olistay.backend.enums.Role;
import olistay.backend.exception.AlreadyHostException;
import olistay.backend.exception.DuplicateNationalIdException;
import olistay.backend.exception.ResourceNotFoundException;
import olistay.backend.repository.HostProfileRepository;
import olistay.backend.repository.UserRepository;
import olistay.backend.service.HostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HostServiceImpl implements HostService {

    private final UserRepository userRepository;
    private final HostProfileRepository hostProfileRepository;

    @Override
    @Transactional
    public HostProfileResponseDTO becomeHost(String currentUserEmail, BecomeHostRequestDTO request) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Guard: already HOST or ADMIN — no need to go through this flow again.
        if (user.getRole() != Role.GUEST) {
            throw new AlreadyHostException(
                    "Your account already has " + user.getRole().name() + " privileges"
            );
        }

        // Guard: national ID already tied to another account.
        if (hostProfileRepository.existsByNationalIdNumber(request.nationalIdNumber())) {
            throw new DuplicateNationalIdException(
                    "This national ID number is already registered to another account"
            );
        }

        // Promote the user role first, then persist the host profile.
        // Both happen within the same transaction — if either fails, neither is committed.
        user.setRole(Role.HOST);
        userRepository.save(user);

        HostProfile hostProfile = HostProfile.builder()
                .user(user)
                .nationalIdNumber(request.nationalIdNumber())
                .cityOfOperation(request.cityOfOperation())
                .intendedPropertyCount(request.intendedPropertyCount())
                .build();

        HostProfile savedProfile = hostProfileRepository.save(hostProfile);

        return HostProfileResponseDTO.fromEntity(savedProfile);
    }
}
