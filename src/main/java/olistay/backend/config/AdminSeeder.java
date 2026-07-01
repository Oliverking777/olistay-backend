package olistay.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import olistay.backend.entity.User;
import olistay.backend.enums.Role;
import olistay.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a single default ADMIN account on application startup so there's
 * always at least one way into the admin panel, even on a fresh database.
 *
 * Safety properties:
 *   - Idempotent: checks existsByEmail() before creating anything, so it's
 *     safe to run on every single application restart without ever
 *     duplicating the account or throwing a unique-constraint violation.
 *   - Non-fatal: wrapped in try/catch. If seeding fails for any reason
 *     (DB not ready, constraint issue, etc.) it logs an error and lets the
 *     application continue starting up rather than crashing the whole app —
 *     an admin can always be created manually as a fallback.
 *   - Configurable via application.properties / environment variables,
 *     with sane defaults for local/dev use.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@olistay.com}")
    private String adminEmail;

    @Value("${app.admin.password:ChangeMe123!}")
    private String adminPassword;

    @Value("${app.admin.first-name:Olistay}")
    private String adminFirstName;

    @Value("${app.admin.last-name:Admin}")
    private String adminLastName;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            seedAdmin();
        } catch (Exception e) {
            // Never let seeding failures take the whole application down —
            // log loudly and move on. Worst case: no default admin exists
            // and one must be created manually / via a DB migration.
            log.error("Admin seeding failed — application will continue starting. Reason: {}", e.getMessage(), e);
        }
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin seeder: account '{}' already exists — skipping.", adminEmail);
            return;
        }

        User admin = User.builder()
                .firstName(adminFirstName)
                .lastName(adminLastName)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .enabled(true)
                .accountNonLocked(true)
                .build();

        userRepository.save(admin);

        log.warn(
                "Admin seeder: created default admin account '{}'. " +
                        "If this is a production environment, log in and change the " +
                        "password immediately, or override app.admin.password before deploy.",
                adminEmail
        );
    }
}
