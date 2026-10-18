package com.subscribly.security;

import com.subscribly.entity.Role;
import com.subscribly.entity.User;
import com.subscribly.repository.RoleRepository;
import com.subscribly.repository.UserRepository;
import com.subscribly.service.UserService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AdminConcurrencyIntegrationTest {

    private static final String DB_USER = "subscribly_user";
    private static final String DB_PASSWORD = "1";
    private static final String EMPTY_DB_URL = "jdbc:postgresql://localhost:5432/subscribly_disposable_empty";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        Flyway flyway = Flyway.configure()
                .dataSource(EMPTY_DB_URL, DB_USER, DB_PASSWORD)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();

        registry.add("spring.datasource.url", () -> EMPTY_DB_URL);
        registry.add("spring.datasource.username", () -> DB_USER);
        registry.add("spring.datasource.password", () -> DB_PASSWORD);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User admin1;
    private User admin2;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_ADMIN")));
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_USER")));

        admin1 = userRepository.save(User.builder()
                .fullName("Admin One")
                .email("admin1@example.com")
                .password("hash1")
                .active(true)
                .roles(new ArrayList<>(List.of(adminRole)))
                .build());

        admin2 = userRepository.save(User.builder()
                .fullName("Admin Two")
                .email("admin2@example.com")
                .password("hash2")
                .active(true)
                .roles(new ArrayList<>(List.of(adminRole)))
                .build());
    }

    @Test
    @DisplayName("Concurrent Demotion: Two concurrent requests attempting to demote both admins serialize safely; exactly one succeeds and one receives 409 Conflict")
    void testConcurrentAdminDemotionSafety() throws Exception {
        assertEquals(2, userRepository.countActiveAdministrators(), "Precondition: exactly 2 active administrators exist");

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        // Task 1: Demote Admin 1 by Admin 2
        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startGate.await();
                userService.updateUserRole(admin1.getId(), "ROLE_USER", admin2.getEmail());
                successCount.incrementAndGet();
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode().value() == 409) {
                    conflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } catch (Exception ex) {
                otherErrorCount.incrementAndGet();
            } finally {
                doneGate.countDown();
            }
        });

        // Task 2: Demote Admin 2 by Admin 1
        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startGate.await();
                userService.updateUserRole(admin2.getId(), "ROLE_USER", admin1.getEmail());
                successCount.incrementAndGet();
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode().value() == 409) {
                    conflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } catch (Exception ex) {
                otherErrorCount.incrementAndGet();
            } finally {
                doneGate.countDown();
            }
        });

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "Threads failed to initialize");
        startGate.countDown();
        assertTrue(doneGate.await(15, TimeUnit.SECONDS), "Concurrent execution timed out");
        executor.shutdown();

        assertEquals(0, otherErrorCount.get(), "Unexpected errors during concurrent demotion");
        assertEquals(1, successCount.get(), "Exactly one demotion must succeed");
        assertEquals(1, conflictCount.get(), "The other concurrent demotion must be blocked with 409 Conflict");

        assertEquals(1, userRepository.countActiveAdministrators(),
                "CRITICAL: Exactly 1 active administrator must remain in the system after concurrent demotion");
    }

    @Test
    @DisplayName("Concurrent Deactivation: Two concurrent requests attempting to deactivate both admins serialize safely; exactly one succeeds and one receives 409 Conflict")
    void testConcurrentAdminDeactivationSafety() throws Exception {
        assertEquals(2, userRepository.countActiveAdministrators(), "Precondition: exactly 2 active administrators exist");

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger otherErrorCount = new AtomicInteger(0);

        // Task 1: Deactivate Admin 1 by Admin 2
        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startGate.await();
                userService.updateUserStatus(admin1.getId(), "INACTIVE", admin2.getEmail());
                successCount.incrementAndGet();
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode().value() == 409) {
                    conflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } catch (Exception ex) {
                otherErrorCount.incrementAndGet();
            } finally {
                doneGate.countDown();
            }
        });

        // Task 2: Deactivate Admin 2 by Admin 1
        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startGate.await();
                userService.updateUserStatus(admin2.getId(), "INACTIVE", admin1.getEmail());
                successCount.incrementAndGet();
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode().value() == 409) {
                    conflictCount.incrementAndGet();
                } else {
                    otherErrorCount.incrementAndGet();
                }
            } catch (Exception ex) {
                otherErrorCount.incrementAndGet();
            } finally {
                doneGate.countDown();
            }
        });

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "Threads failed to initialize");
        startGate.countDown();
        assertTrue(doneGate.await(15, TimeUnit.SECONDS), "Concurrent execution timed out");
        executor.shutdown();

        assertEquals(0, otherErrorCount.get(), "Unexpected errors during concurrent deactivation");
        assertEquals(1, successCount.get(), "Exactly one deactivation must succeed");
        assertEquals(1, conflictCount.get(), "The other concurrent deactivation must be blocked with 409 Conflict");

        assertEquals(1, userRepository.countActiveAdministrators(),
                "CRITICAL: Exactly 1 active administrator must remain in the system after concurrent deactivation");
    }
}
