package olistay.backend.repository;

import olistay.backend.entity.RefreshToken;
import olistay.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Primary lookup when a refresh request comes in.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Used by the grace-window check: if the incoming token was already marked
     * used, this finds the token it was rotated into, so a near-simultaneous
     * second request can be treated as a retry instead of theft.
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :replacedByToken")
    Optional<RefreshToken> findByReplacedByToken(@Param("replacedByToken") String replacedByToken);

    /**
     * Used on logout to invalidate every active session for a user.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    void revokeAllByUser(@Param("user") User user);

    /**
     * Used when theft is confirmed (a revoked/expired token is reused outside
     * the grace window) — revokes the entire token family for that user as a
     * precaution.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
