# Subscribly — Repository State & Reviewable Handoff

## 1. Supported Deployment Mode & Architecture
- **Verified Deployment Target**: Standalone Monolith (`monolith/`).
- **Status**: The monolithic application builds as an executable fat JAR (`monolith/target/subscribly-0.0.1-SNAPSHOT.jar`), migrates clean schemas via Flyway V1–V4, satisfies Hibernate `ddl-auto: validate` in `prod` profile (against both fresh databases and V3-upgraded databases), and passes 192 tests across the Maven reactor including 166 tests in `monolith/`.
- **Incomplete Microservices Target**: The multi-service structure (`user-service`, `subscription-service`, `payment-service`, `notification-service`, `discovery-server`, `config-server`, `api-gateway`) represents an ongoing architectural extraction. It lacks Flyway database migrations, relies on development `ddl-auto: update`, has no application container configurations in `docker-compose.yml`, and depends on Kafka/Eureka clustering that is not yet configured for standalone staging.

---

## 2. Standalone Monolith Completeness & Security Hardening

### 2.1 User-Management Business Logic & API Restoration
During the initial multi-module extraction, `UserController.java` and `UserService.java` were relocated to `user-service`, leaving `monolith/` without user profile retrieval and without user administration in `AdminController.java`.

This functional gap has been resolved directly within `monolith/`:
1. **User DTO**: Created `monolith/src/main/java/com/subscribly/dto/UserResponse.java` providing safe profile representation (ID, full name, email, roles, active status) while strictly omitting password hashes.
2. **User Service**: Created `monolith/src/main/java/com/subscribly/service/UserService.java` implementing profile retrieval, user listing, role mutation, and account activation/deactivation. To prevent bean definition collisions, `UserService` does **not** implement `UserDetailsService` (which is already provided as a dedicated `@Bean` in `SecurityConfig.java`).
3. **User Controller**: Created `monolith/src/main/java/com/subscribly/controller/UserController.java` serving `GET /api/v1/user/me` and legacy `GET /api/user/me`.
4. **Admin Controller**: Restored user administration endpoints in `monolith/src/main/java/com/subscribly/controller/AdminController.java`:
   - `GET /api/v1/admin/users` (and legacy `/api/admin/users`)
   - `PUT /api/v1/admin/users/{id}/role` (and legacy `/api/admin/users/{id}/role`)
   - `PUT /api/v1/admin/users/{id}/status` (and legacy `/api/admin/users/{id}/status`)
5. **Security Configuration**: Hardened `SecurityConfig.java` to restrict `/api/v1/admin/**` and `/api/admin/**` to `ROLE_ADMIN` and permit `/api/v1/auth/**` and `/api/auth/**`.

### 2.2 Hardened Administrator Security & High-Impact Edge Cases
1. **RBAC Enforcement**:
   - `SecurityConfig` enforces `.requestMatchers("/api/v1/admin/**", "/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")`.
   - `AdminController` is annotated with `@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")`.
   - Valid authenticated ordinary users (`ROLE_USER`) receive `403 Forbidden` across all admin operations.
   - Unauthenticated requests receive `401 Unauthorized`.
2. **Input Validation**:
   - Role input is strictly validated: Shorthand values (`"ADMIN"`, `"USER"`) are normalized to `"ROLE_ADMIN"` and `"ROLE_USER"`. Unrecognized roles return `400 Bad Request`.
   - Status input is strictly validated: Accepts only `"ACTIVE"` or `"INACTIVE"` (case-insensitive). Invalid values return `400 Bad Request`.
   - User existence: Unknown user IDs return `404 Not Found` rather than misleading success responses.
3. **High-Impact Edge Case & Concurrency Protections**:
   - **Last Active Administrator Protection (Concurrency Safe)**: Demoting or deactivating the last remaining active administrator is blocked and returns `409 Conflict`.
   - **Pessimistic Concurrency Locking**: To eliminate the race condition where two simultaneous admin requests observe `countActiveAdministrators() == 2` concurrently and both proceed to remove the last admins, `UserService` acquires a pessimistic row lock via `RoleRepository.findByNameWithLock("ROLE_ADMIN")` (`SELECT ... FROM roles WHERE name = 'ROLE_ADMIN' FOR UPDATE`). In PostgreSQL (`READ COMMITTED`), this serializes admin role/status mutations. The second transaction waits for the first to commit, re-evaluates the committed count (`== 1`), and is cleanly rejected with `409 Conflict`. This behavior is proven by multi-threaded integration test `AdminConcurrencyIntegrationTest`.
   - **Self-Demotion / Self-Deactivation Protection**: An operating administrator cannot demote their own account (`409 Conflict`) or deactivate their own active account (`400 Bad Request`). Another administrator must perform these actions.
4. **Credential & Entity Safety**:
   - `User.java` has `@JsonIgnore` on `private String password;`, preventing Jackson serialization leaks.
   - `UserResponse` DTO contains only safe public fields.
   - Privilege escalation via registration is prevented: `AuthController.register()` hardcodes `ROLE_USER` regardless of payload content.
5. **Flyway Migration Strategy & Persistent Active Column**:
   - **V1 Preservation**: Pristine baseline `V1__initial_schema.sql` was restored to its original 4-column structure (`id`, `full_name`, `email`, `password`) to preserve checksum integrity and prevent checksum mismatch failures on pre-existing databases.
   - **Forward Migration V4**: Created `V4__persist_user_active_status.sql` which safely adds `active BOOLEAN DEFAULT TRUE`, backfills existing records where `active IS NULL`, applies `ALTER TABLE users ALTER COLUMN active SET NOT NULL`, and seeds baseline system roles (`ROLE_USER`, `ROLE_ADMIN`) idempotently.
   - **Verified Upgrade Compatibility**: Tested and verified via `FlywayMigrationVerificationTest`: V3 databases with legacy users are cleanly upgraded to V4 with user active status backfilled to `true`, and Hibernate `ddl-auto: validate` passes without error against both fresh and upgraded schemas.
   - `User.isEnabled()` returns `active`.
   - `AuthController.login()` validates `user.isActive()` and rejects deactivated accounts with `403 Forbidden` (`Account has been deactivated`).
   - `AuthController.login()` generates JWT tokens containing the user's real database roles, ensuring promoted administrators receive `ROLE_ADMIN` in subsequent authenticated requests.
6. **Known Limitation (Stateless JWT Revocation)**:
   - In accordance with standard stateless JWT architecture, deactivating an account immediately blocks new logins and password-based authentication. However, existing unexpired JWT tokens (valid up to 24h) remain cryptographically valid until their expiry because `JwtAuthFilter` does not perform a database query on every request. Immediate token invalidation requires provisioning a distributed token blacklist (e.g. Redis).

### 2.3 Preservation of `CommonBeansConfig`
In `HEAD`, `CommonBeansConfig.java` defined `PasswordEncoder` (`BCryptPasswordEncoder`) and `AuthenticationManager`. In `monolith`, these bean definitions were consolidated into `SecurityConfig.java` (avoiding duplicate configuration classes), while `user-service` maintains its own `CommonBeansConfig.java`. All security beans are preserved and function without conflicts.

### 2.4 Microservice Independence
The monolith does not require Eureka, Config Server, OpenFeign, or Kafka for standalone operation:
- `spring.config.import`: Configured with `optional:configserver:http://localhost:8888/`, so startup succeeds when Config Server is absent.
- `eureka.client.enabled`: Disabled in `application-prod.yml` (`false`) to eliminate connection retries during standalone deployments.
- No OpenFeign clients exist in `monolith/`.

---

## 3. Root Source Tree vs. Working Tree Change Mapping

The repository originally contained 32 tracked files under `src/` at commit `HEAD`. Below is the complete behavioral and relocation mapping for every file:

| Original Path (`HEAD`) | Working Tree Location | Nature of Change | Monolith Availability | Discrepancy / Notes |
| :--- | :--- | :--- | :--- | :--- |
| `src/main/java/.../SubscriblyApplication.java` | `monolith/.../SubscriblyApplication.java` | Rename / Relocation | Yes | Preserved as monolith entry point. |
| `src/main/java/.../config/CommonBeansConfig.java` | `monolith/.../config/SecurityConfig.java` & `user-service/.../config/CommonBeansConfig.java` | Deliberate Consolidation | Yes | PasswordEncoder and AuthenticationManager beans consolidated into monolith `SecurityConfig`. |
| `src/main/java/.../config/JwtAuthFilter.java` | `monolith/.../config/JwtAuthFilter.java` | Rename / Relocation & Hardening | Yes | Preserved and hardened across modules. |
| `src/main/java/.../config/SecurityConfig.java` | `monolith/.../config/SecurityConfig.java` | Rename / Relocation & Hardening | Yes | Preserved; permits `/error`, `/api/v1/health`, public webhook, secures `/api/v1/admin/**`. |
| `src/main/java/.../controller/AdminController.java` | `monolith/.../controller/AdminController.java` | Rename / Relocation & Restoration | Yes | Restored `/users`, `/{id}/role`, `/{id}/status` with DTO protection. |
| `src/main/java/.../controller/AuthController.java` | `monolith/.../controller/AuthController.java` | Rename / Relocation | Yes | Provides register, login, legacy routes. |
| `src/main/java/.../controller/HealthController.java` | `monolith/.../controller/HealthController.java` | Rename / Relocation | Yes | Health check endpoint preserved. |
| `src/main/java/.../controller/PaymentController.java` | `monolith/.../controller/PaymentController.java` | Rename / Relocation & Hardening | Yes | Order creation, signature verification, user history, webhook handling. |
| `src/main/java/.../controller/SubscriptionController.java` | `monolith/.../controller/SubscriptionController.java` | Rename / Relocation | Yes | Full subscription lifecycle endpoints. |
| `src/main/java/.../controller/UserController.java` | `monolith/.../controller/UserController.java` & `user-service/.../controller/UserController.java` | Extraction & Monolith Restoration | Yes | Restored in monolith with `UserResponse` DTO; also preserved in `user-service`. |
| `src/main/java/.../dto/LoginRequest.java` | `monolith/.../dto/LoginRequest.java` | Rename / Relocation | Yes | Preserved. |
| `src/main/java/.../dto/RegisterRequest.java` | `monolith/.../dto/RegisterRequest.java` | Rename / Relocation | Yes | Preserved. |
| `src/main/java/.../dto/SubscriptionRequest.java` | `monolith/.../dto/SubscriptionRequest.java` | Rename / Relocation | Yes | Preserved. |
| `src/main/java/.../dto/SubscriptionResponse.java` | `monolith/.../dto/SubscriptionResponse.java` | Rename / Relocation | Yes | Preserved. |
| `src/main/java/.../entity/BillingCycle.java` | `monolith/.../entity/BillingCycle.java` | Rename / Relocation | Yes | Preserved. |
| `src/main/java/.../entity/Payment.java` | `monolith/.../entity/Payment.java` | Rename / Relocation & Hardening | Yes | Added `@Version` for optimistic locking. |
| `src/main/java/.../entity/PaymentStatus.java` | `monolith/.../entity/PaymentStatus.java` | Rename / Relocation | Yes | Preserved. |
| `src/main/java/.../entity/Role.java` | `monolith/.../entity/Role.java` | Rename / Relocation | Yes | Preserved. |
| `src/main/java/.../entity/Subscription.java` | `monolith/.../entity/Subscription.java` | Rename / Relocation & Hardening | Yes | Added `@Version` for optimistic locking. |
| `src/main/java/.../entity/SubscriptionStatus.java` | `monolith/.../entity/SubscriptionStatus.java` | Rename / Relocation | Yes | Preserved. |
| `src/main/java/.../entity/User.java` | `monolith/.../entity/User.java` | Rename / Relocation | Yes | Preserved; active column persisted. |
| `src/main/java/.../repository/PaymentRepository.java` | `monolith/.../repository/PaymentRepository.java` | Rename / Relocation | Yes | Preserved. |
| `src/main/java/.../repository/RoleRepository.java` | `monolith/.../repository/RoleRepository.java` | Rename / Relocation | Yes | Preserved. |
| `src/main/java/.../repository/SubscriptionRepository.java` | `monolith/.../repository/SubscriptionRepository.java` | Rename / Relocation | Yes | Preserved. |
| `src/main/java/.../repository/UserRepository.java` | `monolith/.../repository/UserRepository.java` | Rename / Relocation | Yes | Preserved. |
| `src/main/java/.../scheduler/SubscriptionScheduler.java` | `monolith/.../scheduler/SubscriptionScheduler.java` | Rename / Relocation & Hardening | Yes | Preserved with cron scheduling. |
| `src/main/java/.../service/PaymentService.java` | `monolith/.../service/PaymentService.java` | Rename / Relocation & Hardening | Yes | Gateway integration, idempotency, webhook event processing. |
| `src/main/java/.../service/SubscriptionService.java` | `monolith/.../service/SubscriptionService.java` | Rename / Relocation | Yes | Full subscription management and entitlement validation. |
| `src/main/java/.../service/UserService.java` | `monolith/.../service/UserService.java` & `user-service/.../service/UserService.java` | Extraction & Monolith Restoration | Yes | Restored in monolith with role/status validation and last admin protections. |
| `src/main/java/.../util/JwtUtil.java` | `monolith/.../config/JwtUtil.java` | Rename / Relocation & Hardening | Yes | Hardened with externalized secret, HMAC-SHA256 enforcement, fail-fast checks. |
| `src/main/resources/application.yml` | `monolith/.../resources/application.yml` | Rename / Relocation & Externalization | Yes | Externalized configuration, created `application-prod.yml`. |
| `src/test/java/.../SubscriblyApplicationTests.java` | `monolith/.../SubscriblyApplicationTests.java` | Rename / Relocation | Yes | Context load test preserved. |

**Summary**: 0 unresolved deletions. All 32 original source files and behaviors are accounted for and preserved in `monolith/`.

---

## 4. Security Audit & Credential Scanning

### 4.1 Scanner Methodology & Scope
- **Tool**: `scratch/scan_secrets.py` (enhanced with working tree and complete Git history auditing).
- **Working Tree Scope**: All source code, resource files, Maven configurations, scripts, and documentation files across all 10 modules.
- **Git History Scope**: Full repository commit history (5 commits: `a103b7a`, `5766d51`, `d0d98d9`, `5db869f`, `f1353e1`).
- **Detected Signatures**: Private key headers, AWS access keys, live Razorpay keys, compromised historical JWT secrets, embedded connection strings.
- **Scanner Limitations**:
  1. Pattern-based detection does not perform dynamic entropy analysis on compiled binary archives (`.jar`).
  2. Does not audit credentials provisioned in external cloud secrets managers (AWS Secrets Manager, HashiCorp Vault).

### 4.2 Findings & Secret Distinctions
1. **Active High-Risk Secrets in Working Tree**: **0**
   - No hardcoded JWT secrets, private keys, database passwords, or live Razorpay keys exist in active application code or configuration.
   - All references to the historical secret in documentation files (`docs/PROJECT_AUDIT.md`, `docs/PHASE_1_EXECUTION_PLAN.md`, `docs/STAGING_RUNBOOK.md`) have been strictly redacted/masked.
2. **Historical Exposure in Git History**: **1**
   - Git commit `a103b7a` (the initial repository commit) contained a hardcoded JWT secret in `JwtUtil.java`.
   - In accordance with non-negotiable safety rules, Git history was **not** rewritten.
   - **Remediation**: Active code on branch `microservices-v2` externalizes `JWT_SECRET`. Production and staging environments MUST provision a freshly generated 256-bit Base64 signing key.
3. **Smoke Test Safety**:
   - `scratch/staging_smoke_test.py` uses ephemeral mock accounts (`alice.staging@example.com`, `bob.staging@example.com`).
   - The script strictly suppresses printing JWT bearer tokens and sensitive response bodies to stdout.

### 4.3 `.gitignore` Hardening
`.gitignore` has been updated to exclude:
- Local credential files (`.env`, `.env.*`, `*.env`)
- Scratch scripts and temporary test artifacts (`scratch/`)
- Log files (`*.log`)
- Temporary database dumps (`*.dump`, `*.sql.bak`)
- OS metadata (`.DS_Store`, `Thumbs.db`)

---

## 5. Verified Build, Test, and Artifact Results

### 5.1 Compilation & Test Execution
- **Java Version**: OpenJDK 21 (`azul-21.0.7`).
- **Full Reactor Execution**: `.\mvnw.cmd test`
  - Modules tested: 10
  - Total tests executed: **192**
  - Failures: **0**
  - Errors: **0**
  - Skipped: **0**
  - Execution Time: ~42.2s
- **Monolith Test Suite**: `.\mvnw.cmd test -pl monolith`
  - Total tests executed: **166** (including Flyway V1-V4 migrations, upgrade compatibility from V3 to V4, RBAC authorization, role/status validation, and multi-threaded pessimistic admin concurrency protection tests)
  - Failures: **0**
  - Errors: **0**
  - Skipped: **0**
- **Other Modules**:
  - `payment-service`: 10 tests passed (4 gateway provider, 6 jwt)
  - `subscription-service`: 9 tests passed (3 service lifecycle/reconciliation, 6 jwt)
  - `user-service`: 7 tests passed (1 context loads, 6 jwt)

### 5.2 Packaged Executable Artifact
- **Command**: `.\mvnw.cmd package -pl monolith -DskipTests=false`
- **Artifact Path**: `monolith/target/subscribly-0.0.1-SNAPSHOT.jar`
- **Artifact Size**: 103,072,671 bytes (~103 MB Spring Boot repackaged executable fat JAR).
- **Result**: `BUILD SUCCESS`.

---

## 6. Reviewable Multi-Commit Plan

To ensure clean code review and preserve Git rename detection, the uncommitted changes must be committed in logical increments by pairing deleted `src/` files with their destination paths in the same commit.

> [!IMPORTANT]
> Do NOT run `git commit` or `git add` at this stage. This plan documents the exact sequence for review when ready.

### Commit 1: `refactor(build): establish multi-module reactor and exclude local artifacts`
- **Files**: `pom.xml`, `.gitignore`.
- **Purpose**: Establishes `subscribly-parent` POM and updates Git exclusion rules.
- **Command**:
  ```bash
  git add pom.xml .gitignore
  git commit -m "refactor(build): establish multi-module reactor and exclude local artifacts"
  ```

### Commit 2: `refactor(monolith): relocate root src tree to monolith module`
- **Files**: All 32 deletions under `src/` paired with additions under `monolith/src/`.
- **Purpose**: Moves the flat Spring Boot application into `monolith/` while enabling Git's similarity index to record clean renames.
- **Command**:
  ```bash
  git add src/ monolith/src/ monolith/pom.xml
  git commit -m "refactor(monolith): relocate root src tree to monolith module"
  ```

### Commit 3: `feat(modules): introduce microservice scaffolding and shared library`
- **Files**: `common-lib/`, `user-service/`, `subscription-service/`, `payment-service/`, `notification-service/`, `discovery-server/`, `config-server/`, `api-gateway/`, `config-repo/`, `docker-compose.yml`, `docker/`, `frontend/`.
- **Purpose**: Commits extracted domain services, shared contracts, and supporting service scaffolding.
- **Command**:
  ```bash
  git add common-lib/ user-service/ subscription-service/ payment-service/ notification-service/ discovery-server/ config-server/ api-gateway/ config-repo/ docker-compose.yml docker/ frontend/
  git commit -m "feat(modules): introduce microservice scaffolding and shared library"
  ```

### Commit 4: `security(jwt): enforce HMAC-SHA256, externalized signing secret, and fail-fast startup`
- **Files**: `monolith/src/main/java/com/subscribly/config/JwtUtil.java`, `monolith/src/test/java/com/subscribly/config/JwtUtilTest.java`, module JWT utilities.
- **Purpose**: Hardens token validation, removes hardcoded keys, enforces 256-bit key length, and validates tamper/expiration handling.

### Commit 5: `fix(monolith): restore standalone user management and profile endpoints`
- **Files**: `monolith/src/main/java/com/subscribly/dto/UserResponse.java`, `monolith/src/main/java/com/subscribly/service/UserService.java`, `monolith/src/main/java/com/subscribly/controller/UserController.java`, `monolith/src/main/java/com/subscribly/controller/AdminController.java`, associated unit/WebMvc tests.
- **Purpose**: Restores `/api/v1/user/me` and `/api/v1/admin/users` endpoints in the monolith with safe DTOs and complete regression tests.

### Commit 6: `security(admin): harden user administration, RBAC, input validation, and last-admin protections`
- **Files**: `monolith/src/main/java/com/subscribly/entity/User.java`, `monolith/src/main/java/com/subscribly/entity/Payment.java`, `monolith/src/main/resources/db/migration/V1__initial_schema.sql`, `monolith/src/main/resources/db/migration/V4__persist_user_active_status.sql`, `monolith/src/main/java/com/subscribly/repository/UserRepository.java`, `monolith/src/main/java/com/subscribly/repository/RoleRepository.java`, `monolith/src/main/java/com/subscribly/service/UserService.java`, `monolith/src/main/java/com/subscribly/controller/AdminController.java`, `monolith/src/main/java/com/subscribly/controller/AuthController.java`, `monolith/src/main/java/com/subscribly/config/SecurityConfig.java`, associated test classes (`FlywayMigrationVerificationTest`, `AdminConcurrencyIntegrationTest`, etc.).
- **Purpose**: Adds RBAC enforcement, pessimistic concurrency locking (`PESSIMISTIC_WRITE` on `ROLE_ADMIN`) for last-admin protection, self-demotion/deactivation protections, role and status validation, pristine V1 baseline preservation, forward migration V4 with legacy user backfill, and password hash serialization suppression.

### Commit 7: `fix(gateway): enforce fail-fast Razorpay credentials validation and offline simulation separation`
- **Files**: `monolith/src/main/java/com/subscribly/gateway/RazorpayGatewayProvider.java`, `RazorpayGatewayProviderTest.java`.
- **Purpose**: Validates Razorpay credentials at startup when live mode is enabled; prevents silent fallback to mock mode.

### Commit 8: `docs: update staging runbook, security audit, and repository handoff`
- **Files**: `docs/REPOSITORY_HANDOFF.md`, `docs/STAGING_RUNBOOK.md`, `docs/PROJECT_AUDIT.md`, `docs/PHASE_1_EXECUTION_PLAN.md`.
- **Purpose**: Documents architectural state, verification runs, secret redaction, and runbooks.

---

## 7. Outstanding Release Blockers & Staging Prerequisites

1. **JWT Secret Generation**:
   - Production/staging environment must supply `JWT_SECRET` via external environment variable with `>= 256 bits` of entropy (`openssl rand -base64 32`).
2. **Database Provisioning**:
   - Dedicated staging PostgreSQL database must be created with a distinct user (`subscribly_user`).
   - Flyway migrations V1–V4 must be executed prior to launching application instances (or via automated startup migration).
3. **Payment Gateway Credentials**:
   - For live Razorpay test transactions, provision real test API keys (`RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`) and set `RAZORPAY_LIVE_CALL_ENABLED=true`.
4. **Containerization**:
   - A multi-stage Dockerfile for `monolith/` should be finalized before Kubernetes/ECS staging deployment.
5. **Token Revocation Blacklist (Future Roadmap)**:
   - To support immediate invalidation of unexpired JWTs when accounts are deactivated, provision Redis and implement a JWT blacklist filter.
