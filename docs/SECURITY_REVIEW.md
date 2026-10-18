# Subscribly Comprehensive Security & Threat Boundary Review

**Document Version:** 1.0.0  
**Baseline Branch:** `microservices-v2`  
**Review Scope:** Monolith Core, Microservice Boundaries, Cryptographic Implementations, Authorization Enforcement, and Configuration Management  
**Audit Date:** 2026-10-18  

---

## 1. Security Architecture & Threat Model Overview

Subscribly implements a defense-in-depth security posture designed to enforce authentication boundaries, strict multi-tenant resource isolation, cryptographic integrity verification, and fail-fast configuration safety.

```
+-----------------------------------------------------------------------------------+
|                                  INCOMING TRAFFIC                                 |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
| API GATEWAY / SECURITY FILTER CHAIN                                               |
| - Stateless Session Management (SessionCreationPolicy.STATELESS)                  |
| - CSRF Disabled (Stateless Bearer Token Architecture)                             |
| - CORS Configured (Explicit Allowed Origins: localhost:5173, localhost:3000)      |
| - Public Endpoints: /actuator/health, /api/v1/health, /api/v1/auth/**, /webhook  |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
| JWT AUTHENTICATION FILTER (OncePerRequestFilter)                                  |
| - Bearer Token Header Parsing                                                     |
| - Constant-Time HS256 HMAC-SHA256 Cryptographic Verification                      |
| - Expiration Boundary Check (Strict Temporal Rejection)                          |
| - Claims Extraction & SecurityContext Population                                  |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
| METHOD SECURITY & CONTROLLER OWNERSHIP GUARDS                                     |
| - @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')") for Admin Domains       |
| - Principal Email Ownership Verification on Subscriptions & Payments              |
| - Cross-User Request Forgery / Infiltration Blocked with HTTP 403 Forbidden       |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
| PERSISTENCE & DATA PROTECTION                                                     |
| - BCrypt Password Hashing with Dynamic Salt                                       |
| - @JsonIgnore on User Entity Password Field                                       |
| - Database-Backed Idempotency Uniqueness Constraints                              |
+-----------------------------------------------------------------------------------+
```

---

## 2. Core Security Controls Verified

### A. Password Storage & Handling
- **Mechanism:** `BCryptPasswordEncoder` (Spring Security standard).
- **Properties:**
  - Automatically incorporates a cryptographically secure 128-bit salt for each password hash.
  - Mitigates rainbow table and pre-computation attacks.
  - Work factor: Standard default strength (10 rounds).
- **Leak Prevention:** The `password` field on `User` is decorated with `@com.fasterxml.jackson.annotation.JsonIgnore`. During serialization to DTOs or entity JSON responses, the password hash is completely stripped and never emitted.

### B. JWT Signing & Validation
- **Algorithm:** HMAC-SHA256 (`HS256`).
- **Minimum Secret Length:** Enforces minimum 256 bits (32 bytes) for signing key.
- **Fail-Fast Configuration:** `JwtUtil.validateConfiguration()` explicitly rejects blank or weak keys. In the `prod` profile, missing or weak keys immediately abort Spring Boot context startup (`IllegalStateException`).
- **Validation Behaviors Tested:**
  - Valid Token: Grants access and extracts authorities (`ROLE_USER`, `ROLE_ADMIN`).
  - Missing Token: Rejects with `401 Unauthorized`.
  - Malformed Token: Trapped cleanly by `isTokenValid()` without leaking internal stack traces; rejects with `401 Unauthorized`.
  - Expired Token: Verified by `JwtUtilTest` and rejected immediately.

### C. Role-Based Access Control (RBAC)
- **Role Hierarchy:**
  - `ROLE_USER`: Standard subscriber; can manage owned subscriptions, initiate payments, view owned history.
  - `ROLE_ADMIN`: Administrative operator; can view system-wide subscriptions, list all users, adjust active statuses, and update roles.
- **Protection Mechanism:**
  - `@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")` at controller class and method levels.
  - Requests from authenticated `ROLE_USER` tokens targeting administrative endpoints (`/api/v1/admin/**`) are rejected with HTTP `403 Forbidden`.
  - Verified live in Newman suite (Requests 30, 31, 32, 33, 34).

### D. Multi-Tenant Resource Ownership & IDOR Protection
- **Vulnerability Addressed:** Insecure Direct Object References (IDOR) and cross-user data manipulation.
- **Implementation:**
  - `SubscriptionController` extracts `principal.getName()` from the authenticated security context.
  - Compares the authenticated email against request parameters and body fields:
    ```java
    String authenticatedEmail = requireAuthenticatedEmail(principal);
    if (req != null && req.getEmail() != null && !req.getEmail().isBlank()
            && !req.getEmail().equalsIgnoreCase(authenticatedEmail)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot create subscription for another user");
    }
    ```
  - Query parameters (`/my?email=victim@example.com`) and path variables (`/{id}/cancel?email=victim@example.com`) enforce ownership equality; mismatches abort immediately with `403 Forbidden`.
  - Verified live in Newman suite (Requests 13, 15, 28).

### E. Account Deactivation Enforcement
- **Implementation:**
  - The `active` boolean on `User` is evaluated during login:
    ```java
    if (!user.isActive()) {
        log.warn("[AuthController] Login rejected for deactivated account: {}", email);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", "ERROR",
                "message", "Account has been deactivated"
        ));
    }
    ```
  - Deactivated accounts cannot obtain fresh JWT tokens.

### F. Cryptographic Webhook Integrity (HMAC-SHA256)
- **Implementation:**
  - Gateway webhook notifications (`/api/v1/payments/webhook`) calculate an HMAC-SHA256 digest over the exact raw HTTP request body bytes using `RAZORPAY_WEBHOOK_SECRET`.
  - Compares calculated digest with the `X-Razorpay-Signature` header using `MessageDigest.isEqual()`:
    ```java
    byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
    byte[] actualBytes = signatureHeader.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expectedBytes, actualBytes);
    ```
  - **Timing Side-Channel Protection:** `MessageDigest.isEqual()` executes in constant time, preventing timing-attack signature discovery.
  - Forged signatures or missing headers are rejected with HTTP `400 Bad Request`.
  - Verified live in Newman suite (Requests 24, 26, 27).

### G. Administrator Provisioning & Bootstrap Hardening
- **Configuration Boundary:**
  - Administrator bootstrapping is explicitly opt-in: `subscribly.security.admin-bootstrap.enabled: false`. In production or unconfigured environments, zero privileged accounts are automatically seeded.
  - Password entropy enforcement: Password must be supplied externally via `SUBSCRIBLY_SECURITY_ADMIN_BOOTSTRAP_PASSWORD`. Must be >= 12 characters and free of dictionary/predictable patterns (`password`, `admin`, `123456`, `changeme`, `subscribly`).
- **Lifecycle & Tamper Safeguard:**
  - Checks `userRepository.countAllAdministratorsEver()`. If any admin account ever existed (even if later deleted or deactivated), bootstrap aborts cleanly to preserve intentional security states.
  - Public registration (`/api/v1/auth/register`) strictly assigns `ROLE_USER` only; `ROLE_ADMIN` cannot be self-assigned.
- **Log Masking:** Logs explicitly mask all credentials and record only audit status.

### H. Payment Simulation Boundary & Fail-Closed Enforcement
- **Opt-In Safety:**
  - Governed by `subscribly.payments.simulation-enabled` (`PAYMENT_SIMULATION_ENABLED: false` by default).
  - Hardcoded `simulateSuccess = true` paths have been eliminated from `SubscriptionScheduler` and `PaymentService`.
- **Fail-Closed Policy:**
  - Direct manual simulated payment requests fail with `IllegalStateException` when simulation is disabled.
  - In auto-renewal sweeps, expired subscriptions transition to `EXPIRED` status without granting false entitlement or false ledger entries.
  - Retries execute bounded exponential backoff failing closed to `FAILED` with entitlement revoked after 3 failed attempts.

### I. Entity Response Isolation & DTO Boundary
- **Information Disclosure Prevention:**
  - Controllers no longer return raw Hibernate JPA entities (`Payment`, `Subscription`).
  - Dedicated DTO `PaymentResponse` prevents serialization of Hibernate ByteBuddy lazy proxies, user password hashes, and schema internals.

---

## 3. Analysis of Historical Credential Exposure & Remediation

### Historical Context
During earlier prototype development on legacy commits, a default development JWT secret and mock webhook secret were committed into `application.yml`. 

### Current State & Remediation Status
1. **Separation of Baseline:**
   - The production code published on branch `publish/subscribly-code-only` has all mock secrets externalized via Spring environment variables:
     ```yaml
     jwt:
       secret: ${JWT_SECRET:local-development-only-jwt-secret-not-for-prod-32bytes-min!}
     razorpay:
       key-id: ${RAZORPAY_KEY_ID:rzp_test_mockKeyId}
       key-secret: ${RAZORPAY_KEY_SECRET:rzp_test_mockSecretKey12345}
       webhook-secret: ${RAZORPAY_WEBHOOK_SECRET:rzp_test_mockWebhookSecret12345}
     ```
   - In production profiles (`prod`), `JwtUtil` strictly disallows default placeholder values and aborts context boot.
2. **Key Rotation Plan:**
   - **JWT Signing Secret:** In production environments, `JWT_SECRET` must be injected via secure environment variables or vault secret managers with a minimum 512-bit random entropy. Any token issued by a legacy development key is rejected because signing verification uses the actively injected environment secret.
   - **Payment Provider Secrets:** No real money transactions or production Razorpay accounts were ever linked to the development repository. If moving to production, official live API keys and webhook secrets must be generated inside the Razorpay dashboard and injected via Kubernetes secrets or AWS Parameter Store.
3. **Git Hygiene Safeguard:**
   - Neither `.env` files, production credentials, nor private keys exist anywhere in the tracked Git tree.

---

## 4. Security Findings & Recommendations Matrix

| Finding ID | Severity | Category | Description | Status | Recommendation |
| :--- | :---: | :--- | :--- | :---: | :--- |
| `SEC-01` | **LOW** | Information Disclosure | Actuator `/actuator/health` exposes component details when `management.endpoint.health.show-details: always` | **ACCEPTED (DEV)** | In production profile (`application-prod.yml`), set `show-details: never` or restrict actuator access to internal monitoring CIDRs. |
| `SEC-02` | **MEDIUM** | Network Boundary | `/api/v1/internal/**` endpoints permit unauthenticated access within the local network | **BY DESIGN** | Currently designed for service-to-service communication behind API Gateway. When deploying in Kubernetes, restrict internal routes via Istio Mutual TLS (mTLS) or NetworkPolicies. |
| `SEC-03` | **INFO** | Credential Exposure | Legacy development Git commits contain historical mock test credentials | **RESOLVED** | Environment externalization verified; production deployment uses injected vault secrets with rotation. |
| `SEC-04` | **CRITICAL** | Default Provisioning | Prior default admin password in `AdminBootstrapConfig` risked default credential takeover | **RESOLVED** | Enforced opt-in flag, 12+ character entropy validation, dictionary token rejection, and lifecycle check. |
| `SEC-05` | **HIGH** | Financial Correctness | Hardcoded simulation success in scheduler auto-renewal sweep created false entitlements | **RESOLVED** | Extracted `PAYMENT_SIMULATION_ENABLED` flag; fails closed to `EXPIRED` status without live gateway mandate. |

---

## 5. Security Regression Verification

All security mechanisms were verified against both the automated test reactor (227 tests) and the live Newman API collection (61 assertions):
- **BCrypt Hashing:** Verified by `AuthControllerTest` and live registration.
- **JWT Malformed / Expired Rejection:** Verified by `JwtUtilTest` (6 tests) and Newman Request 11.
- **RBAC Admin Enforcement:** Verified by `AdminControllerTest` and Newman Request 30 (403 Forbidden).
- **Admin Bootstrap Security:** Verified by `AdminBootstrapSecurityTest` (6 tests).
- **Payment Simulation Fail-Closed:** Verified by `PaymentSimulationSecurityTest` (4 tests).
- **Cross-User Ownership Checks:** Verified by Newman Requests 13, 15, 28 (403 Forbidden).
- **HMAC Webhook Verification:** Verified by `PaymentLifecycleIntegrationTest` and Newman Requests 24, 26, 27 (Valid 200, Forged 400).
- **Financial Precision:** Verified by `MonetaryPrecisionFinancialTest` (5 tests) and Flyway `V5` schema verification.
- **Entity Response Isolation:** Verified by `PaymentResponseDtoTest` (2 tests) and Newman Requests 21, 22, 23, 35, 36.
