package olistay.backend.dto;

/**
 * Returned from /auth/login, /auth/register, and /auth/refresh.
 *
 * Deliberately does NOT carry the refresh token — that is set as an httpOnly
 * cookie by the controller and must never appear in a JSON response body,
 * where it would be reachable from JavaScript / logs.
 */
public record AuthResponseDTO(
        String accessToken,
        String tokenType,
        UserResponseDTO user
) {

    /**
     * Convenience factory so call sites don't need to remember the literal
     * "Bearer" token type string everywhere.
     */
    public static AuthResponseDTO of(String accessToken, UserResponseDTO user) {
        return new AuthResponseDTO(accessToken, "Bearer", user);
    }
}
