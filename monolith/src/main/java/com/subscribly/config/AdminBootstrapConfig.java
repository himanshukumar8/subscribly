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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminBootstrapConfig implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    @Value("${app.bootstrap.admin.email:admin1@example.com}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:Password@123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        try {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.execute(status -> {
                seedAdminIfNotPresent();
                return null;
            });
        } catch (Throwable t) {
            log.warn("[AdminBootstrap] Skipped initial administrator bootstrap (schema may not be fully initialized or unmigrated): {}", t.getMessage());
        }
    }

    private void seedAdminIfNotPresent() {
        if (userRepository.countActiveAdministrators() == 0) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

            String normalizedEmail = adminEmail.trim().toLowerCase();
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
        }
    }
}
