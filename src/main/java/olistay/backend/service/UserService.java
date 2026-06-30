package olistay.backend.service;

import olistay.backend.dto.UpdateProfileRequestDTO;
import olistay.backend.dto.UserResponseDTO;

public interface UserService {

    /**
     * Returns the profile of the currently authenticated user.
     * "currentUserEmail" is extracted from the SecurityContext by the controller.
     */
    UserResponseDTO getCurrentUser(String currentUserEmail);

    /**
     * Partially updates the authenticated user's profile.
     * Only non-null fields in the request are applied (PATCH semantics).
     */
    UserResponseDTO updateCurrentUser(String currentUserEmail, UpdateProfileRequestDTO request);

    /**
     * Returns any user's public profile by ID.
     * Used for viewing host profiles on property listings.
     */
    UserResponseDTO getUserById(Long id);

    /**
     * Permanently deletes the authenticated user's account and revokes
     * all their refresh tokens.
     */
    void deleteCurrentUser(String currentUserEmail);
}
