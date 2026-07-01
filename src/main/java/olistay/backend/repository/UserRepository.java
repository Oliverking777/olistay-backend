package olistay.backend.repository;

import olistay.backend.entity.User;
import olistay.backend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Used by CustomUserDetailsService during authentication (email is the username).
     */
    Optional<User> findByEmail(String email);

    /**
     * Used during registration to enforce email uniqueness with a friendly error
     * before hitting the DB unique-constraint exception.
     */
    boolean existsByEmail(String email);

    /**
     * Used by the "Become a Landlord" flow and admin views to filter by role.
     */
    List<User> findAllByRole(Role role);

    /**
     * Admin: paginated list of all users, optionally filtered by role.
     * Passing null for role returns all users regardless of role.
     */
    @Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role)")
    Page<User> findAllByOptionalRole(@Param("role") Role role, Pageable pageable);

    /**
     * Admin: lock or unlock a user account by toggling accountNonLocked.
     * Done via @Modifying query to avoid loading the full entity just to
     * flip one boolean.
     */
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = :locked WHERE u.id = :id")
    void setAccountNonLocked(@Param("id") Long id, @Param("locked") boolean locked);

    /**
     * Admin: enable or disable a user account entirely.
     */
    @Modifying
    @Query("UPDATE User u SET u.enabled = :enabled WHERE u.id = :id")
    void setEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);
}