package olistay.backend.service.impl;

import lombok.RequiredArgsConstructor;
import olistay.backend.dto.UpdateProfileRequestDTO;
import olistay.backend.dto.UserResponseDTO;
import olistay.backend.entity.User;
import olistay.backend.exception.ResourceNotFoundException;
import olistay.backend.repository.RefreshTokenRepository;
import olistay.backend.repository.UserRepository;
import olistay.backend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getCurrentUser(String currentUserEmail) {
        return UserResponseDTO.fromEntity(findByEmailOrThrow(currentUserEmail));
    }

    @Override
    @Transactional
    public UserResponseDTO updateCurrentUser(String currentUserEmail, UpdateProfileRequestDTO request) {
        User user = findByEmailOrThrow(currentUserEmail);

        // PATCH semantics: only apply fields that were explicitly provided (non-null).
        // Email and role are intentionally not updatable here.
        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null)  user.setLastName(request.lastName());
        if (request.phoneNumber() != null) user.setPhoneNumber(request.phoneNumber());

        return UserResponseDTO.fromEntity(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponseDTO.fromEntity(user);
    }

    @Override
    @Transactional
    public void deleteCurrentUser(String currentUserEmail) {
        User user = findByEmailOrThrow(currentUserEmail);
        // Revoke all sessions before deletion so orphaned cookies can't be replayed.
        refreshTokenRepository.deleteAllByUser(user);
        userRepository.delete(user);
    }

    private User findByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
