# Subscribly Defect Log & Remediation History

**Document Version:** 1.1.0  
**Baseline Branch:** `microservices-v2`  
**Execution Environment:** Windows, Java 21 (Azul Zulu 21.0.7), PostgreSQL 18.1  
**Last Updated:** 2026-10-18T20:58:00+05:30  

---

## Defect Summary

| Defect ID | Severity | Component | Summary | Status | Verified By |
| :--- | :---: | :--- | :--- | :---: | :--- |
| `DEF-01` | **CRITICAL** | `payment-service`, `common-lib` | Bytecode `ClassFormatError: Invalid method Code length 0` from preview JDK target classes | **RESOLVED** | Full Maven reactor (`BUILD SUCCESS`, 205 tests) |
| `DEF-02` | **HIGH** | `monolith` (Entity Serialization) | `InvalidDefinitionException` on Hibernate ByteBuddy lazy proxy serialization in `Payment`, `Subscription`, and `User` | **RESOLVED** | Newman requests 21, 22, 23, 35, 36 (100% Pass) |
| `DEF-03` | **MEDIUM** | `monolith` (`SubscriptionRequest`) | Missing Jackson `@JsonAlias` and unhandled PostgreSQL NOT NULL constraint on `plan` / `price` | **RESOLVED** | Newman request 12 & `SubscriptionServiceTest` (100% Pass) |
| `DEF-04` | **HIGH** | `monolith` (Admin Bootstrapping) | Initial administrator absence on fresh databases prevented admin login and administrative operations | **RESOLVED** | Newman requests 31, 32, 33, 34 & `SubscriblyApplicationTests` (100% Pass) |

---

## Detailed Defect Reports

### DEF-01: Java 25 Preview Bytecode Format Incompatibility

- **Date Discovered:** 2026-10-18
- **Affected Artifacts:**
  - `common-lib/target/classes`
  - `payment-service/target/test-classes`
- **Severity:** **CRITICAL** (Blocked build and test execution for microservices)
- **Symptom:**
  ```text
  java.lang.ClassFormatError: Invalid method Code length 0 in class file com/subscribly/paymentservice/service/PaymentServiceTest
      at java.base/java.lang.ClassLoader.defineClass1(Native Method)
  ```
- **Root Cause Analysis:**
  Target classfiles in `common-lib/target` and `payment-service/target` were previously compiled by an IDE workspace utilizing early-access / preview Java 25 bytecode features (`classfile version 69.65535`), causing the standard Java 21 virtual machine (`azul-21.0.7`, `classfile version 65.0`) to reject class definitions during classloading.
- **Remediation:**
  Recompiled and re-tested using Maven with explicit Java 21 home (`$env:JAVA_HOME = "C:\Users\HIMANSHU\.jdks\azul-21.0.7"`) and maven compiler release target set to `21`.
- **Regression Verification:**
  - `.\mvnw.cmd test` executed across all 10 modules: **205 passed, 0 failures, 0 errors, 0 skipped**.
  - `payment-service` test suite: 23 passed in 3.015s.

---

### DEF-02: Hibernate Proxy Jackson Serialization Failure (ByteBuddyInterceptor)

- **Date Discovered:** 2026-10-18
- **Affected Artifacts:**
  - `monolith/src/main/java/com/subscribly/entity/User.java`
  - `monolith/src/main/java/com/subscribly/entity/Subscription.java`
  - `monolith/src/main/java/com/subscribly/entity/Payment.java`
  - `monolith/src/main/resources/application.yml`
- **Severity:** **HIGH** (Caused runtime HTTP 500 Internal Server Error on payment and internal lookup endpoints)
- **Symptom:**
  ```text
  org.springframework.http.converter.HttpMessageConversionException: Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]
  com.fasterxml.jackson.databind.exc.InvalidDefinitionException: No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS) (through reference chain: com.subscribly.entity.Subscription["user"]->com.subscribly.entity.User$HibernateProxy$Z1nDIXB2["hibernateLazyInitializer"])
  ```
- **Root Cause Analysis:**
  Endpoints `/api/v1/payments/pay/{id}`, `/api/v1/payments/my`, `/api/v1/internal/subscriptions/{id}`, and `/api/v1/internal/payments/{id}` return JPA entity objects (`Payment`, `Subscription`) directly. Because associations are configured with `fetch = FetchType.LAZY`, Hibernate injects runtime ByteBuddy proxies (`ByteBuddyInterceptor`). Jackson attempted to introspect internal proxy state (`hibernateLazyInitializer`), which lacks getter methods, causing `InvalidDefinitionException` when `SerializationFeature.FAIL_ON_EMPTY_BEANS` is enabled.
- **Remediation:**
  1. Configured `spring.jackson.serialization.fail-on-empty-beans: false` in `monolith/src/main/resources/application.yml`.
  2. Decorated `User`, `Subscription`, and `Payment` entity classes with `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})`.
  3. Decorated relational fields in `Subscription` (`user`) and `Payment` (`subscription`, `user`) with `@JsonIgnoreProperties` to break bidirectional circular serialization paths and prevent proxy introspection.
- **Regression Verification:**
  - Live Newman verification:
    - `05 - Payments / 21 - Simulated Manual Payment (Positive)`: **200 OK**
    - `05 - Payments / 22 - Simulated Manual Payment Idempotent Replay (Positive)`: **200 OK**
    - `05 - Payments / 23 - Get My Payments (Positive)`: **200 OK**
    - `09 - Internal Service Routing / 35 - Internal Subscription Lookup (Positive)`: **200 OK**
    - `09 - Internal Service Routing / 36 - Internal Payment Lookup (Positive)`: **200 OK**
  - Full Maven reactor tests: **205 passed, 0 failures**.

---

### DEF-03: Subscription Creation 500 Error on Client Field Mismatch & Unhandled DB Constraint

- **Date Discovered:** 2026-10-18
- **Affected Artifacts:**
  - `monolith/src/main/java/com/subscribly/dto/SubscriptionRequest.java`
  - `monolith/src/main/java/com/subscribly/controller/SubscriptionController.java`
  - `monolith/src/main/java/com/subscribly/service/SubscriptionService.java`
- **Severity:** **MEDIUM** (Caused runtime HTTP 500 when clients submitted `planName` / `amount` or omitted optional plan/price)
- **Symptom:**
  ```text
  org.postgresql.util.PSQLException: ERROR: null value in column "plan" of relation "subscriptions" violates not-null constraint
    Detail: Failing row contains (1, 6, null, null, MONTHLY, ACTIVE, 2026-10-18, 2026-11-18, ..., t, 0).
  ```
- **Root Cause Analysis:**
  `SubscriptionRequest` fields were named `plan` and `price`. When API clients or frontend scripts submitted `planName` or `amount`, Jackson left `plan` and `price` as `null`. When `SubscriptionService.createSubscription` persisted the entity, PostgreSQL rejected the insert with a NOT NULL constraint violation, producing an unhandled 500 error instead of a graceful fallback or 400 validation error. Furthermore, `SubscriptionController` only mapped `/create`, returning 404 if called at root `/api/v1/subscription`.
- **Remediation:**
  1. Added `@JsonProperty("plan") @JsonAlias({"planName"})` to `plan` and `@JsonProperty("price") @JsonAlias({"amount"})` to `price` in `SubscriptionRequest.java`.
  2. Updated `SubscriptionController.java` mapping to `@PostMapping({"", "/create"})` to match `subscription-service` parity and frontend Axios client contract.
  3. Added safe fallback default values (`"PRO"` for plan, `49.99` for price) in `SubscriptionService.createSubscription` if omitted.
- **Regression Verification:**
  - Live Newman verification:
    - `04 - Subscriptions / 12 - Create Subscription (Positive)`: **200 OK**
    - `04 - Subscriptions / 13 - Create Subscription Cross-User Disallowed (Negative)`: **403 Forbidden**
  - Maven reactor unit tests: `SubscriptionServiceTest` (10 tests) and `SubscriptionControllerTest` (8 tests) **100% Pass**.

---

### DEF-04: Initial Administrator Account Absence on Fresh Database Deployments

- **Date Discovered:** 2026-10-18
- **Affected Artifacts:**
  - `monolith/src/main/java/com/subscribly/config/AdminBootstrapConfig.java`
- **Severity:** **HIGH** (Admin login and management operations returned HTTP 401 Unauthorized on fresh database deployments where no prior admin record existed)
- **Symptom:**
  ```text
  POST /api/v1/auth/login with {"email": "admin1@example.com"} returned HTTP 401 Unauthorized
  GET /api/v1/admin/subscriptions returned HTTP 401 Unauthorized
  GET /api/v1/admin/users returned HTTP 401 Unauthorized
  ```
- **Root Cause Analysis:**
  Flyway migration `V4__persist_user_active_status.sql` seeds `ROLE_ADMIN` and `ROLE_USER` into the `roles` table, but no administrator user record was provisioned in `users`. Consequently, in clean database deployments or truncated disposable test environments, no user held `ROLE_ADMIN`, and no public API endpoint permits self-registration with administrative privileges. This caused admin login to fail with HTTP 401, cascading failures to all admin endpoints.
- **Remediation:**
  Implemented `AdminBootstrapConfig` as a Spring `CommandLineRunner`. Uses `TransactionTemplate` to check whether `userRepository.countActiveAdministrators() == 0`, automatically provisioning an initial administrator account (`admin1@example.com` with BCrypt-hashed password and `ROLE_ADMIN` + `ROLE_USER`) in an isolated transaction. The runner safely catches exceptions if tables are not yet initialized or during unmigrated test runs, preventing application context rollback failures.
- **Regression Verification:**
  - Live Newman verification:
    - `08 - Administration / 31 - Admin Login (Positive)`: **200 OK**
    - `08 - Administration / 32 - Admin Get Subscriptions (Positive)`: **200 OK**
    - `08 - Administration / 33 - Admin Get All Users (Positive)`: **200 OK**
    - `08 - Administration / 34 - Admin Update User Status (Positive)`: **200 OK**
  - Full Maven reactor clean test: **205 passed, 0 failures, 0 errors, 0 skipped**.

---

## 3. Verification Defect Metrics

```
Total Defects Discovered: 4
Total Defects Remediated: 4
Unresolved Defects: 0
Regression Test Pass Rate: 100% (205/205 Maven, 61/61 Newman assertions)
```
