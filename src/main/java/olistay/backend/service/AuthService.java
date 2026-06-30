package olistay.backend.service;

import olistay.backend.dto.LoginRequestDTO;
import olistay.backend.dto.RegisterRequestDTO;

/**
 * Contract for registration, login, token refresh, and logout.
 *
 * register/login/refresh return an AuthResult, not a bare AuthResponseDTO —
 * the controller needs the raw refresh token string to set the httpOnly
 * cookie, but that string must never be serialized into the JSON response
 * body. See AuthResult javadoc.
 */
public interface AuthService {

    AuthResult register(RegisterRequestDTO request);

    AuthResult login(LoginRequestDTO request);

    /**
     * @param incomingRefreshToken the raw token value read from the httpOnly cookie
     */
    AuthResult refresh(String incomingRefreshToken);

    /**
     * @param incomingRefreshToken the raw token value read from the httpOnly cookie,
     *                              so the specific session (not all sessions) is revoked
     */
    void logout(String incomingRefreshToken);
}
