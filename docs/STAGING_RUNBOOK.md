# Subscribly — Staging Runbook

## 1. Supported Deployment Mode & Architecture

### Target: Standalone Monolith (`monolith/`)
The **only** deployment mode supported for staging in this repository is the **Standalone Monolith**.

### Explicit Microservices Limitations
The microservice modules (`user-service`, `subscription-service`, `payment-service`, `api-gateway`, `discovery-server`, `config-server`) are in an incomplete extraction state and **cannot** be deployed safely to staging:
- **No Database Migrations**: The microservice modules contain zero Flyway migration scripts and rely on `ddl-auto: update`, which is prohibited in staging.
- **Missing Container Definitions**: `docker-compose.yml` provides only backing services (Postgres, Redis, Kafka, Zookeeper). There are no Dockerfiles or Compose configurations for service application containers.
- **Distributed Infrastructure Dependencies**: Microservices require Eureka discovery, Config server, and Apache Kafka cluster coordination with OpenFeign RPCs.

---

## 2. Environment Variables Specification

All sensitive values must be injected via environment variables. Never commit secrets to source control.

| Environment Variable | Required / Conditional | Default / Fallback | Description |
| :--- | :---: | :---: | :--- |
| `SPRING_PROFILES_ACTIVE` | **Required** | None | Must be set to `prod` for staging deployments. |
| `PORT` | Optional | `8089` | HTTP server listening port. |
| `SPRING_DATASOURCE_URL` | **Required** | `jdbc:postgresql://localhost:5432/subscribly` | JDBC URL for the PostgreSQL database. |
| `SPRING_DATASOURCE_USERNAME` | **Required** | None | Database username. |
| `SPRING_DATASOURCE_PASSWORD` | **Required** | None | Database password. |
| `JWT_SECRET` | **Required** | None | 256-bit (32-byte) Base64 cryptographically secure key for HS256 signing. |
| `RAZORPAY_LIVE_CALL_ENABLED` | **Required** | `false` | `false` = offline simulation mode; `true` = live gateway mode. |
| `RAZORPAY_KEY_ID` | Conditional | `rzp_test_mockKeyId` | Required if `RAZORPAY_LIVE_CALL_ENABLED=true`. |
| `RAZORPAY_KEY_SECRET` | Conditional | `rzp_test_mockSecretKey12345` | Required if `RAZORPAY_LIVE_CALL_ENABLED=true`. |
| `RAZORPAY_WEBHOOK_SECRET` | Conditional | `rzp_test_mockWebhookSecret12345` | Required if `RAZORPAY_LIVE_CALL_ENABLED=true` or verifying real webhooks. |
| `RAZORPAY_API_URL` | Optional | `https://api.razorpay.com/v1` | Razorpay Orders API base endpoint. |
| `EUREKA_CLIENT_ENABLED` | Optional | `false` | Set to `false` for standalone deployment without Eureka. |

---

## 3. Cryptographically Secure JWT Secret Generation & Rotation

The historical key committed in commit a103b7a (`A7fL...JoI=`) is compromised and must never be reused.

### Generate a New 256-Bit Secret (32 Bytes Base64)

**Using OpenSSL:**
```bash
openssl rand -base64 32
```

**Using PowerShell:**
```powershell
$bytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

### Rotation Steps
1. Generate a new 256-bit Base64 key using one of the commands above.
2. Update the environment variable `JWT_SECRET` in the staging secret manager / container environment.
3. Restart the monolith process. Existing tokens signed with the old key will be rejected immediately with HTTP 401, requiring clients to re-authenticate cleanly.

---

## 4. Database Provisioning & Flyway Behavior

- **Engine**: PostgreSQL 15+ (verified through 18.1).
- **Flyway Lifecycle**: Enabled in `prod` profile (`spring.flyway.enabled=true`). On application startup, Flyway connects first, creates `flyway_schema_history` if absent, and applies migrations `V1__init_schema.sql`, `V2__add_performance_and_idempotency_indexes.sql`, and `V3__production_resilience_and_payment_fields.sql`.
- **Hibernate Verification**: In `prod` profile, `spring.jpa.hibernate.ddl-auto` is strictly set to `validate`. Hibernate validates entities against the schema migrated by Flyway and will never alter, drop, or create tables at runtime.

---

## 5. Build, Execution, and Smoke-Test Procedure

### Step 1: Package Executable JAR
```powershell
# Set Java 21 Azul JDK
$env:JAVA_HOME = "C:\Users\HIMANSHU\.jdks\azul-21.0.7"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Build packaged executable JAR with all tests verified
.\mvnw.cmd package -pl monolith
```
Artifact generated: `monolith/target/subscribly-0.0.1-SNAPSHOT.jar`.

### Step 2: Start Application in Staging Profile
```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:PORT = "8089"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://<host>:5432/<staging_db>"
$env:SPRING_DATASOURCE_USERNAME = "<db_user>"
$env:SPRING_DATASOURCE_PASSWORD = "<db_password>"
$env:JWT_SECRET = "<secure_base64_256bit_key>"
$env:RAZORPAY_LIVE_CALL_ENABLED = "false"
$env:EUREKA_CLIENT_ENABLED = "false"

java -jar monolith/target/subscribly-0.0.1-SNAPSHOT.jar
```

### Step 3: Run Automated HTTP Smoke Tests
```powershell
python scratch/staging_smoke_test.py
```
Validates:
1. `GET /api/v1/health` -> HTTP 200 `{"status": "UP"}`
2. `GET /actuator/health` -> HTTP 200 `{"status": "UP"}`
3. `POST /api/v1/auth/register` (User A) -> HTTP 200 + JWT
4. `POST /api/v1/auth/login` (User A) -> HTTP 200 + JWT
5. `POST /api/v1/auth/register` (User B) -> HTTP 200 + JWT
6. `GET /api/v1/subscription/my` (Unauthenticated) -> HTTP 401
7. `POST /api/v1/subscription/create` (User A) -> HTTP 200
8. `GET /api/v1/subscription/my` (User A) -> HTTP 200 (lists own subscription)
9. `GET /api/v1/subscription/{id}/validate` (User A) -> HTTP 200 (own subscription)
10. `GET /api/v1/subscription/{id}/validate` (User B on User A) -> HTTP 403 Forbidden
11. `POST /api/v1/payments/order` (User B on User A) -> HTTP 403 Forbidden
12. `POST /api/v1/payments/webhook` (Tampered / Invalid Signature) -> HTTP 400 Bad Request

### Step 4: Graceful Shutdown
Send SIGTERM (or Ctrl+C) to the running Java process. Hikari connection pools and Tomcat web server will close gracefully.

---

## 6. Verified vs Unverified Capabilities

| Area | Status | Evidence / Notes |
| :--- | :---: | :--- |
| **Monolith Reactor Tests** | **VERIFIED** | 129 tests passing in monolith, 155 tests across full 10-module reactor (Exit code 0). |
| **Flyway Migrations (V1-V3)** | **VERIFIED** | Verified fresh DB apply and incremental upgrade against PostgreSQL 18.1. |
| **Hibernate Schema Validation** | **VERIFIED** | `ddl-auto=validate` verified against migrated schema with zero entity mismatch. |
| **Fail-Fast Secret Validation** | **VERIFIED** | `@PostConstruct` in `JwtUtil` and `RazorpayGatewayProvider` rejects missing/weak/placeholder secrets in production. |
| **HTTP Security & Authorization** | **VERIFIED** | 12 HTTP smoke tests verified: 401 on unauthenticated, 403 on cross-user access, 400 on bad webhook signature. |
| **Trusted Proxy Configuration** | **VERIFIED** | `server.forward-headers-strategy: framework` configured to handle TLS-terminating proxies. |
| **Offline Payment Simulation** | **VERIFIED** | Orders generated with `order_sandbox_` prefix without external network calls. |
| **Real Razorpay Sandbox Calls** | **UNVERIFIED** | Not demonstrated against live Razorpay sandbox HTTP endpoint; requires real merchant test credentials. |
| **Compromised Historical JWT Key** | **PENDING ROTATION**| Exposed key in git history (`a103b7a`) must be rotated before staging deployment. |
