package olistay.backend.repository;

import olistay.backend.entity.TenantFinancialProfile;
import olistay.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantFinancialProfileRepository extends JpaRepository<TenantFinancialProfile, Long> {

    Optional<TenantFinancialProfile> findByUser(User user);

    boolean existsByUser(User user);
}