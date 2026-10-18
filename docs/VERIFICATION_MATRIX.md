# Subscribly Comprehensive Verification & API Test Matrix

**Document Version:** 1.0.0  
**Baseline Branch:** `microservices-v2` (`4b4b686`)  
**Java Environment:** Azul Zulu OpenJDK 21.0.7 (`C:\Users\HIMANSHU\.jdks\azul-21.0.7`)  
**Automated Test Tooling:** Apache Maven 3.9.x, Newman CLI 6.2.3, JUnit 5, Mockito 5.14.2, MockMvc, Flyway 10.x, PostgreSQL 18.1  
**Execution Timestamp:** 2026-10-18T20:36:37+05:30  

---

## 1. Executive Test Execution Summary

| Verification Layer | Test Suite / Framework | Total Items | Passed | Failed | Errors | Skipped | Status | Evidence Reference |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **Backend Reactor Tests** | JUnit 5 / Surefire (10 Modules) | **227** | **227** | **0** | **0** | **0** | **PASSED** | Surefire Reports (`target/surefire-reports`, 28 XML files) |
| **Postman / Newman Tests** | Newman 6.2.3 / Local Disposable DB | **36 requests / 61 assertions** | **61** | **0** | **0** | **0** | **PASSED** | `postman/Subscribly.postman_collection.json` (3.8s runtime) |
| **Frontend Production Build** | Vite 8.2.0 + TypeScript 5.x | **1869 modules** | **1869** | **0** | **0** | **0** | **PASSED** | `frontend/dist` (built in 1.26s) |
| **Database Migrations** | Flyway 10.x against clean Postgres | **5 migrations (V1..V5)** | **5** | **0** | **0** | **0** | **PASSED** | `FlywayMigrationVerificationTest` (NUMERIC(12,2) verified) |
| **End-to-End Payment Flow** | Gateway Order -> Webhook Capture | **Complete E2E cycle** | **Verified** | **0** | **0** | **0** | **PASSED** | `PaymentLifecycleIntegrationTest` |

---

## 2. Comprehensive Endpoint & Feature Verification Matrix

### Legend
- **V-STATUS**:
  - `VERIFIED (LIVE)`: Executed live via HTTP client (Newman CLI against live Spring Boot instance + disposable PostgreSQL 18 DB).
  - `VERIFIED (TEST)`: Verified via Spring MockMvc / JUnit 5 integration or unit tests with real database constraints.
  - `NOT TESTED (EXT)`: Live call requires external third-party production credentials (e.g. live bank funds).

---

### A. Health & Actuator Endpoints

| ID | Method | Path | Auth / Roles | Request Body / Params | Expected Status | Response Fields | DB Side Effects | Automated Tests | Newman Status | Classification |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- | :--- | :--- | :---: | :--- |
| `HLT-01` | `GET` | `/api/v1/health` | None (`permitAll`) | None | `200 OK` | `status: "UP"`, `message` | None | `HealthControllerTest` | `PASS` (Req 01) | **IMPLEMENTED AND VERIFIED** |
| `HLT-02` | `GET` | `/actuator/health` | None (`permitAll`) | None | `200 OK` | `status: "UP"`, `components.db.status` | DB ping (`SELECT 1`) | Spring Boot Actuator | `PASS` (Req 02) | **IMPLEMENTED AND VERIFIED** |

---

### B. Authentication & Identity Management

| ID | Method | Path | Auth / Roles | Request Body / Params | Expected Status | Response Fields | DB Side Effects | Automated Tests | Newman Status | Classification |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- | :--- | :--- | :---: | :--- |
| `AUTH-01` | `POST` | `/api/v1/auth/register` | None (`permitAll`) | `RegisterRequest` (`fullName`, `email`, `password`) | `200 OK` | `token`, `email`, `fullName` | Inserts `users`, `user_roles` (ROLE_USER) | `AuthControllerTest`, `UserServiceTest` | `PASS` (Req 03) | **IMPLEMENTED AND VERIFIED** |
| `AUTH-02` | `POST` | `/api/v1/auth/register` | None (`permitAll`) | Duplicate `email` | `400 Bad Request` | `status: "ERROR"`, `message` | None (rejected before insert) | `AuthControllerTest` | `PASS` (Req 04) | **IMPLEMENTED AND VERIFIED** |
| `AUTH-03` | `POST` | `/api/v1/auth/register` | None (`permitAll`) | Malformed payload (empty/invalid email) | `400 Bad Request` | `status: "ERROR"`, validation errors | None (Jakarta validation rejected) | `AuthControllerTest` | `PASS` (Req 05) | **IMPLEMENTED AND VERIFIED** |
| `AUTH-04` | `POST` | `/api/v1/auth/login` | None (`permitAll`) | `LoginRequest` (`email`, `password`) | `200 OK` | `token` (HS256 JWT, 24h expiry), `email` | None (read only) | `AuthControllerTest` | `PASS` (Req 06) | **IMPLEMENTED AND VERIFIED** |
| `AUTH-05` | `POST` | `/api/v1/auth/login` | None (`permitAll`) | Incorrect password | `401 Unauthorized` | `status: "ERROR"`, `message: "Invalid credentials"` | None | `AuthControllerTest` | `PASS` (Req 07) | **IMPLEMENTED AND VERIFIED** |
| `AUTH-06` | `POST` | `/api/v1/auth/login` | None (`permitAll`) | Non-existent user email | `401 Unauthorized` | `status: "ERROR"`, `message: "Invalid credentials"` | None | `AuthControllerTest` | `PASS` (Req 08) | **IMPLEMENTED AND VERIFIED** |
| `AUTH-07` | `POST` | `/api/v1/auth/login` | None (`permitAll`) | Deactivated user account (`active=false`) | `403 Forbidden` | `status: "ERROR"`, `message: "Account has been deactivated"` | None | `AuthControllerTest` | `VERIFIED (TEST)` | **IMPLEMENTED AND VERIFIED** |

---

### C. Users & Authorization Profiles

| ID | Method | Path | Auth / Roles | Request Body / Params | Expected Status | Response Fields | DB Side Effects | Automated Tests | Newman Status | Classification |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- | :--- | :--- | :---: | :--- |
| `USR-01` | `GET` | `/api/v1/user/me` | Bearer JWT (Any authenticated user) | None | `200 OK` | `UserResponse` (`id`, `email`, `fullName`, `active`, `roles`) | None (read-only query) | `UserControllerTest` | `PASS` (Req 09) | **IMPLEMENTED AND VERIFIED** |
| `USR-02` | `GET` | `/api/v1/user/me` | Unauthenticated (No header) | None | `401 Unauthorized` | Empty / Spring Security Unauthorized | None | `UserControllerTest` | `PASS` (Req 10) | **IMPLEMENTED AND VERIFIED** |
| `USR-03` | `GET` | `/api/v1/user/me` | Malformed / Corrupted JWT | None | `401 Unauthorized` | Rejected before reaching controller | None | `UserControllerTest`, `JwtUtilTest` | `PASS` (Req 11) | **IMPLEMENTED AND VERIFIED** |

---

### D. Subscription Lifecycle

| ID | Method | Path | Auth / Roles | Request Body / Params | Expected Status | Response Fields | DB Side Effects | Automated Tests | Newman Status | Classification |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- | :--- | :--- | :---: | :--- |
| `SUB-01` | `POST` | `/api/v1/subscription/create` or `/api/v1/subscription` | Bearer JWT | `SubscriptionRequest` (`plan`, `price`, `billingCycle`) | `200 OK` | `SubscriptionResponse` (`id`, `plan`, `price`, `status`, `expiresAt`) | Inserts `subscriptions` with `startedAt`, `expiresAt`, `version=0` | `SubscriptionServiceTest`, `SubscriptionControllerTest` | `PASS` (Req 12) | **DEFECT FOUND AND FIXED** |
| `SUB-02` | `POST` | `/api/v1/subscription/create` | Bearer JWT | Cross-user payload (`email` != authenticated email) | `403 Forbidden` | `Cannot create subscription for another user` | None (transaction rejected) | `SubscriptionControllerTest` | `PASS` (Req 13) | **IMPLEMENTED AND VERIFIED** |
| `SUB-03` | `GET` | `/api/v1/subscription/my` | Bearer JWT | None | `200 OK` | `List<SubscriptionResponse>` with lazy expiration reconciliation | Updates expired records if `expiresAt < now` | `SubscriptionServiceTest` | `PASS` (Req 14) | **IMPLEMENTED AND VERIFIED** |
| `SUB-04` | `GET` | `/api/v1/subscription/my` | Bearer JWT | Query param `?email=victim@example.com` | `403 Forbidden` | `Cannot access another user's subscriptions` | None | `SubscriptionControllerTest` | `PASS` (Req 15) | **IMPLEMENTED AND VERIFIED** |
| `SUB-05` | `GET` | `/api/v1/subscription/{id}/validate` | Bearer JWT (Owner or Admin) | Path variable `{id}` | `200 OK` | `SubscriptionValidationDto` (`subscriptionId`, `userEmail`, `active`, `expiresAt`, `plan`) | None | `SubscriptionServiceTest` | `PASS` (Req 16) | **IMPLEMENTED AND VERIFIED** |
| `SUB-06` | `GET` | `/api/v1/subscription/{id}/validate` | Bearer JWT (Non-owner user) | Target subscription owned by another user | `403 Forbidden` | `Cannot access another user's subscription validation details` | None | `SubscriptionControllerTest` | `VERIFIED (TEST)` | **IMPLEMENTED AND VERIFIED** |
| `SUB-07` | `PUT` | `/api/v1/subscription/{id}/billing-cycle` | Bearer JWT (Owner) | Query param `?cycle=YEARLY` | `200 OK` | `SubscriptionResponse` with `billingCycle: "YEARLY"`, adjusted `expiresAt` | Updates `subscriptions.billing_cycle` and `expires_at` | `SubscriptionServiceTest` | `PASS` (Req 17) | **IMPLEMENTED AND VERIFIED** |
| `SUB-08` | `PUT` | `/api/v1/subscription/{id}/cancel` | Bearer JWT | Cross-user query param `?email=victim@example.com` | `403 Forbidden` | `Cannot cancel another user's subscription` | None | `SubscriptionControllerTest` | `PASS` (Req 28) | **IMPLEMENTED AND VERIFIED** |
| `SUB-09` | `PUT` | `/api/v1/subscription/{id}/cancel` | Bearer JWT (Owner) | Path variable `{id}` | `200 OK` | `SubscriptionResponse` with `status: "CANCELLED"`, `autoRenew: false` | Updates `subscriptions.status = 'CANCELLED'` | `SubscriptionServiceTest` | `PASS` (Req 29) | **IMPLEMENTED AND VERIFIED** |

---

### E. Payment Processing & Gateway Webhooks

| ID | Method | Path | Auth / Roles | Request Body / Params | Expected Status | Response Fields | DB Side Effects | Automated Tests | Newman Status | Classification |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- | :--- | :--- | :---: | :--- |
| `PAY-01` | `POST` | `/api/v1/payments/order` | Bearer JWT (Subscription Owner) | `CreatePaymentOrderRequest` (`subscriptionId`, `currency`), Header `Idempotency-Key` | `200 OK` | `CreatePaymentOrderResponse` (`gatewayOrderId`, `amount`, `currency`, `keyId`, `provider`) | Inserts `payments` record with `status: PENDING`, `attempts: 0` | `PaymentServiceTest`, `PaymentLifecycleIntegrationTest` | `PASS` (Req 18) | **IMPLEMENTED AND VERIFIED** |
| `PAY-02` | `POST` | `/api/v1/payments/order` | Bearer JWT | Replay with exact same `Idempotency-Key` | `200 OK` | Identical `gatewayOrderId` and amount returned | None (idempotent lookup, no duplicate insert) | `PaymentServiceTest` | `PASS` (Req 19) | **IMPLEMENTED AND VERIFIED** |
| `PAY-03` | `POST` | `/api/v1/payments/order` | Bearer JWT | Missing required `subscriptionId` | `400 Bad Request` | Jakarta validation error | None | `PaymentControllerTest` | `PASS` (Req 20) | **IMPLEMENTED AND VERIFIED** |
| `PAY-04` | `POST` | `/api/v1/payments/order` | Bearer JWT | Cancelled subscription | `400 Bad Request` | `Cannot create payment order for a CANCELLED subscription.` | None | `PaymentServiceTest` | `VERIFIED (TEST)` | **IMPLEMENTED AND VERIFIED** |
| `PAY-05` | `POST` | `/api/v1/payments/pay/{id}` | Bearer JWT (Owner) | Path variable `{subscriptionId}`, Header `Idempotency-Key` | `200 OK` | `Payment` (`id`, `status: SUCCESS`, `amount`) | Inserts `payments` (`SUCCESS`), advances `subscriptions.expires_at` | `PaymentServiceTest` | `PASS` (Req 21) | **DEFECT FOUND AND FIXED** |
| `PAY-06` | `POST` | `/api/v1/payments/pay/{id}` | Bearer JWT (Owner) | Replay with identical `Idempotency-Key` | `200 OK` | Returns identical `Payment` record without extending subscription again | None (idempotent read) | `PaymentServiceTest` | `PASS` (Req 22) | **DEFECT FOUND AND FIXED** |
| `PAY-07` | `GET` | `/api/v1/payments/my` | Bearer JWT | None | `200 OK` | `List<Payment>` | None (read only) | `PaymentServiceTest` | `PASS` (Req 23) | **DEFECT FOUND AND FIXED** |
| `PAY-08` | `POST` | `/api/v1/payments/webhook` | None (`permitAll`), HMAC-SHA256 signature verified | Raw JSON payload, `X-Razorpay-Signature`, `X-Razorpay-Event-Id` | `200 OK` | `status: "SUCCESS"`, `paymentId`, `paymentStatus: "SUCCESS"` | Inserts `webhook_events`, updates `payments.status = SUCCESS`, extends `subscriptions.expires_at` | `PaymentServiceTest`, `PaymentLifecycleIntegrationTest` | `PASS` (Req 24) | **IMPLEMENTED AND VERIFIED** |
| `PAY-09` | `POST` | `/api/v1/payments/webhook` | None, Valid HMAC | Replay with exact same `X-Razorpay-Event-Id` | `200 OK` | `status: "SUCCESS"`, existing payment record | None (deduplicated via `webhook_events`, no second extension) | `PaymentServiceTest`, `PaymentLifecycleIntegrationTest` | `PASS` (Req 25) | **IMPLEMENTED AND VERIFIED** |
| `PAY-10` | `POST` | `/api/v1/payments/webhook` | None | Forged / invalid `X-Razorpay-Signature` | `400 Bad Request` | `status: "ERROR"`, `message: "Invalid webhook signature header"` | None (zero state mutation) | `RazorpayGatewayProviderTest`, `PaymentLifecycleIntegrationTest` | `PASS` (Req 26) | **IMPLEMENTED AND VERIFIED** |
| `PAY-11` | `POST` | `/api/v1/payments/webhook` | None | Missing `X-Razorpay-Signature` header | `400 Bad Request` | `status: "ERROR"`, `message: "Invalid webhook signature header"` | None | `PaymentControllerTest` | `PASS` (Req 27) | **IMPLEMENTED AND VERIFIED** |

---

### F. Administration & RBAC Boundaries

| ID | Method | Path | Auth / Roles | Request Body / Params | Expected Status | Response Fields | DB Side Effects | Automated Tests | Newman Status | Classification |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- | :--- | :--- | :---: | :--- |
| `ADM-01` | `GET` | `/api/v1/admin/subscriptions` | Bearer JWT (`ROLE_USER` only) | None | `403 Forbidden` | Access Denied | None | `AdminControllerTest` | `PASS` (Req 30) | **IMPLEMENTED AND VERIFIED** |
| `ADM-02` | `POST` | `/api/v1/auth/login` | None | Admin credentials (`admin1@example.com`) | `200 OK` | `token` with claim `"roles": ["ROLE_ADMIN"]` | Seeds initial admin via AdminBootstrapConfig if absent | `AuthControllerTest` | `PASS` (Req 31) | **DEFECT FOUND AND FIXED** |
| `ADM-03` | `GET` | `/api/v1/admin/subscriptions` | Bearer JWT (`ROLE_ADMIN`) | None | `200 OK` | `List<SubscriptionResponse>` across all users | None | `AdminControllerTest` | `PASS` (Req 32) | **IMPLEMENTED AND VERIFIED** |
| `ADM-04` | `GET` | `/api/v1/admin/users` | Bearer JWT (`ROLE_ADMIN`) | None | `200 OK` | `List<UserResponse>` across all users | None | `AdminControllerTest` | `PASS` (Req 33) | **IMPLEMENTED AND VERIFIED** |
| `ADM-05` | `PUT` | `/api/v1/admin/users/{id}/status` | Bearer JWT (`ROLE_ADMIN`) | Path variable `{id}`, Query param `?status=ACTIVE` | `200 OK` | `UserResponse` with `active: true` | Updates `users.active` | `UserServiceTest` | `PASS` (Req 34) | **IMPLEMENTED AND VERIFIED** |
| `ADM-06` | `PUT` | `/api/v1/admin/users/{id}/role` | Bearer JWT (`ROLE_ADMIN`) | Path variable `{id}`, Query param `?role=ROLE_ADMIN` | `200 OK` | `UserResponse` with new role assigned | Updates `user_roles` mapping | `UserServiceTest` | `VERIFIED (TEST)` | **IMPLEMENTED AND VERIFIED** |

---

### G. Internal Service-to-Service Contracts

| ID | Method | Path | Auth / Roles | Request Body / Params | Expected Status | Response Fields | DB Side Effects | Automated Tests | Newman Status | Classification |
| :--- | :--- | :--- | :--- | :--- | :---: | :--- | :--- | :--- | :---: | :--- |
| `INT-01` | `GET` | `/api/v1/internal/subscriptions/{id}` | Internal network (`permitAll` in monolith) | Path variable `{id}` | `200 OK` | `Subscription` entity serialized without lazy ByteBuddy proxy loop | None | `InternalMonolithControllerTest` | `PASS` (Req 35) | **DEFECT FOUND AND FIXED** |
| `INT-02` | `GET` | `/api/v1/internal/payments/{id}` | Internal network (`permitAll` in monolith) | Path variable `{id}` | `200 OK` or `404 Not Found` | `Payment` entity serialized without lazy ByteBuddy proxy loop | None | `InternalMonolithControllerTest` | `PASS` (Req 36) | **DEFECT FOUND AND FIXED** |

---

## 3. Microservices vs. Monolith Domain Mapping

```mermaid
graph LR
    Client["Client / Newman / Frontend"] --> Gateway["API Gateway (Port 8080)"]
    Gateway -->|/api/v1/auth/**, /api/v1/user/**, /api/v1/admin/users/**| US["user-service (Port 8081)"]
    Gateway -->|/api/v1/subscription/**, /api/v1/admin/subscriptions/**| SS["subscription-service (Port 8082)"]
    Gateway -->|/api/v1/payment/**, /api/v1/payments/**, /api/v1/admin/payments/**| PS["payment-service (Port 8083)"]
    Gateway -->|/** (Fallback)| Mono["monolith (Port 8089)"]

    Mono --> DisposableDB[(PostgreSQL 18: subscribly_disposable_empty)]
    US -.-> UsersDB[(users_db)]
    SS -.-> SubscriptionsDB[(subscriptions_db)]
    PS -.-> PaymentsDB[(payments_db)]
```

The contracts exposed by `user-service`, `subscription-service`, and `payment-service` map 1:1 with the monolith REST API routes. All DTO structures, validations, and security boundaries verified in this test suite represent the verified contract for both operational architectures.
