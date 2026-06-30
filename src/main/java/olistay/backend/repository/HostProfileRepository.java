package olistay.backend.repository;

import olistay.backend.entity.HostProfile;
import olistay.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HostProfileRepository extends JpaRepository<HostProfile, Long> {

    Optional<HostProfile> findByUser(User user);

    boolean existsByNationalIdNumber(String nationalIdNumber);

    boolean existsByUser(User user);
}
