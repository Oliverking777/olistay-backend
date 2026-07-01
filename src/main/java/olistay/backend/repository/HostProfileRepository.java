package olistay.backend.repository;

import olistay.backend.entity.HostProfile;
import olistay.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HostProfileRepository extends JpaRepository<HostProfile, Long> {

    Optional<HostProfile> findByUser(User user);

    Optional<HostProfile> findByUserId(Long userId);

    boolean existsByNationalIdNumber(String nationalIdNumber);

    boolean existsByUser(User user);

    /**
     * Admin: paginated list of all host profiles — HOST management view.
     */
    Page<HostProfile> findAll(Pageable pageable);
}
