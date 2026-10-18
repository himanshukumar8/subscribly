package com.subscribly.config;

import com.subscribly.entity.Role;
import com.subscribly.entity.User;
import com.subscribly.repository.RoleRepository;
import com.subscribly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Hardened Administrator Provisioning Bootstrap.
 * Strictly opt-in via 'subscribly.security.admin-bootstrap.enabled' and fails closed
 * if required production credentials are weak, missing, or predictable.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminBootstrapConfig implements CommandLineRunner {

    private static final Set<String> REJECTED_DEFAULT_PASSWORDS = Set.of(
            "password@123",
            "password",
            "admin",
            "admin123",
            "admin12345678",
            "administrator",
            "changeme",
            "defaultpassword"
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    @Value("${subscribly.security.admin-bootstrap.enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${subscribly.security.admin-bootstrap.email:admin1@example.com}")
    private String adminEmail;

    @Value("${subscribly.security.admin-bootstrap.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!bootstrapEnabled) {
            log.info("[AdminBootstrap] Administrator bootstrap is disabled by configuration (subscribly.security.admin-bootstrap.enabled=false).");
            return;
        }

        try {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.execute(status -> {
                validateAndSeedAdministrator();
                return null;
            });
        } catch (Throwable t) {
            log.warn("[AdminBootstrap] Skipped initial administrator bootstrap (schema may not be fully initialized or unmigrated): {}", t.getMessage());
        }
    }

    public boolean validateAndSeedAdministrator() {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.error("[AdminBootstrap] Bootstrap rejected: 'subscribly.security.admin-bootstrap.password' is required when bootstrap is enabled, but none was provided.");
            return false;
        }

        if (adminPassword.length() < 12) {
            log.error("[AdminBootstrap] Bootstrap rejected: configured administrator password is too short ({} chars). Minimum requirement is 12 characters.",
                    adminPassword.length());
            return false;
        }

        String lowerPassword = adminPassword.trim().toLowerCase(Locale.ROOT);
        if (REJECTED_DEFAULT_PASSWORDS.contains(lowerPassword)) {
            log.error("[AdminBootstrap] Bootstrap rejected: configured password is a known predictable default. Supply a secure, high-entropy secret.");
            return false;
        }

        long existingAdminCount = userRepository.countAllAdministratorsEver();
        if (existingAdminCount > 0) {
            log.info("[AdminBootstrap] Administrator account(s) already exist (count={}). Skipping bootstrap to preserve intentional account lifecycle states.",
                    existingAdminCount);
            return false;
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

        String normalizedEmail = adminEmail != null && !adminEmail.isBlank() ? adminEmail.trim().toLowerCase(Locale.ROOT) : "admin1@example.com";
        User admin = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> User.builder()
                        .email(normalizedEmail)
                        .fullName("System Administrator")
                        .active(true)
                        .build());

        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setActive(true);
        List<Role> roles = admin.getRoles() != null ? admin.getRoles() : new ArrayList<>();
        if (!roles.contains(adminRole)) {
            roles.add(adminRole);
        }
        if (!roles.contains(userRole)) {
            roles.add(userRole);
        }
        admin.setRoles(roles);
        userRepository.save(admin);
        log.info("[AdminBootstrap] Successfully seeded initial administrator account: {}", normalizedEmail);
        return true;
    }
}
