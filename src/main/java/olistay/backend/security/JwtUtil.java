package olistay.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import olistay.backend.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Handles creation and validation of JWT ACCESS tokens.
 *
 * Note: refresh tokens are deliberately NOT JWTs in this design — they are
 * opaque random strings persisted in {@link olistay.backend.entity.RefreshToken},
 * which is what makes server-side revocation, rotation, and theft detection
 * possible. A self-contained JWT refresh token cannot be revoked before its
 * expiry without an additional blocklist, so we don't use one here. The
 * opaque-token generation lives in RefreshTokenService, not here.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms}") long accessTokenExpirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    /**
     * Issues a short-lived access token. Subject is the user's email; role is
     * embedded as a claim so the filter can populate authorities without an
     * extra DB hit on every request.
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates signature and expiry, and confirms the token's subject matches
     * the expected user email. Returns false rather than throwing so the
     * filter can treat any failure mode uniformly as "unauthenticated".
     */
    public boolean isTokenValid(String token, String expectedEmail) {
        try {
            String email = extractEmail(token);
            return email.equals(expectedEmail) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        // Throws ExpiredJwtException / SignatureException / MalformedJwtException
        // as appropriate; callers that need a boolean result should go through
        // isTokenValid() instead of catching these directly.
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}