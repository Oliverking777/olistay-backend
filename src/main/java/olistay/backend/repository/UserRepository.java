package olistay.backend.repository;

import olistay.backend.entity.User;
import olistay.backend.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
