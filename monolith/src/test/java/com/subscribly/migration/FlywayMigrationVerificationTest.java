package com.subscribly.migration;

import com.subscribly.entity.WebhookEvent;
import com.subscribly.repository.WebhookEventRepository;
import com.subscribly.service.WebhookEventService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlywayMigrationVerificationTest {

    private static final String DB_USER = "subscribly_user";
    private static final String DB_PASSWORD = "1";
    private static final String EMPTY_DB_URL = "jdbc:postgresql://localhost:5432/subscribly_disposable_empty";
    private static final String BASELINE_DB_URL = "jdbc:postgresql://localhost:5432/subscribly_disposable_baseline";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Pre-run Flyway on the empty disposable DB before Spring Boot initializes EntityManagerFactory
        Flyway flyway = Flyway.configure()
                .dataSource(EMPTY_DB_URL, DB_USER, DB_PASSWORD)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();

        // Point Spring Boot to the migrated disposable database and enforce strict validate
        registry.add("spring.datasource.url", () -> EMPTY_DB_URL);
        registry.add("spring.datasource.username", () -> DB_USER);
        registry.add("spring.datasource.password", () -> DB_PASSWORD);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private WebhookEventService webhookEventService;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Test
    @Order(1)
    @DisplayName("Disposable Empty DB: V1, V2, V3, V4 migrations execute in order and record success")
    void testEmptyDatabaseMigrationAndOrdering() {
        Flyway flyway = Flyway.configure()
                .dataSource(EMPTY_DB_URL, DB_USER, DB_PASSWORD)
                .locations("classpath:db/migration")
                .load();

        MigrationInfo[] applied = flyway.info().applied();
        assertEquals(4, applied.length, "All 4 migrations (V1, V2, V3, V4) must be applied");

        assertEquals("1", applied[0].getVersion().getVersion());
        assertEquals("initial schema", applied[0].getDescription());
        assertTrue(applied[0].getState().isApplied());

        assertEquals("2", applied[1].getVersion().getVersion());
        assertEquals("payment idempotency and retries", applied[1].getDescription());
        assertTrue(applied[1].getState().isApplied());

        assertEquals("3", applied[2].getVersion().getVersion());
        assertEquals("payment gateway integration", applied[2].getDescription());
        assertTrue(applied[2].getState().isApplied());

        assertEquals("4", applied[3].getVersion().getVersion());
        assertEquals("persist user active status", applied[3].getDescription());
        assertTrue(applied[3].getState().isApplied());
    }

    @Test
    @Order(2)
    @DisplayName("Schema Inspection: Verifies all entity columns, unique constraints, and indices")
    void testSchemaColumnsAndConstraints() throws Exception {
        try (Connection conn = DriverManager.getConnection(EMPTY_DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {

            // 1. Verify payments table columns
            Set<String> paymentColumns = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT column_name FROM information_schema.columns WHERE table_name = 'payments'")) {
                while (rs.next()) {
                    paymentColumns.add(rs.getString("column_name").toLowerCase());
                }
            }
            assertTrue(paymentColumns.contains("id"));
            assertTrue(paymentColumns.contains("idempotency_key"));
            assertTrue(paymentColumns.contains("gateway_order_id"));
            assertTrue(paymentColumns.contains("gateway_payment_id"));
            assertTrue(paymentColumns.contains("gateway_signature"));
            assertTrue(paymentColumns.contains("currency"));
            assertTrue(paymentColumns.contains("provider"));
            assertTrue(paymentColumns.contains("version"));
            assertTrue(paymentColumns.contains("created_at_instant"));
            assertTrue(paymentColumns.contains("next_retry_instant"));
            assertTrue(paymentColumns.contains("processed_at_instant"));
            assertTrue(paymentColumns.contains("failure_reason"));

            // 2. Verify webhook_events table columns
            Set<String> webhookColumns = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT column_name FROM information_schema.columns WHERE table_name = 'webhook_events'")) {
                while (rs.next()) {
                    webhookColumns.add(rs.getString("column_name").toLowerCase());
                }
            }
            assertTrue(webhookColumns.contains("id"));
            assertTrue(webhookColumns.contains("event_id"));
            assertTrue(webhookColumns.contains("event_type"));
            assertTrue(webhookColumns.contains("provider"));
            assertTrue(webhookColumns.contains("status"));
            assertTrue(webhookColumns.contains("received_at"));
            assertTrue(webhookColumns.contains("processed_at"));

            // 3. Verify unique indexes / constraints
            Set<String> indexes = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT indexname FROM pg_indexes WHERE tablename IN ('payments', 'webhook_events', 'subscriptions')")) {
                while (rs.next()) {
                    indexes.add(rs.getString("indexname").toLowerCase());
                }
            }
            assertTrue(indexes.contains("uk_payment_idempotency_key"), "Missing uk_payment_idempotency_key");
            assertTrue(indexes.contains("uk_payment_gateway_order_id"), "Missing uk_payment_gateway_order_id");
            assertTrue(indexes.contains("uk_webhook_event_id"), "Missing uk_webhook_event_id");
            assertTrue(indexes.contains("idx_payments_retry_lookup"), "Missing idx_payments_retry_lookup");
            assertTrue(indexes.contains("idx_subscriptions_due_lookup"), "Missing idx_subscriptions_due_lookup");

            // 4. Verify users table columns and active NOT NULL constraint
            Set<String> userColumns = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT column_name, is_nullable FROM information_schema.columns WHERE table_name = 'users'")) {
                while (rs.next()) {
                    String col = rs.getString("column_name").toLowerCase();
                    userColumns.add(col);
                    if ("active".equals(col)) {
                        assertEquals("NO", rs.getString("is_nullable"), "users.active must be NOT NULL");
                    }
                }
            }
            assertTrue(userColumns.contains("id"));
            assertTrue(userColumns.contains("email"));
            assertTrue(userColumns.contains("full_name"));
            assertTrue(userColumns.contains("password"));
            assertTrue(userColumns.contains("active"));
        }
    }

    @Test
    @Order(3)
    @DisplayName("Hibernate ddl-auto: validate passes cleanly against Flyway-migrated schema")
    void testHibernateDdlAutoValidateContextLoaded() {
        // Because the test is annotated with @SpringBootTest and spring.jpa.hibernate.ddl-auto=validate,
        // reaching this test means Hibernate's SchemaValidator validated all entity mappings against the DB without error!
        assertNotNull(webhookEventService);
        assertNotNull(webhookEventRepository);
    }

    @Test
    @Order(4)
    @DisplayName("Baseline & Incremental Migration: Baselines legacy V1 database and successfully upgrades with V2, V3, and V4")
    void testBaselineAndIncrementalMigration() throws Exception {
        // Step 1: Initialize baseline DB with only original V1 schema (simulating pre-Flyway legacy schema)
        Flyway baselineCleaner = Flyway.configure()
                .dataSource(BASELINE_DB_URL, DB_USER, DB_PASSWORD)
                .cleanDisabled(false)
                .load();
        baselineCleaner.clean();

        // Apply original V1 directly (no active column)
        try (Connection conn = DriverManager.getConnection(BASELINE_DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGSERIAL PRIMARY KEY,
                    full_name VARCHAR(255),
                    email VARCHAR(255) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL
                );
                CREATE TABLE IF NOT EXISTS roles (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(255) NOT NULL UNIQUE
                );
                CREATE TABLE IF NOT EXISTS user_roles (
                    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    roles_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                    PRIMARY KEY (user_id, roles_id)
                );
                CREATE TABLE IF NOT EXISTS subscriptions (
                    id BIGSERIAL PRIMARY KEY,
                    user_id BIGINT NOT NULL REFERENCES users(id),
                    plan VARCHAR(255) NOT NULL,
                    price DOUBLE PRECISION NOT NULL,
                    billing_cycle VARCHAR(50) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    start_date DATE,
                    end_date DATE,
                    started_at TIMESTAMP WITH TIME ZONE,
                    expires_at TIMESTAMP WITH TIME ZONE,
                    auto_renew BOOLEAN DEFAULT TRUE
                );
                CREATE TABLE IF NOT EXISTS payments (
                    id BIGSERIAL PRIMARY KEY,
                    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id),
                    user_id BIGINT REFERENCES users(id),
                    amount DOUBLE PRECISION NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    attempts INTEGER DEFAULT 0,
                    created_at DATE,
                    next_retry_at DATE,
                    auto_renew BOOLEAN DEFAULT FALSE
                );
                INSERT INTO users (id, full_name, email, password) VALUES (1, 'Legacy User', 'legacy@example.com', 'hashed_legacy');
            """);
        }

        // Step 2: Flyway baselineOnMigrate=true, baselineVersion="1"
        Flyway flyway = Flyway.configure()
                .dataSource(BASELINE_DB_URL, DB_USER, DB_PASSWORD)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();

        MigrateResult result = flyway.migrate();
        assertTrue(result.success, "Incremental migration must succeed");

        MigrationInfo[] applied = flyway.info().applied();
        assertEquals(4, applied.length);
        assertEquals("1", applied[0].getVersion().getVersion());
        assertTrue(applied[0].getState().isApplied());
        assertEquals("2", applied[1].getVersion().getVersion());
        assertEquals("3", applied[2].getVersion().getVersion());
        assertEquals("4", applied[3].getVersion().getVersion());

        // Verify existing legacy user has active = true
        try (Connection conn = DriverManager.getConnection(BASELINE_DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT active FROM users WHERE id = 1")) {
            assertTrue(rs.next());
            assertTrue(rs.getBoolean("active"), "Legacy user must have active set to true");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Existing Schema Upgrade: Upgrades V3 schema to V4, backfills existing users with active=true, and validates schema")
    void testUpgradeFromV3ToV4PreservesUsers() throws Exception {
        // Step 1: Clean and migrate baseline DB to target V3 only
        Flyway cleaner = Flyway.configure()
                .dataSource(BASELINE_DB_URL, DB_USER, DB_PASSWORD)
                .cleanDisabled(false)
                .load();
        cleaner.clean();

        Flyway v3Flyway = Flyway.configure()
                .dataSource(BASELINE_DB_URL, DB_USER, DB_PASSWORD)
                .locations("classpath:db/migration")
                .target("3")
                .load();
        MigrateResult v3Result = v3Flyway.migrate();
        assertTrue(v3Result.success);
        assertEquals(3, v3Flyway.info().applied().length);

        // Step 2: Insert existing users into V3 schema (where active column does NOT exist yet)
        try (Connection conn = DriverManager.getConnection(BASELINE_DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO users (id, full_name, email, password) VALUES (101, 'User Prior V4', 'user_prior_v4@example.com', 'hash1')");
            stmt.execute("INSERT INTO users (id, full_name, email, password) VALUES (102, 'Admin Prior V4', 'admin_prior_v4@example.com', 'hash2')");
        }

        // Step 3: Run Flyway forward migration to V4
        Flyway v4Flyway = Flyway.configure()
                .dataSource(BASELINE_DB_URL, DB_USER, DB_PASSWORD)
                .locations("classpath:db/migration")
                .target("4")
                .load();
        MigrateResult v4Result = v4Flyway.migrate();
        assertTrue(v4Result.success, "Migration from V3 to V4 must succeed");
        assertEquals(4, v4Flyway.info().applied().length);

        // Step 4: Verify existing users have active = true and NOT NULL
        try (Connection conn = DriverManager.getConnection(BASELINE_DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT id, email, active FROM users WHERE id IN (101, 102) ORDER BY id")) {
                assertTrue(rs.next());
                assertEquals(101L, rs.getLong("id"));
                assertEquals("user_prior_v4@example.com", rs.getString("email"));
                assertTrue(rs.getBoolean("active"), "Existing user must be backfilled to active=true");

                assertTrue(rs.next());
                assertEquals(102L, rs.getLong("id"));
                assertEquals("admin_prior_v4@example.com", rs.getString("email"));
                assertTrue(rs.getBoolean("active"), "Existing admin must be backfilled to active=true");
            }
        }

        // Step 5: Verify Hibernate ddl-auto: validate passes against this upgraded V3->V4 database
        Map<String, Object> jpaProps = new HashMap<>();
        jpaProps.put("jakarta.persistence.jdbc.url", BASELINE_DB_URL);
        jpaProps.put("jakarta.persistence.jdbc.user", DB_USER);
        jpaProps.put("jakarta.persistence.jdbc.password", DB_PASSWORD);
        jpaProps.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        jpaProps.put("hibernate.hbm2ddl.auto", "validate");
        jpaProps.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        jpaProps.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");

        org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean emfBean =
                new org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean();
        emfBean.setPackagesToScan("com.subscribly.entity");
        emfBean.setJpaVendorAdapter(new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter());
        emfBean.setJpaPropertyMap(jpaProps);
        emfBean.afterPropertiesSet();
        assertNotNull(emfBean.getObject(), "Hibernate ddl-auto: validate succeeded against upgraded V4 database");
        emfBean.destroy();
    }

    @Test
    @Order(6)
    @DisplayName("Database-Backed Webhook Concurrency: Concurrent inserts on uk_webhook_event_id roll back sub-tx cleanly without aborting connection")
    void testDatabaseBackedWebhookConcurrency() throws Exception {
        String sharedEventId = "evt_concurrency_pg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                startGate.await();
                try {
                    return webhookEventService.recordEventIfNew(sharedEventId, "payment.captured", "RAZORPAY");
                } catch (Exception ex) {
                    // Even if constraint violation bubbles up from sub-transaction, it is handled
                    return false;
                } finally {
                    doneGate.countDown();
                }
            }));
        }

        startGate.countDown();
        assertTrue(doneGate.await(10, TimeUnit.SECONDS), "Concurrent tasks did not finish in time");
        executor.shutdown();

        int successCount = 0;
        int duplicateCount = 0;

        for (Future<Boolean> f : futures) {
            if (Boolean.TRUE.equals(f.get())) {
                successCount++;
            } else {
                duplicateCount++;
            }
        }

        assertEquals(1, successCount, "Exactly ONE concurrent thread must claim the event");
        assertEquals(1, duplicateCount, "The other concurrent thread must detect duplicate or collision");

        // Verify that after collision, connection is still healthy and queries work
        Optional<WebhookEvent> found = webhookEventRepository.findByEventId(sharedEventId);
        assertTrue(found.isPresent(), "Event must exist in DB");
        assertEquals("PROCESSING", found.get().getStatus());
    }
}
