package olistay.backend.service.impl;

import lombok.RequiredArgsConstructor;
import olistay.backend.entity.RefreshToken;
import olistay.backend.entity.User;
import olistay.backend.exception.TokenRefreshException;
import olistay.backend.repository.RefreshTokenRepository;
import olistay.backend.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${jwt.refresh-grace-window-seconds}")
    private long graceWindowSeconds;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(generateOpaqueToken())
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .used(false)
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public RefreshToken validateForRotation(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException("Refresh token not recognized"));

        if (refreshToken.isRevoked()) {
            throw new TokenRefreshException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new TokenRefreshException("Refresh token has expired, please log in again");
        }

        if (!refreshToken.isUsed()) {
            // Normal case: first time this token is being redeemed.
            return refreshToken;
        }

        // Token was already used. This is either theft (replay of a stolen
        // token after rotation) or the legitimate double-refresh race
        // condition (duplicate request arriving within the grace window).
        boolean withinGraceWindow = refreshToken.getUsedAt() != null
                && refreshToken.getUsedAt().plusSeconds(graceWindowSeconds).isAfter(LocalDateTime.now());

        if (withinGraceWindow && refreshToken.getReplacedByToken() != null) {
            // Treat as a duplicate request: return the token this one was
            // already rotated into, so the caller can issue a fresh access
            // token from it instead of re-rotating again.
            return refreshTokenRepository.findByToken(refreshToken.getReplacedByToken())
                    .orElseThrow(() -> new TokenRefreshException("Replacement token not found"));
        }

        // Reused outside the grace window: treat as theft. Revoke the entire
        // token family for this user so a stolen token chain is fully killed.
        revokeAllUserTokens(refreshToken.getUser());
        throw new TokenRefreshException("Refresh token reuse detected, all sessions revoked for safety");
    }

    @Override
    @Transactional
    public void rotateToken(RefreshToken oldToken, RefreshToken newToken) {
        oldToken.setUsed(true);
        oldToken.setUsedAt(LocalDateTime.now());
        oldToken.setReplacedByToken(newToken.getToken());
        refreshTokenRepository.save(oldToken);
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken findByTokenOrThrow(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException("Refresh token not recognized"));
    }

    /**
     * 256 bits of randomness, URL-safe base64 encoded. Deliberately NOT a
     * JWT — see RefreshTokenService javadoc for why opaque tokens are used
     * for refresh while JwtUtil is used only for access tokens.
     */
    private String generateOpaqueToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}