package olistay.backend.service;

import olistay.backend.dto.BecomeHostRequestDTO;
import olistay.backend.dto.HostProfileResponseDTO;

public interface HostService {

    /**
     * Promotes an authenticated GUEST to HOST role.
     * Persists a HostProfile with the qualification data supplied.
     * Throws AlreadyHostException if the user is already HOST or ADMIN.
     * Throws DuplicateNationalIdException if the national ID is already registered.
     */
    HostProfileResponseDTO becomeHost(String currentUserEmail, BecomeHostRequestDTO request);
}