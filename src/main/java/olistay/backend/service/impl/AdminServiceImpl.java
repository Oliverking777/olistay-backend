package olistay.backend.service.impl;

import lombok.RequiredArgsConstructor;
import olistay.backend.dto.*;
import olistay.backend.entity.Property;
import olistay.backend.entity.PropertyImage;
import olistay.backend.entity.User;
import olistay.backend.enums.PropertyStatus;
import olistay.backend.enums.Role;
import olistay.backend.exception.InvalidStateException;
import olistay.backend.exception.ResourceNotFoundException;
import olistay.backend.repository.*;
import olistay.backend.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final UserRepository userRepository;
    private final HostProfileRepository hostProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // ── Listing moderation ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<PropertySummaryDTO> getPendingListings(Pageable pageable) {
        return propertyRepository.findAllByStatus(PropertyStatus.UNDER_REVIEW, pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional
    public PropertyResponseDTO approveListing(Long propertyId) {
        Property property = findPropertyOrThrow(propertyId);

        if (property.getStatus() != PropertyStatus.UNDER_REVIEW) {
            throw new InvalidStateException(
                    "Only listings in UNDER_REVIEW status can be approved. " +
                            "Current status: " + property.getStatus()
            );
        }

        property.setStatus(PropertyStatus.AVAILABLE);
        Property saved = propertyRepository.save(property);
        return PropertyResponseDTO.fromEntity(saved, loadImages(saved));
    }

    @Override
    @Transactional
    public PropertyResponseDTO rejectListing(Long propertyId, String reason) {
        Property property = findPropertyOrThrow(propertyId);

        if (property.getStatus() != PropertyStatus.UNDER_REVIEW) {
            throw new InvalidStateException(
                    "Only listings in UNDER_REVIEW status can be rejected. " +
                            "Current status: " + property.getStatus()
            );
        }

        // Archive rather than delete — preserves the host's submission
        // for audit and lets them resubmit after fixing the issues.
        property.setStatus(PropertyStatus.ARCHIVED);
        Property saved = propertyRepository.save(property);

        // Log the rejection reason server-side. A notification flow
        // (email/in-app) can be added here when that feature is built.
        System.out.printf(
                "[ADMIN] Listing %d rejected. Host: %s. Reason: %s%n",
                saved.getId(), saved.getHost().getEmail(),
                reason != null ? reason : "No reason provided"
        );

        return PropertyResponseDTO.fromEntity(saved, loadImages(saved));
    }

    @Override
    @Transactional
    public PropertyResponseDTO archiveListing(Long propertyId) {
        Property property = findPropertyOrThrow(propertyId);
        property.setStatus(PropertyStatus.ARCHIVED);
        Property saved = propertyRepository.save(property);
        return PropertyResponseDTO.fromEntity(saved, loadImages(saved));
    }

    // ── User management ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDTO> getUsers(Role roleFilter, Pageable pageable) {
        return userRepository.findAllByOptionalRole(roleFilter, pageable)
                .map(AdminUserResponseDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponseDTO getUserById(Long userId) {
        return AdminUserResponseDTO.fromEntity(findUserOrThrow(userId));
    }

    @Override
    @Transactional
    public AdminUserResponseDTO lockUser(Long userId) {
        User user = findUserOrThrow(userId);
        guardSelfAction(user);
        userRepository.setAccountNonLocked(userId, false);
        user.setAccountNonLocked(false);
        return AdminUserResponseDTO.fromEntity(user);
    }

    @Override
    @Transactional
    public AdminUserResponseDTO unlockUser(Long userId) {
        User user = findUserOrThrow(userId);
        userRepository.setAccountNonLocked(userId, true);
        user.setAccountNonLocked(true);
        return AdminUserResponseDTO.fromEntity(user);
    }

    @Override
    @Transactional
    public AdminUserResponseDTO disableUser(Long userId) {
        User user = findUserOrThrow(userId);
        guardSelfAction(user);
        userRepository.setEnabled(userId, false);
        user.setEnabled(false);
        return AdminUserResponseDTO.fromEntity(user);
    }

    @Override
    @Transactional
    public AdminUserResponseDTO enableUser(Long userId) {
        User user = findUserOrThrow(userId);
        userRepository.setEnabled(userId, true);
        user.setEnabled(true);
        return AdminUserResponseDTO.fromEntity(user);
    }

    @Override
    @Transactional
    public void revokeAllSessions(Long userId) {
        User user = findUserOrThrow(userId);
        refreshTokenRepository.revokeAllByUser(user);
    }

    // ── Host management ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<HostProfileAdminResponseDTO> getHosts(Pageable pageable) {
        return hostProfileRepository.findAll(pageable)
                .map(HostProfileAdminResponseDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public HostProfileAdminResponseDTO getHostByUserId(Long userId) {
        return HostProfileAdminResponseDTO.fromEntity(
                hostProfileRepository.findByUserId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "No host profile found for user id: " + userId
                        ))
        );
    }

    @Override
    @Transactional
    public AdminUserResponseDTO demoteHost(Long userId) {
        User user = findUserOrThrow(userId);

        if (user.getRole() != Role.HOST) {
            throw new InvalidStateException(
                    "User is not a HOST — cannot demote. Current role: " + user.getRole()
            );
        }

        // Downgrade role — HostProfile row is deliberately retained for audit.
        user.setRole(Role.GUEST);
        User saved = userRepository.save(user);

        // Revoke all sessions so the ex-host's currently running sessions
        // don't keep HOST-scoped access beyond this point.
        refreshTokenRepository.revokeAllByUser(saved);

        return AdminUserResponseDTO.fromEntity(saved);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Property findPropertyOrThrow(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    /**
     * Prevents an admin from locking/disabling their own account via the
     * admin API — would leave the platform with no accessible admin account
     * if the last admin self-locks. The check is lightweight (role only,
     * no email comparison) since the admin endpoints don't receive the
     * caller's own identity.
     */
    private void guardSelfAction(User target) {
        if (target.getRole() == Role.ADMIN) {
            throw new InvalidStateException(
                    "Admin accounts cannot be locked or disabled via the admin API"
            );
        }
    }

    private PropertySummaryDTO toSummary(Property property) {
        String primaryImageUrl = propertyImageRepository
                .findFirstByPropertyAndIsPrimaryTrue(property)
                .map(PropertyImage::getImageUrl)
                .orElse(null);
        return PropertySummaryDTO.fromEntity(property, primaryImageUrl);
    }

    private List<PropertyImageResponseDTO> loadImages(Property property) {
        return propertyImageRepository.findAllByPropertyOrderByUploadOrderAsc(property)
                .stream()
                .map(PropertyImageResponseDTO::fromEntity)
                .toList();
    }
}