# Subscribly — Project Audit (Phase Zero)

**Audit Date:** 2026-10-17  
**Auditor Role:** Principal Backend Engineer / Security / SRE  
**Branch:** `microservices-v2` (current HEAD: `f1353e1`)  
**Repository:** https://github.com/himanshukumar8/subscribly

---

## 1. Repository State

### 1.1 Git History
- **5 total commits** on `microservices-v2`, all linear (no merges).
- Commits: initial commit → README → scheduler fix → repository update → payment query.
- **Branch `master`** exists (remote HEAD) but shares the same commit history.
- **Critical finding:** The entire microservices decomposition is **uncommitted**. `git status` shows:
  - 32 files **deleted** from the original `src/` tree (the monolith's root-level source).
  - 14 directories **untracked** (all new microservice modules, docker, frontend, etc.).
  - The root `pom.xml` is **modified** (changed from single-module to multi-module parent).
- This means the committed codebase on GitHub is the **original flat monolith** under `src/`, while the local working tree has been restructured into microservices **without a single commit**.

### 1.2 Project Structure (Local Working Tree)

```
subscribly/
├── pom.xml                    # Multi-module parent POM (modified, uncommitted)
├── monolith/                  # Relocated original monolith (untracked)
├── user-service/              # New microservice (untracked)
├── subscription-service/      # New microservice (untracked)
├── payment-service/           # New microservice (untracked)
├── notification-service/      # New microservice (untracked)
├── common-lib/                # Shared DTOs and events (untracked)
├── api-gateway/               # Spring Cloud Gateway (untracked)
├── discovery-server/          # Eureka Server (untracked)
├── config-server/             # Spring Cloud Config (untracked)
├── config-repo/               # Config files (untracked)
├── docker/                    # Docker init scripts (untracked)
├── docker-compose.yml         # Postgres + Redis + Kafka (untracked)
├── frontend/                  # Vite/React frontend (untracked)
└── README.md                  # Original README (committed)
```

### 1.3 Technology Stack

| Component | Version / Technology |
|---|---|
| **Java** | POM targets **21**; local machine has **JDK 25 only** |
| **Spring Boot** | **3.4.3** |
| **Spring Cloud** | **2024.0.0** |
| **Build** | Maven 3.9.11 (wrapper) |
| **Database** | PostgreSQL (via Docker Compose, postgres:15-alpine) |
| **ORM** | Spring Data JPA / Hibernate |
| **Security** | Spring Security + custom JWT (jjwt 0.11.5) |
| **Messaging** | Spring Kafka (Confluent 7.4.0 in Docker) |
| **Service Discovery** | Eureka |
| **API Gateway** | Spring Cloud Gateway |
| **Config** | Spring Cloud Config Server (native profile) |
| **Lombok** | Used across all modules |
| **Frontend** | Vite + React + TypeScript + Tailwind (separate) |

---

## 2. Baseline Build Results

### 2.1 Toolchain Compatibility & Build Baseline

#### Environment Discrepancy (Initial Failure)
When building with the system default Java runtime (`java version "25.0.1"` at `C:\Program Files\Java\jdk-25`):
```
Command: .\mvnw.cmd clean compile -pl common-lib -DskipTests
Result:  BUILD FAILURE
Error:   java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN
```
**Root Cause:** This is an **environment compatibility issue**, not a code defect. Lombok's annotation processor (version 1.18.36 / transitive from Spring Boot 3.4.3) relies on internal `javac` APIs (`com.sun.tools.javac.code.TypeTag`) that were changed/refactored in JDK 25. The project POM explicitly targets Java 21 (`<java.version>21</java.version>`).

#### Verified Baseline Under Compatible JDK 21
Testing against a compatible JDK 21 distribution (`azul-21.0.7` located at `C:\Users\HIMANSHU\.jdks\azul-21.0.7`):
```cmd
set JAVA_HOME=C:\Users\HIMANSHU\.jdks\azul-21.0.7
set PATH=C:\Users\HIMANSHU\.jdks\azul-21.0.7\bin;%PATH%
.\mvnw.cmd compile -DskipTests
```
**Result:** **`BUILD SUCCESS`** across all 10 reactor modules in 2.509 seconds!

| Module | Build Status (JDK 21) | Time |
|---|---|---|
| `subscribly-parent` | SUCCESS | 0.003s |
| `monolith` (`subscribly`) | SUCCESS | 1.150s |
| `discovery-server` | SUCCESS | 0.248s |
| `api-gateway` | SUCCESS | 0.147s |
| `config-server` | SUCCESS | 0.165s |
| `common-lib` | SUCCESS | 0.037s |
| `user-service` | SUCCESS | 0.094s |
| `notification-service` | SUCCESS | 0.082s |
| `subscription-service` | SUCCESS | 0.130s |
| `payment-service` | SUCCESS | 0.084s |

**Conclusion:** The project source code is **NOT broken**. It compiles cleanly across all modules when executed with the project-configured Java 21 toolchain.

### 2.2 Test Suite Baseline Execution
Executing the full test suite under JDK 21:
```cmd
set JAVA_HOME=C:\Users\HIMANSHU\.jdks\azul-21.0.7
set PATH=C:\Users\HIMANSHU\.jdks\azul-21.0.7\bin;%PATH%
.\mvnw.cmd test
```
**Result:** **`BUILD SUCCESS`** across all 10 modules in 22.172 seconds.
- `SubscriblyApplicationTests.contextLoads()` in `monolith` executed and **PASSED** (20.901s, successfully bootstrapped Spring Data JPA against local PostgreSQL).
- **Test Deficit:** Zero tests exist in `user-service`, `subscription-service`, `payment-service`, `notification-service`, `api-gateway`, `discovery-server`, or `config-server` (`No sources to compile` in `src/test`).
- The project has only 1 smoke test for the entire codebase. Unit, integration, security, and concurrency test coverage is currently 0%.

---

## 3. Architecture Assessment

### 3.1 Current Architecture: Incomplete Microservices Migration

The project has been partially decomposed from a monolith into microservices but the migration is **incomplete and unverified**:

| Module | Port | Database | Purpose | Maturity |
|---|---|---|---|---|
| `monolith` | 8089 | `subscribly` / `monolith_db` | Original all-in-one app (security disabled!) | Legacy, security stripped |
| `user-service` | 8081 | `users_db` | Auth, users, JWT | Most complete |
| `subscription-service` | 8082 | `subscriptions_db` | Subscription CRUD | Functional structure |
| `payment-service` | 8083 | `payments_db` | Payment processing, audit, webhooks | Most sophisticated |
| `notification-service` | 8084 | N/A | Kafka consumer, email/SMS/push stubs | Stub only |
| `api-gateway` | 8080 | N/A | Route traffic, block internal APIs | Functional |
| `discovery-server` | 8761 | N/A | Eureka registry | Boilerplate |
| `config-server` | 8888 | N/A | Centralized config | Boilerplate |

### 3.2 Architecture Problems

1. **Monolith has security explicitly disabled.** `SubscriblyApplication.java` excludes `SecurityAutoConfiguration`, `UserDetailsServiceAutoConfiguration`, `SecurityFilterAutoConfiguration`, and `ManagementWebSecurityAutoConfiguration`. Every endpoint is unauthenticated. This is directly contradicted by the README's security claims.

2. **Monolith has no auth/user controllers.** The original `AuthController`, `UserController`, `SecurityConfig`, and `JwtAuthFilter` were deleted from the committed `src/` tree and relocated to `user-service/`. The monolith now has controllers for subscriptions, payments, admin, and health — but **no authentication mechanism whatsoever**.

3. **Running the full stack requires 7+ JVM processes + Kafka + Zookeeper + Redis + Postgres.** This is extremely heavy for a zero-budget portfolio project. Starting the full microservices stack locally requires Docker Compose for infrastructure + starting 7 separate Spring Boot applications.

4. **Inter-service communication via Feign requires all services running.** `subscription-service` calls `user-service` via Feign; `payment-service` calls both `user-service` and `subscription-service` via Feign. If any upstream service is down, dependent services throw unhandled `FeignException`.

5. **Each database uses `ddl-auto: update`** — no version-controlled migrations (Flyway/Liquibase). Schema is auto-generated from entities. This is not production-safe.

6. **Docker Compose creates databases with password `password` but application configs use password `1`.** Credential mismatch means Docker-based setup won't work without manual fixes.

---

## 4. Verified Features (Code-Level Inspection)

### Features That Exist in Code

| Feature | Location | Status |
|---|---|---|
| User registration | `user-service/AuthController.register()` | Implemented, uses BCrypt |
| User login + JWT generation | `user-service/AuthController.login()` | Implemented, returns JWT with roles claim |
| JWT validation filter | `user-service/JwtAuthFilter` (also copied to subscription-service, payment-service) | Implemented but duplicated 3x |
| Password hashing (BCrypt) | `user-service/CommonBeansConfig` → `PasswordEncoder` bean | Verified |
| Role seeding (ROLE_USER, ROLE_ADMIN) | `user-service/DataSeeder` | Verified |
| Get current user profile | `user-service/UserController.getLoggedUser()` | Implemented |
| Update password | `user-service/UserController.updatePassword()` | Implemented |
| Admin: list all users | `user-service/AdminUserController.getAllUsers()` | Implemented |
| Admin: update user role/status | `user-service/AdminUserController` | Implemented |
| Create subscription | `subscription-service/SubscriptionService.createSubscription()` | Implemented, calls user-service via Feign |
| Get my subscriptions | `subscription-service/SubscriptionController.getMySubscriptions()` | Implemented |
| Cancel subscription | `subscription-service/SubscriptionService.cancelSubscription()` | Implemented (immediate cancel, sets endDate=now) |
| Update billing cycle | `subscription-service/SubscriptionService.updateBillingCycle()` | Implemented |
| Admin: update subscription status | `subscription-service/SubscriptionController.updateStatus()` | Implemented but **no admin role check in controller** |
| Payment order creation | `payment-service/PaymentService.createOrder()` | Implemented with gateway abstraction |
| Payment verification | `payment-service/PaymentService.verifyPayment()` | Implemented with signature check |
| Webhook handling | `payment-service/PaymentService.processWebhook()` | Implemented with idempotency |
| Payment state machine | `payment-service/PaymentStateMachine` | Implemented with explicit transition map |
| Payment audit trail | `payment-service/PaymentAuditService` | Implemented (REQUIRES_NEW propagation) |
| Webhook idempotency | `payment-service/WebhookEventRecord` | DB-backed, unique constraint on event_id |
| Retry backoff calculation | `payment-service/PaymentService.calculateNextRetry()` | Exponential: 15m × 2^(n-1) |
| Kafka event publishing | subscription-service, payment-service | Implemented with safe try/catch |
| Notification consumer | `notification-service/NotificationEventConsumer` | Stub implementation (logs, no actual delivery) |
| Internal API blocking | `api-gateway/InternalApiBlockingFilter` | Blocks `/api/v1/internal/` at gateway |
| Health endpoint | `monolith/HealthController` | Manual health check (redundant with Actuator) |
| Actuator health/info | All services | Configured |
| CORS configuration | `api-gateway/application.yml` | Configured for localhost origins |
| Global exception handler | `user-service/GlobalExceptionHandler` | Implemented |
| Graceful shutdown | user-service, subscription-service, payment-service | `server.shutdown: graceful` |

### Features Claimed in README but Missing or Broken

| Feature | README Claim | Actual Status |
|---|---|---|
| "JWT Authentication" | Yes | **Broken in monolith** (security disabled). Works only in microservices. |
| "Secured private endpoints" | Yes | **Monolith has zero security.** Microservices have JWT filter. |
| "Auto-renew support" | Yes | **Stub only.** `createAutoRenewPaymentForSubscription()` is empty. `processPayment()` returns `true` without doing anything. |
| "Retry logic for failed payments" | Yes | **Partially implemented.** Scheduler finds payments to retry but `processPayment()` is a no-op. Payment-service has retry fields but no scheduled retry processor. |
| "Daily scheduled job" | Yes | Exists in monolith (`@Scheduled(fixedRate = 300000)` = 5 min, not daily). **Calls empty methods.** Not present in microservices. |
| "Centralized exceptions" | Yes | Only in `user-service`. Missing in subscription-service, payment-service. |
| "Clean Architecture" | Yes | Controllers return JPA entities directly in most cases. DTOs exist but are inconsistently used. |

---

## 5. Security Vulnerabilities

### 5.1 Critical

| # | Vulnerability | Location | Severity |
|---|---|---|---|
| S1 | **Monolith has ALL security disabled** | `monolith/SubscriblyApplication.java` excludes all security auto-config | CRITICAL |
| S2 | **JWT secret hardcoded in source** | `JwtUtil.java`: `A7fL...JoI=` (historical commit a103b7a) | CRITICAL |
| S3 | **Razorpay mock credentials in source** | `payment-service/application.yml` and `RazorpayGatewayProvider.java` | MEDIUM (mock keys) |
| S4 | **Database password `1` hardcoded** | All `application.yml` files | CRITICAL |
| S5 | **Internal APIs unprotected** | `/api/v1/internal/**` is `permitAll()` in all SecurityConfigs. Gateway blocks them but direct service access bypasses this. | CRITICAL |
| S6 | **No CORS on individual services** | CORS only configured at gateway. Direct service access has no CORS restriction. | MEDIUM |
| S7 | **No rate limiting** on auth endpoints | Login/register have no throttling | MEDIUM |
| S8 | **Subscription status update has no admin check** | `SubscriptionController.updateStatus()` has no role annotation; path is under `anyRequest().authenticated()`, not admin-only | CRITICAL |
| S9 | **`/api/v1/payments/all` in monolith has no admin check** | Any authenticated user can see all payments | CRITICAL |
| S10 | **JWT has no issuer claim validation** | Token just validates subject + expiry, no issuer check | MEDIUM |
| S11 | **No token revocation/refresh mechanism** | Once issued, JWT is valid for 24h with no way to invalidate | MEDIUM |

### 5.2 Authentication Flow Issues

- JWT filter in `subscription-service` and `payment-service` extracts username from JWT but **does not load user details from a database** — it creates a bare `UsernamePasswordAuthenticationToken` with the subject string. The `@AuthenticationPrincipal` in controllers resolves to a plain `String` (username), not a `UserDetails`. This means **role-based checks in security config (`hasAnyAuthority`) cannot work** because no authorities are set in the authentication token.

---

## 6. Data Model Defects

### 6.1 Financial Precision
- **`Double` used for monetary amounts** in:
  - `monolith/Subscription.price` → `Double`
  - `monolith/Payment.amount` → `Double`
  - `subscription-service/Subscription.price` → `Double`
  - `payment-service/Payment.amount` → `Double`
  - `common-lib/SubscriptionDto.price` → `Double`
- **This is incorrect for financial calculations.** Floating-point arithmetic produces rounding errors. Must use `BigDecimal` with explicit scale.

### 6.2 Timestamp Issues
- Monolith uses `LocalDate` for `Payment.createdAt` and `Payment.nextRetryAt` — insufficient granularity for payment timestamps.
- Payment-service correctly uses `LocalDateTime` for these fields — inconsistency between monolith and microservice.
- No timezone handling anywhere — all `LocalDate.now()` / `LocalDateTime.now()` use JVM default timezone.

### 6.3 Missing Constraints
- Monolith `User` entity has no `@Table(name=...)` annotation — defaults to table name `user` which is a PostgreSQL reserved word.
- Monolith `Subscription` has `@ManyToOne(fetch = FetchType.LAZY)` to `User` but exposes the entity via REST, causing potential `LazyInitializationException` or N+1 queries.
- No `@Version` field on any entity — no optimistic locking for concurrent updates.
- Subscription entity has no unique constraint preventing duplicate active subscriptions for the same user+plan.

### 6.4 Schema Generation
- All services use `ddl-auto: update` — Hibernate auto-generates schema. This is unsafe for production:
  - Cannot roll back schema changes.
  - Does not handle column renames, type changes, or data migrations.
  - Can silently create incorrect schema.

---

## 7. Business Logic Defects

### 7.1 Subscription Lifecycle
- **No subscription state machine.** Status transitions are arbitrary — `updateStatus()` accepts any string and converts via `valueOf()`. A subscription can go from `CANCELLED` → `ACTIVE` with no restriction.
- **Cancellation is immediate only.** `cancelSubscription()` sets `endDate = LocalDate.now()`. No "cancel at end of billing period" option.
- **`updateBillingCycle()` resets endDate from today**, not from current endDate. Changing billing cycle mid-period loses remaining prepaid time.

### 7.2 Payment Processing
- **Monolith `PaymentService.makePayment()` always creates SUCCESS payment** — no actual payment processing, no failure simulation, no pending state.
- **Monolith scheduler calls empty methods** — `createAutoRenewPaymentForSubscription()` is empty, `processPayment()` always returns `true`.
- **Payment-service `RazorpayGatewayProvider.createOrder()` generates fake order IDs** — it's a simulator but not clearly labeled.
- **No retry scheduler in payment-service.** The `nextRetryAt` and `retryCount` fields are set but nothing reads them to trigger retries.
- **No max retry limit.** `retryCount` increments indefinitely.
- **No idempotency key on payment creation.** Two identical `createOrder` requests create two payments.

### 7.3 Concurrency
- **No protection against concurrent renewal processing.** If multiple scheduler instances run (or the scheduler fires while still processing), the same subscription could be renewed twice.
- **No `SELECT FOR UPDATE` or optimistic locking** on subscription or payment state transitions.
- **Webhook idempotency check (`existsByEventId`) is not atomic** with the subsequent save — race condition allows duplicate processing under concurrent webhook delivery.

---

## 8. Operational Risks

| Risk | Impact | Current Mitigation |
|---|---|---|
| Running 7+ services locally | Extreme complexity, ~3GB RAM minimum | Docker Compose for infra only |
| Kafka required | All event publishing fails silently without Kafka | `publishEventSafely()` catches exceptions |
| Redis in docker-compose | Not used by any service code | None needed (dead config) |
| No CI/CD pipeline | No GitHub Actions workflow | None |
| No Dockerfile for any service | Cannot containerize | None |
| No `.dockerignore` | N/A (no Dockerfiles) | None |
| SQL debug logging in production config | Performance impact, log noise | None |
| `show-sql: true` in all services | Verbose, potential information leak | None |
| No health check for database connectivity | Actuator health configured but `show-details: always` exposes internals | None |
| Config Server uses `native` profile | Config files must be on local filesystem | Config is also in each service's `application.yml` (redundant) |

---

## 9. Deployment Assessment

### 9.1 Current Deployment Capability
- **Zero deployment capability.** No Dockerfiles, no CI pipeline, no cloud configuration.
- The frontend has a `vercel.json` suggesting Vercel deployment intent, but the backend has nothing.

### 9.2 Zero-Budget Hosting Feasibility
Running 7 Spring Boot services + Kafka + Zookeeper + PostgreSQL on free-tier hosting is **not feasible**. Free-tier PaaS providers (Render, Railway, Fly.io) typically offer:
- 1 free web service (Render) or very limited compute.
- Limited RAM (512MB typical) — insufficient for even one Spring Boot + Eureka + Kafka stack.
- No free managed Kafka.

**Recommendation:** Consolidate to the **modular monolith** architecture for deployment. A single Spring Boot application + free PostgreSQL (Neon, Supabase, Render) is achievable at zero cost.

---

## 10. Summary of Critical Findings

| Priority | Finding | Category |
|---|---|---|
| P0 | Toolchain mismatch — JDK 25 runtime fails Lombok compiler plugin; verified clean build & test under JDK 21 | Environment |
| P0 | Entire microservices work is uncommitted (32 deleted files, 15 untracked folders) | Git |
| P0 | Monolith has ALL security disabled | Security |
| P0 | JWT secret hardcoded in source code | Security |
| P0 | Database passwords hardcoded (`1`) | Security |
| P0 | Zero automated tests | Testing |
| P0 | No CI/CD pipeline | DevOps |
| P0 | No database migrations (uses ddl-auto: update) | Database |
| P1 | `Double` used for monetary values | Data Integrity |
| P1 | No subscription state machine — arbitrary transitions | Business Logic |
| P1 | Auto-renew scheduler calls empty methods | Business Logic |
| P1 | Payment-service JWT filter doesn't set roles → RBAC broken | Security |
| P1 | Internal APIs permitAll without network-level isolation | Security |
| P2 | No optimistic locking on any entity | Concurrency |
| P2 | No idempotency on payment creation | Reliability |
| P2 | No retry scheduler in payment-service | Business Logic |
| P2 | 7-service architecture impossible on free hosting | Deployment |
| P2 | JPA entities exposed directly as API responses | API Design |
| P2 | No rate limiting | Security |
| P2 | No pagination on list endpoints | Performance |
