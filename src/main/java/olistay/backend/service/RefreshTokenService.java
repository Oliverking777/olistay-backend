package olistay.backend.service;

import olistay.backend.entity.RefreshToken;
import olistay.backend.entity.User;

/**
 * Owns the lifecycle of opaque refresh tokens: issuing, rotating, and
 * detecting reuse/theft.
 *
 * Rotation strategy: every successful /auth/refresh call invalidates the
 * presented token and issues a brand new one ("rotation"). If a token that
 * has already been marked used is presented again, that's normally a sign of
 * theft (an attacker replaying a stolen token after the legitimate client
 * already rotated past it) — EXCEPT for the specific double-refresh race
 * condition this project hit: two near-simultaneous requests from the *same*
 * legitimate client (e.g. two browser tabs, or a retried request after a
 * flaky network) both presenting the same token before either had a chance
 * to see the rotation complete.
 *
 * The grace window (jwt.refresh-grace-window-seconds) distinguishes these
 * cases: if the reused token's replacement was issued less than N seconds
 * ago, treat it as a duplicate request and silently return the existing
 * replacement chain rather than flagging theft.
 */
public interface RefreshTokenService {

    /**
     * Creates and persists a brand new refresh token for the given user.
     * Used at login/register, and as the final step of a successful rotation.
     */
    RefreshToken createRefreshToken(User user);

    /**
     * Looks up a presented token and validates it can be used: not expired,
     * not revoked, and either unused or within the grace window. Throws
     * TokenRefreshException for every other case (missing, expired, revoked,
     * reused outside the grace window).
     */
    RefreshToken validateForRotation(String token);

    /**
     * Marks the given token as used and links it to its replacement, then
     * persists. Called as part of rotation, before the new token is returned
     * to the caller.
     */
    void rotateToken(RefreshToken oldToken, RefreshToken newToken);

    /**
     * Revokes every active refresh token belonging to the user. Used on
     * logout and as the response to confirmed token theft.
     */
    void revokeAllUserTokens(User user);

    /**
     * Plain lookup with no validation side effects — does NOT check expiry,
     * does NOT trigger theft detection, does NOT consider the grace window.
     * Used by logout(), where the only thing that matters is "whose tokens
     * do we revoke", and a user must always be able to log out even with an
     * expired or already-used token.
     */
    RefreshToken findByTokenOrThrow(String token);
}
