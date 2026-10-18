package com.subscribly.userservice.config;

import com.subscribly.userservice.entity.Role;
import com.subscribly.userservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        seedRole("ROLE_USER");
        seedRole("ROLE_ADMIN");
    }

    private void seedRole(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = Role.builder().name(roleName).build();
            roleRepository.save(role);
            log.info("[DataSeeder] Seeded role: {}", roleName);
        }
    }
}
