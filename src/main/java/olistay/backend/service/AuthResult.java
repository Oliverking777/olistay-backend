package olistay.backend.service;

import olistay.backend.dto.AuthResponseDTO;

/**
 * Internal carrier returned by AuthService methods — NOT exposed via the
 * REST API directly. Pairs the safe, JSON-serializable AuthResponseDTO with
 * the raw refresh token string, which the controller needs in order to set
 * the httpOnly cookie but which must never appear in a response body.
 *
 * This exists so AuthServiceImpl doesn't need an HttpServletResponse
 * dependency just to set a cookie — that stays a controller concern — while
 * still giving the controller everything it needs from a single service call.
 */
public record AuthResult(
        AuthResponseDTO body,
        String rawRefreshToken
) {}