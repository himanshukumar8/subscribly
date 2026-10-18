# Subscribly: Complete SDE-2 / SDE-3 Technical & Interview Handbook
### Production-Grade Subscription Engine & Event-Driven Financial Architecture

---

## Document Metadata
- **Repository:** `D:\Projects\Subscribly\subscribly`
- **Branch:** `microservices-v2`
- **Architecture:** Hybrid Modular Monolith + Spring Cloud Distributed Microservices
- **Core Stack:** Java 21 LTS (Azul Zulu 21.0.7), Spring Boot 3.4.3, Spring Cloud 2024.0.0, PostgreSQL 18, React 18, Vite, Tailwind CSS, Docker Compose
- **Target Audience:** SDE-2 / SDE-3 Backend Engineers, Systems Architects, Technical Interviewers
- **Target Length:** 55–60 substantive pages (~20,000–25,000 words in standard formatting)
- **Verified Test Suite:** 205 automated tests across 24 test suites (100% passing, 0 errors, 0 failures)
- **Author:** Senior Distributed Systems & Software Engineering Lead

---

# Table of Contents
1. [Executive Overview & Project Walkthrough](#chapter-1--executive-overview--project-walkthrough)
   - 1.1 The Real-World Financial Problem
   - 1.2 Core Domain Model & Entity Relationships
   - 1.3 Architecture & Module Responsibilities
   - 1.4 Structured Interview Walkthroughs (2-Min, 5-Min, 15-Min)
   - 1.5 Honest Engineering Maturity Assessment
2. [Requirements and Engineering Decisions](#chapter-2--requirements-and-engineering-decisions)
   - 2.1 Functional Requirements (FR)
   - 2.2 Non-Functional Requirements (NFR)
   - 2.3 Architecture Decision Records (ADRs 001–006)
3. [Java and Spring Boot Internals](#chapter-3--java-and-spring-boot-internals)
   - 3.1 IoC Container, Bean Lifecycle & Proxies
   - 3.2 Declarative Transaction Management & Propagation
   - 3.3 Spring Security 6 & Filter Chain Ordering
   - 3.4 Task Scheduling & Multi-Instance Safety
   - 3.5 Java 21 Modern Language Features in Production
4. [PostgreSQL and Financial Data Modeling](#chapter-4--postgresql-and-financial-data-modeling)
   - 4.1 Relational Storage vs. Document Stores (PG vs. MongoDB)
   - 4.2 Database Schema & Flyway Migration Strategy
   - 4.3 Indexing Strategy, Query Plans & HikariCP Pool Sizing
   - 4.4 PostgreSQL MVCC, Lock Hierarchy & Transaction Isolation
5. [Subscription Lifecycle and Mathematical Correctness](#chapter-5--subscription-lifecycle-and-mathematical-correctness)
   - 5.1 The Subscription Finite State Machine (FSM)
   - 5.2 Temporal Accuracy: `Instant` vs. `LocalDate`
   - 5.3 Renewal, Expiration & Cancellation Semantics
   - 5.4 The High-Scale Scheduled Polling Engine
6. [Payment Processing and Gateway Abstraction](#chapter-6--payment-processing-and-gateway-abstraction)
   - 6.1 The Gateway Abstraction Layer (Strategy Pattern)
   - 6.2 Razorpay Order Creation & Client Modal Integration
   - 6.3 Dual-Channel Verification & Raw HMAC-SHA256 Cryptography
   - 6.4 Retry Strategy, Exponential Backoff & State Machines
   - 6.5 Financial Reconciliation & Drift Detection
7. [Concurrency, Locking, and Distributed Invariants](#chapter-7--concurrency-locking-and-distributed-invariants)
   - 7.1 Optimistic vs. Pessimistic Locking
   - 7.2 Race Condition Deep-Dives in Subscribly
   - 7.3 Transactional Outbox Pattern vs. Distributed 2PC
8. [Modular Monolith vs. Distributed Microservices](#chapter-8--modular-monolith-vs-distributed-microservices)
   - 8.1 Architectural Trade-Off Matrix
   - 8.2 Subscribly Service Topography & Routing
   - 8.3 Synchronous Feign vs. Asynchronous Kafka Messaging
9. [Caching, Redis, and High-Throughput Optimization](#chapter-9--caching-redis-and-high-throughput-optimization)
   - 9.1 When to Introduce Redis (and When NOT To)
   - 9.2 Cache Invalidation, TTLs & Stampede Protection
   - 9.3 Eliminating N+1 Selects & Keyset Pagination
10. [Security Architecture, Cryptography, and RBAC](#chapter-10--security-architecture-cryptography-and-rbac)
    - 10.1 BCrypt Password Hashing & Salt Physics
    - 10.2 JWT Architecture: Symmetric vs. Asymmetric
    - 10.3 Role-Based Access Control (RBAC) & Ownership Invariants
11. [REST API Design, Error Contracts, and Frontend Integration](#chapter-11--rest-api-design-error-contracts-and-frontend-integration)
    - 11.1 Standardized Error Contract (RFC 7807)
    - 11.2 Frontend Architecture (React 18 + Vite + Tailwind)
12. [Testing Strategy, Verification Matrix, and Test Doubles](#chapter-12--testing-strategy-verification-matrix-and-test-doubles)
    - 12.1 The Testing Pyramid in Subscribly
    - 12.2 Verified Reactor Test Matrix (205 Passed Tests)
    - 12.3 Mockito Doubles & Clock Injection
13. [Reliability, Observability, and Operational Telemetry](#chapter-13--reliability-observability-and-operational-telemetry)
    - 13.1 Correlation IDs Across Distributed Traces
    - 13.2 Spring Boot Actuator & Health Indicators
    - 13.3 Graceful Shutdown & Zero-Downtime Deployment
14. [Architectural Decision Matrix (10 Direct Comparisons)](#chapter-14--architectural-decision-matrix-10-direct-comparisons)
15. [60 In-Depth SDE-2/SDE-3 Interview Challenge Questions & Model Answers](#chapter-15--60-in-depth-sde-2sde-3-interview-challenge-questions--model-answers)
    - Domain 1: Java & Spring Boot Core Internals (Q1–Q10)
    - Domain 2: Concurrency, Locking & Transactions (Q11–Q20)
    - Domain 3: Database Modeling & Performance (Q21–Q30)
    - Domain 4: Payment Processing & Gateways (Q31–Q40)
    - Domain 5: Distributed Systems & Microservices (Q41–Q50)
    - Domain 6: Security, Testing & System Reliability (Q51–Q60)
16. [Comprehensive System Design Blueprints (6 Scenarios)](#chapter-16--comprehensive-system-design-blueprints-6-scenarios)
    - Blueprint 1: High-Scale Subscription Billing Engine (10M Users)
    - Blueprint 2: Resilient Payment Webhook Ingestion (10k Webhooks/s)
    - Blueprint 3: Distributed Scheduled Job Runner (ShedLock Architecture)
    - Blueprint 4: Multi-Tenant SaaS Billing Platform
    - Blueprint 5: Event-Driven Notification Engine (Bulkhead Architecture)
    - Blueprint 6: Zero-Downtime Monolith-to-Microservices Migration (Strangler Fig)
17. [Revision Toolkit, Navigation Guide, and Glossary](#chapter-17--revision-toolkit-navigation-guide-and-glossary)
    - 17.1 Source Code Navigation Guide
    - 17.2 The Day-Before Interview Revision Cheat Sheet
    - 17.3 20 High-Yield Flashcard Concepts
    - 17.4 4-Week SDE-2/SDE-3 Mastery Study Plan
    - 17.5 Architectural Glossary (40+ Terms)
    - 17.6 "Why This and Not That?" Defense Checklist
    - 17.7 Known Limitations & Top 3 High-Value Roadmap Items

---

# Chapter 1 — Executive Overview & Project Walkthrough

### 1.1 The Real-World Financial Problem
Modern software-as-a-service (SaaS) businesses depend on recurring subscription billing. While the consumer-facing user journey appears trivial—select plan, pay monthly fee, enjoy service—the backend engineering reality is notoriously fraught with catastrophic failure modes:

1. **Double-Billing and Financial Non-Idempotency:** A momentary network partition between the client browser, payment gateway, and backend server often triggers duplicate webhook delivery or rapid client retries. Without strict idempotency, customers are billed multiple times, triggering chargebacks, financial penalties, and customer churn.
2. **State Drift Between Gateways and Internal Ledgers:** Third-party gateways (e.g., Razorpay, Stripe) maintain their own internal state machines. If a gateway marks an order paid but the internal database drops the connection during transaction commit, the customer's account remains unpaid while money is deducted from their bank account.
3. **Temporal Boundary Violations:** Date-based calculations that neglect leap years, varying monthly lengths, and timezones cause subscriptions to expire hours early or renew late, violating service level agreements (SLAs).
4. **Concurrent Lifecycle Operations:** A user clicking "Cancel Subscription" at the exact second a scheduled auto-renewal job triggers payment collection leads to race conditions, charging customers who just cancelled.
5. **Cascading Distributed Failures:** Naive microservices architectures that synchronously chain HTTP calls across user, subscription, payment, and notification services collapse entirely when an external gateway experiences latency spikes.

**Subscribly** was engineered as an enterprise-grade, fault-tolerant subscription and recurring payment management engine designed specifically to solve these fundamental engineering challenges.

---

### 1.2 Core Domain Model & Entity Relationships
Subscribly models the subscription domain through strict relational boundaries:

```mermaid
erDiagram
    USERS ||--o{ SUBSCRIPTIONS : "owns"
    USERS ||--o{ PAYMENTS : "initiates"
    USERS }o--o{ ROLES : "assigned"
    SUBSCRIPTIONS ||--o{ PAYMENTS : "billed_for"
    PAYMENTS ||--o{ PAYMENT_AUDIT_LOGS : "generates"
    PAYMENTS ||--o| WEBHOOK_EVENT_RECORDS : "verified_by"

    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar full_name
        boolean active
        timestamp created_at
    }

    SUBSCRIPTIONS {
        bigint id PK
        bigint user_id FK
        varchar plan
        double price
        varchar status
        varchar billing_cycle
        instant started_at
        instant expires_at
        boolean auto_renew
        bigint version
    }

    PAYMENTS {
        bigint id PK
        varchar payment_reference UK
        varchar gateway_order_id UK
        varchar gateway_payment_id
        varchar gateway_signature
        bigint subscription_id FK
        bigint user_id FK
        double amount
        varchar currency
        varchar status
        int retry_count
        timestamp next_retry_at
        bigint version
    }

    PAYMENT_AUDIT_LOGS {
        bigint id PK
        bigint payment_id FK
        varchar payment_reference
        varchar from_status
        varchar to_status
        varchar reason
        varchar triggered_by
        timestamp timestamp
    }

    WEBHOOK_EVENT_RECORDS {
        varchar event_id PK
        varchar event_type
        varchar status
        timestamp processed_at
    }
```

---

### 1.3 Architecture & Module Responsibilities
Subscribly is implemented as a **dual-topology architecture**:
- **Phase 1 Modular Monolith (`/monolith`):** A single deployable artifact structured into clean domain modules (`user`, `subscription`, `payment`, `scheduler`, `security`). Perfect for low operational overhead, fast local development, and sub-millisecond in-process transactional boundaries.
- **Phase 2 Distributed Microservices (`/user-service`, `/subscription-service`, `/payment-service`, `/notification-service`, `/api-gateway`, `/discovery-server`, `/config-server`, `/common-lib`):** Independent deployable Spring Boot services communicating synchronously via Spring Cloud OpenFeign and asynchronously via Apache Kafka events, unified through Netflix Eureka and Spring Cloud Gateway.

#### Core Modules Summary
- **`common-lib`:** Shared DTOs (`UserDto`, `SubscriptionDto`), domain events (`PaymentCompletedEvent`, `PaymentFailedEvent`, `SubscriptionExpiredEvent`), and serialization contracts.
- **`discovery-server`:** Netflix Eureka service registry providing dynamic service registration, client-side load balancing, and heartbeat health checks.
- **`config-server`:** Spring Cloud Config Server backed by Git/local repositories providing centralized, profile-aware configuration without service redeployment.
- **`api-gateway`:** Non-blocking Spring Cloud Gateway (Netty-based) handling centralized JWT routing, correlation ID injection, rate limiting, and path rewrites.
- **`user-service`:** Identity provider, BCrypt password hashing, role management, and JWT issue/validation.
- **`subscription-service`:** Subscription lifecycle engine, expiry tracking, plan transitions, and auto-renewal coordination.
- **`payment-service`:** Gateway abstraction layer, Razorpay integration, HMAC-SHA256 signature verification, idempotency tracking, and state machine enforcement.
- **`notification-service`:** Event consumer listening to Kafka topics and dispatching transactional email/SMS notifications.
- **`frontend`:** React 18 single-page application built with Vite and Tailwind CSS, featuring dark-mode dashboard, Razorpay Checkout SDK integration, and optimistic status updates.

---

### 1.4 Structured Interview Walkthroughs

#### The 2-Minute Elevator Pitch
> *"Subscribly is a fault-tolerant subscription and recurring billing platform built with Java 21, Spring Boot 3, and PostgreSQL. It solves the critical engineering problems of double-billing, state drift, and race conditions during payment collection. I built it first as a clean modular monolith to establish strict transaction boundaries and automated testing, then decomposed it into distributed microservices connected via Spring Cloud Gateway, Eureka, OpenFeign, and Kafka.*
>
> *Its core technical features include a mathematically deterministic subscription lifecycle engine using UTC Instants, a Razorpay gateway provider implementing dual-channel verification (synchronous client-side redirect + raw-payload HMAC-SHA256 webhook ingestion), database-enforced idempotency keys, and optimistic locking to prevent race conditions during concurrent renewals and cancellations. The entire reactor is validated by 205 automated tests across 24 test suites with zero failures."*

#### The 5-Minute Technical Deep Dive
> *"When designing Subscribly, I prioritized financial data integrity above all else. Subscription systems live or die by consistency: money cannot be deducted twice, and active subscriptions must never expire prematurely.*
>
> *To guarantee this, I implemented three core engineering patterns:*
> *First, strict idempotency and auditability: Every payment begins with a unique `payment_reference` and maps to a gateway order. In the webhook ingestion pipeline, the raw request body is verified against Razorpay's HMAC-SHA256 signature before any JSON deserialization occurs. Webhooks are recorded in an immutable `webhook_event_records` table using atomic unique constraints, guaranteeing that duplicate webhook deliveries are acknowledged with HTTP 200 without reprocessing.*
>
> *Second, robust concurrency control: Both `Subscription` and `Payment` entities leverage Hibernate `@Version` optimistic locking. If a scheduled renewal batch attempts to charge a subscription while the user initiates a cancellation, one transaction succeeds while the other encounters an `OptimisticLockException` and aborts gracefully, preventing corrupted state.*
>
> *Third, architectural pragmatism: I avoided speculative infrastructure like distributed locks or distributed 2PC transactions. Instead, I established clean service boundaries where PostgreSQL acts as the single source of truth, and asynchronous events handle non-critical side effects like notifications. In this iteration, we proved the architecture locally across 205 automated unit and integration tests."*

#### The 15-Minute SDE-3 Architectural Interview
> In a comprehensive senior interview, walk through:
> 1. **System Invariants:** Why financial records must never be hard-deleted, why audit logs use `Propagation.REQUIRES_NEW`, and why clock skew is mitigated via injected `Clock` beans.
> 2. **Failure Analysis:** Walk through a gateway outage, lost webhook, database deadlock, and simultaneous user cancellation.
> 3. **The Microservices Evolution:** Contrast the compile-time safety and ACID transactional guarantees of the monolith with the independent scalability and fault isolation of the poly-service architecture.
> 4. **Scale Bottlenecks at 100x:** Explain table partitioning on `payments`, read replica offloading, keyset pagination in the renewal scheduler, and Redis Token Bucket rate limiting at the gateway.

---

### 1.5 Honest Engineering Maturity Assessment
In technical interviews, claiming a side project is "100% production-ready for millions of users" destroys credibility. SDE-3 candidates distinguish implemented reality from architectural aspiration:

| Dimension | Implemented & Tested in Repository | Production Hardening Required |
| :--- | :--- | :--- |
| **Code Correctness** | 205 passing JUnit 5 tests, clean Maven build across 10 modules. | Distributed fault-injection (Chaos Monkey) under live network latency. |
| **Payment Flow** | Razorpay SDK abstraction, order creation, signature check, webhook deduplication. | Live PCI-DSS compliance audits, automated reconciliation cron with banking settlement files. |
| **Database** | Relational schema, Flyway migrations (V1–V4), B-tree indexes, foreign keys. | Multi-region read replicas, connection pool PgBouncer sidecars, table partitioning. |
| **Messaging** | Kafka templates configured, event models defined, mocked integration verified. | Multi-broker cluster, consumer group rebalance tuning, dead-letter queue (DLQ) automated replay UI. |
| **Security** | BCrypt cost 12, JWT validation filter, RBAC, admin demotion safeguard. | Secret vault (HashiCorp Vault / AWS Secrets Manager), mTLS between microservices, WAF. |

---

# Chapter 2 — Requirements and Engineering Decisions

### 2.1 Functional Requirements (FR)
- **FR-01 (Authentication & Authorization):** Users must register with email and password, authenticate to receive stateless JWTs, and be authorized based on roles (`ROLE_USER`, `ROLE_ADMIN`).
- **FR-02 (Plan Management):** Support tiered subscription tiers (e.g., FREE, BASIC, PRO, ENTERPRISE) with fixed prices, currencies, and billing cycles (MONTHLY, ANNUAL).
- **FR-03 (Subscription Lifecycle):** Users can subscribe to plans, view active entitlements, cancel auto-renewal, and immediately view state transitions (`PENDING`, `ACTIVE`, `CANCELLED`, `EXPIRED`).
- **FR-04 (Payment Ingestion):** Initiate payment orders with third-party gateways (Razorpay), receive client payment verification tokens, and process asynchronous gateway webhook events.
- **FR-05 (Automated Renewal Scheduler):** A background worker must periodically poll for subscriptions nearing expiration, initiate recurring charges, and advance subscription validity windows upon success.
- **FR-06 (Immutable Audit Ledger):** Every state transition of a payment or subscription must generate an append-only audit log recording old state, new state, trigger actor, and timestamp.

---

### 2.2 Non-Functional Requirements (NFR)
- **NFR-01 (Strict Financial Idempotency):** No sequence of network retries, duplicate client clicks, or duplicate webhook payloads may result in multiple successful charges for a single billing period.
- **NFR-02 (Low Latency Reads):** Customer subscription entitlement checks (e.g., "Is user X active on plan PRO?") must execute in < 20ms at p99.
- **NFR-03 (High Availability of Ingestion):** Webhook ingestion endpoints must accept and persist incoming events in < 50ms, returning HTTP 200 to prevent gateway backoff.
- **NFR-04 (Clock Skew Invariance):** All timestamps must be stored in UTC (`Instant` / `timestamptz`). Expiry checks must be deterministic and testable via injected mock clocks.
- **NFR-05 (Zero Secret Exposure):** Gateway API secrets, JWT private signing keys, and database passwords must never be committed to source code or logged in plain text.

---

### 2.3 Architecture Decision Records (ADRs)

#### ADR-001: Dual-Phase Architecture (Modular Monolith to Microservices)
- **Context:** Building microservices from day zero introduces distributed transaction complexity, network overhead, and testing friction before domain boundaries stabilize.
- **Decision:** Implement the complete domain logic first as a modular monolith in `/monolith`, establishing rock-solid database constraints and 100% test coverage. Subsequently, extract bounded contexts into standalone Spring Cloud services (`user-service`, `subscription-service`, `payment-service`) sharing clean domain contracts in `common-lib`.
- **Consequences:** The project demonstrates both architectural styles. Developers can run tests in under 40 seconds locally in the monolith while showcasing distributed cloud readiness in the microservices modules.

#### ADR-002: PostgreSQL as the Single Source of Financial Truth
- **Context:** Subscribly requires multi-table atomic transactions (updating subscription state, creating payment records, and writing immutable audit logs simultaneously).
- **Decision:** Standardize on PostgreSQL with relational foreign keys, unique constraints, and ACID guarantees. Reject document stores (MongoDB) for core ledgers.
- **Consequences:** Eliminates phantom reads and partial write anomalies. Caching is delegated to Redis where necessary, but PostgreSQL retains authoritative ownership.

#### ADR-003: Razorpay Payment Gateway Abstraction with Strategy Pattern
- **Context:** Hardcoding gateway SDK calls directly inside domain controllers couples the business logic to a single vendor.
- **Decision:** Introduce a clean interface `PaymentGatewayProvider` with methods `createOrder()`, `verifyPaymentSignature()`, and `verifyWebhookSignature()`. Implement `RazorpayGatewayProvider` as the default concrete bean.
- **Consequences:** New payment processors (e.g., Stripe, PayPal, Adyen) can be plugged in by adding a new provider class without touching domain business services.

#### ADR-004: Dual-Channel Verification with Raw-Payload Webhooks
- **Context:** Browsers can crash before sending payment verification to the backend. Conversely, webhooks can be delayed by minutes.
- **Decision:** Support both synchronous client verification (`/api/payments/verify`) and asynchronous webhook ingestion (`/api/payments/webhook`). Enforce HMAC-SHA256 signature verification directly against the raw byte string of incoming webhook payloads before parsing.
- **Consequences:** 100% reliable payment capture regardless of client browser stability. Protects against JSON formatting/reordering tampering attacks.

#### ADR-005: Optimistic Locking with Retry Strategy over Distributed Locks
- **Context:** Concurrent renewals and cancellations risk lost update anomalies. Distributed locks (e.g., Redlock) introduce external Redis dependencies, lock timeout hazards, and single points of failure.
- **Decision:** Implement JPA `@Version` optimistic locking on `Subscription` and `Payment` entities combined with application-level retry policies.
- **Consequences:** Eliminates database lock queues and deadlocks. Ensures 100% safety under write contention with zero extra infrastructure.

#### ADR-006: React 18 + Vite Single Page Application
- **Context:** The frontend requires real-time payment modal interaction, token-authenticated REST calls, and clean status reflection.
- **Decision:** Build a single-page application using React 18, Vite for sub-second hot module reloading, and Tailwind CSS for utility-first responsive styling.
- **Consequences:** Extremely fast production builds (13.56s bundle time), zero runtime CSS overhead, and native TypeScript type safety across API boundaries.

---

# Chapter 3 — Java and Spring Boot Internals

### 3.1 IoC Container, Bean Lifecycle & Proxies
Spring Boot manages application components inside the `ApplicationContext`. In Subscribly, beans are managed through explicit stereotype annotations (`@Service`, `@Repository`, `@Component`, `@Configuration`).

```mermaid
sequenceDiagram
    participant JVM as JVM Classloader
    participant Ctx as Spring ApplicationContext
    participant Bean as PaymentService Bean
    participant DB as HikariCP / Database

    JVM->>Ctx: Load Class Definitions & Bytecode
    Ctx->>Ctx: Instantiate Bean via Reflection (Constructor Injection)
    Ctx->>Bean: Inject Dependencies (PaymentRepository, GatewayProvider)
    Ctx->>Bean: Invoke @PostConstruct / InitializingBean
    Ctx->>DB: Verify Datasource Connectivity
    Note over Bean: Bean enters READY / IN-SERVICE state
    Ctx->>Bean: Serve Application Traffic
    JVM->>Ctx: JVM Shutdown Hook Triggered
    Ctx->>Bean: Invoke @PreDestroy / DisposableBean
    Ctx->>DB: Close Hikari Connection Pool
```

#### Bean Lifecycle Invariants in Subscribly
1. **Constructor Injection over Field Injection:** All services utilize Lombok `@RequiredArgsConstructor` with `private final` fields. This guarantees immutability, prevents `NullPointerException` during unit testing, and eliminates circular dependency antipatterns at compile time.
2. **Stateless Singletons:** All service beans are thread-safe singletons. No instance variables store request-specific user data. State is strictly passed via stack frames (method arguments and local variables).

---

### 3.2 Declarative Transaction Management & Propagation
Declarative transaction management via `@Transactional` uses Spring AOP proxies. When a method annotated with `@Transactional` is invoked, Spring intercepts the call, acquires a database connection from HikariCP, begins an ACID transaction, executes the method body, and commits upon successful return.

```java
// Located at: payment-service/src/main/java/com/subscribly/paymentservice/service/PaymentAuditService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAuditService {

    private final PaymentAuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTransition(Long paymentId, String paymentReference, 
                                 PaymentStatus fromStatus, PaymentStatus toStatus, 
                                 String reason, String triggeredBy) {
        log.info("[PaymentAudit] Recording transition: {} -> {}", fromStatus, toStatus);
        PaymentAuditLog logEntry = PaymentAuditLog.builder()
                .paymentId(paymentId)
                .paymentReference(paymentReference)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(reason)
                .triggeredBy(triggeredBy)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(logEntry);
    }
}
```

#### Why `Propagation.REQUIRES_NEW` is Essential for Auditing
In standard Spring applications, `@Transactional` defaults to `Propagation.REQUIRED`, which joins the existing outer transaction. If the outer payment transaction fails and rolls back, **all audit logs written during that transaction are also rolled back and lost!**
By marking `PaymentAuditService.recordTransition()` with `Propagation.REQUIRES_NEW`:
1. The outer transaction is temporarily suspended.
2. An independent, second database connection is opened.
3. The audit log is written and immediately committed in its own physical transaction.
4. The outer transaction resumes. Even if the outer transaction later throws a `RuntimeException` and rolls back, the audit log remains permanently recorded in PostgreSQL!

---

### 3.3 Spring Security 6 & Filter Chain Ordering
Subscribly implements stateless authentication using Spring Security 6:

```mermaid
flowchart TD
    Req[Incoming HTTP Request] --> SC[SecurityFilterChain]
    SC --> Cors[CorsFilter]
    SC --> Csrf[CsrfFilter - Disabled for Stateless API]
    SC --> JWT[JwtAuthFilter - Custom OncePerRequestFilter]
    JWT -->|Extract Bearer Token| TokenVal{Token Valid?}
    TokenVal -->|Yes| SetAuth[SecurityContextHolder.setAuthentication]
    TokenVal -->|No / Absent| Anon[AnonymousAuthenticationFilter]
    SetAuth --> Disp[DispatcherServlet]
    Anon --> Disp
    Disp --> AuthCheck{Endpoint Requires Role?}
    AuthCheck -->|Authorized| Controller[Controller Method Execution]
    AuthCheck -->|Forbidden 403 / 401| ErrResp[AuthenticationEntryPoint / AccessDeniedHandler]
```

#### Security Implementation Highlights
- **`OncePerRequestFilter` Guarantees Single Execution:** `JwtAuthFilter` intercepts every incoming HTTP request exactly once per dispatch. It extracts the `Authorization: Bearer <token>` header, verifies the cryptographic signature, extracts user claims (`email`, `roles`), and instantiates a `UsernamePasswordAuthenticationToken` stored in `SecurityContextHolder`.
- **Stateless Session Policy:** `sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)` instructs Spring Security to never create an `HttpSession`. This ensures zero server-side memory leaks and seamless horizontal scaling behind a round-robin load balancer.

---

### 3.4 Task Scheduling & Multi-Instance Safety
In `monolith/src/main/java/com/subscribly/scheduler/SubscriptionScheduler.java`, recurring subscription renewals are polled via Spring's `@Scheduled` annotation:

```java
@Scheduled(cron = "${app.scheduler.subscription-renewal-cron:0 0 2 * * ?}")
public void processSubscriptionRenewals() {
    log.info("[SubscriptionScheduler] Starting automated renewal batch execution...");
    // 1. Query subscriptions expiring within renewal window
    // 2. Iterate in micro-batches
    // 3. Initiate recurring payment charges
    // 4. Update validity periods idempotently
}
```

#### Multi-Instance Protection
When running multiple instances of a service in Docker or Kubernetes, a standard `@Scheduled` task executes on **every single container simultaneously**, causing duplicate billing.
Subscribly safeguards against this in three layers:
1. **Optimistic Locking:** Every subscription update increments `@Version`. If Instance A and Instance B both attempt to renew Subscription 100, one commits and the second fails with `OptimisticLockingFailureException`.
2. **Payment Reference Deduplication:** The renewal payment reference is generated deterministically (e.g., `RENEW-SUB100-20261018`). PostgreSQL's unique constraint on `payment_reference` rejects duplicate payment insertions immediately.
3. **ShedLock / Redis Lock Architecture (Recommended for High Scale):** Integrating ShedLock wraps `@Scheduled` with a distributed lock table in PostgreSQL or Redis, ensuring only one instance acquires the execution lease.

---

### 3.5 Java 21 Modern Language Features in Production
Subscribly is built on **Java 21 LTS**, leveraging modern enterprise language capabilities:
- **Record Classes:** Domain events (`PaymentCompletedEvent`, `PaymentFailedEvent`) and DTOs are declared as immutable `record` components, providing built-in value-based equality, concise syntax, and zero boilerplate.
- **Pattern Matching for Switch:** Used inside the payment state machine and webhook handlers to branch cleanly on event types without messy `instanceof` casts.
- **Virtual Threads Readiness:** All I/O operations and database queries are fully non-pinning, allowing the application to enable `spring.threads.virtual.enabled=true` for massive concurrency under heavy I/O workloads.
- **Sequenced Collections:** Deterministic audit trail traversal using `LinkedHashMap` and sequenced collections ensuring chronological log ordering.

---

# Chapter 4 — PostgreSQL and Financial Data Modeling

### 4.1 Relational Storage vs. Document Stores (PG vs. MongoDB)
A common system design interview question is: *"Why choose PostgreSQL over MongoDB for a subscription and billing platform?"*

| Architectural Dimension | PostgreSQL (Subscribly Choice) | MongoDB (Document Store) |
| :--- | :--- | :--- |
| **Data Integrity & Consistency** | Strict ACID guarantees across multiple tables; foreign keys prevent orphaned payments. | Document-level atomicity. Multi-document transactions exist but carry high latency overhead. |
| **Financial Ledger Constraints** | Database-level `UNIQUE` constraints (`payment_reference`, `gateway_order_id`) reject double billing at the storage engine layer. | Requires application-level checks or unique sparse indexes that can fail during sharded cluster rebalancing. |
| **Audit Log Immutability** | Append-only audit tables with foreign keys and timestamp indexes. | Can append to nested arrays, but documents risk exceeding the 16MB BSON limit over time. |
| **Query Flexibility & Reporting** | Complex joins, window functions (`LEAD`, `LAG`), aggregations for MRR/ARR financial reporting. | Aggregation pipeline is verbose; joins (`$lookup`) are memory-intensive and un-sharded. |
| **Schema Evolution** | Declarative Flyway migrations with strict type checking and rollback validation. | Schema-on-read leads to silent bugs where older documents lack new financial fields. |

---

### 4.2 Database Schema & Flyway Migration Strategy
Subscribly utilizes **Flyway** for deterministic, repeatable database migrations located at `monolith/src/main/resources/db/migration/`:

```sql
-- V1__initial_schema.sql (Excerpt)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    plan VARCHAR(50) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) NOT NULL,
    billing_cycle VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    auto_renew BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 0
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    payment_reference VARCHAR(100) NOT NULL UNIQUE,
    gateway_order_id VARCHAR(100) UNIQUE,
    gateway_payment_id VARCHAR(100),
    gateway_signature VARCHAR(255),
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id) ON DELETE RESTRICT,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    amount DOUBLE PRECISION NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(50) NOT NULL,
    retry_count INT DEFAULT 0,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    version BIGINT DEFAULT 0
);

-- V2__payment_idempotency_and_retries.sql
CREATE TABLE webhook_event_records (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
```

---

### 4.3 Indexing Strategy, Query Plans & HikariCP Pool Sizing
Database indexes in Subscribly are tailored specifically to access patterns:

1. **Foreign Key Indexes:**
   ```sql
   CREATE INDEX idx_sub_user_id ON subscriptions(user_id);
   CREATE INDEX idx_pay_subscription_id ON payments(subscription_id);
   CREATE INDEX idx_pay_user_id ON payments(user_id);
   ```
2. **Scheduler Composite Index:**
   ```sql
   CREATE INDEX idx_sub_renewal ON subscriptions(status, auto_renew, expires_at);
   ```
3. **Idempotency Key Hash Lookups:**
   `payment_reference`, `gateway_order_id`, and `event_id` have `UNIQUE` B-tree indexes, ensuring $O(1)$ duplicate detection during ingestion.

---

### 4.4 PostgreSQL MVCC, Lock Hierarchy & Transaction Isolation
PostgreSQL implements Multiversion Concurrency Control (MVCC):
- Every row on disk contains hidden system columns: `xmin` (the transaction ID that inserted the row) and `xmax` (the transaction ID that updated or deleted the row).
- In `READ COMMITTED` isolation, each query sees a snapshot of committed transactions taken at the start of that specific query.
- In `REPEATABLE READ`, the snapshot is taken at the start of the entire transaction, guaranteeing that repeated queries return identical results.
- **Lock Hierarchy:** PostgreSQL maintains table-level locks (`AccessShare`, `RowShare`, `RowExclusive`, `AccessExclusive`) and row-level locks (`FOR UPDATE`, `FOR SHARE`). Subscribly relies primarily on non-blocking row-level MVCC updates with optimistic version verification.

---

# Chapter 5 — Subscription Lifecycle and Mathematical Correctness

### 5.1 The Subscription Finite State Machine (FSM)

```mermaid
stateDiagram-v2
    [*] --> PENDING : User selects plan
    PENDING --> ACTIVE : Payment verified (Sync or Webhook)
    PENDING --> CANCELLED : Payment failed / User abandoned
    ACTIVE --> CANCELLED : User requests cancellation
    ACTIVE --> EXPIRED : Expiry date passed without renewal
    ACTIVE --> ACTIVE : Successful auto-renewal (extends expires_at)
    CANCELLED --> [*]
    EXPIRED --> [*]
```

#### Non-Negotiable Lifecycle Invariants
1. **No Overlapping Active Subscriptions for the Same Plan:** A user cannot have two concurrently `ACTIVE` subscriptions for the identical service tier.
2. **Zero Lost Time on Early Renewal:** If a subscription expires on `2026-11-01T00:00:00Z` and renews on `2026-10-28T12:00:00Z`, the new expiry date must be `2026-12-01T00:00:00Z` ($old\_expiry + 1\_month$), **not** $now() + 1\_month$. Calculating from `now()` steals 3.5 days of paid service from the customer!
3. **Grace Period Entitlement:** When a user clicks "Cancel Subscription", the status updates to `CANCELLED` (or `PENDING_CANCELLATION`), but entitlements remain valid until `expires_at`. The user has paid for the full term.

---

### 5.2 Temporal Accuracy: `Instant` vs. `LocalDate`
All business-critical timestamps are represented internally as `java.time.Instant` in UTC. For user-facing display, the frontend converts the UTC epoch millisecond timestamp to the user's local timezone via browser APIs.

```java
// Testing temporal logic deterministically
Clock fixedClock = Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC);
Instant now = Instant.now(fixedClock);
Instant expiresAt = now.plus(30, ChronoUnit.DAYS);
assertEquals("2026-11-17T10:00:00Z", expiresAt.toString());
```

---

### 5.3 Renewal, Expiration & Cancellation Semantics
1. **Auto-Renewal Window:** Subscriptions are inspected within a 24-hour lookahead window before `expires_at`.
2. **Grace Periods:** Subscriptions encountering payment failure enter a configurable grace period (e.g., 3 days) during which the customer retains access while exponential retry attempts execute in the background.
3. **Immediate Cancellation vs. End-of-Term Cancellation:** Subscribly defaults to End-of-Term cancellation. `auto_renew` is set to `false`, but the subscription remains `ACTIVE` until `expires_at` is reached.

---

### 5.4 The High-Scale Scheduled Polling Engine
In high-scale deployments, executing `SELECT * FROM subscriptions WHERE expires_at <= :threshold` can return millions of records, causing Out-Of-Memory (OOM) errors.
Subscribly's engine implements **Micro-Batching with Keyset Pagination**:
```java
Long lastProcessedId = 0L;
List<Subscription> batch;
do {
    batch = subscriptionRepository.findExpiringBatch(threshold, lastProcessedId, PageRequest.of(0, 100));
    for (Subscription sub : batch) {
        processSingleRenewalSafely(sub);
        lastProcessedId = sub.getId();
    }
} while (!batch.isEmpty());
```

---

# Chapter 6 — Payment Processing and Gateway Abstraction

### 6.1 The Gateway Abstraction Layer (Strategy Pattern)
Subscribly decouples payment vendor SDKs using the **Strategy Pattern**:

```mermaid
classDiagram
    class PaymentGatewayProvider {
        <<interface>>
        +createOrder(ref, amount, currency) GatewayOrderResponse
        +verifyPaymentSignature(orderId, paymentId, signature) boolean
        +verifyWebhookSignature(payload, signatureHeader) boolean
        +getProviderName() String
    }
    class RazorpayGatewayProvider {
        -String keyId
        -String keySecret
        -String webhookSecret
        +createOrder()
        +verifyPaymentSignature()
        +verifyWebhookSignature()
    }
    class StripeGatewayProvider {
        -String stripeApiKey
        +createOrder()
        +verifyPaymentSignature()
        +verifyWebhookSignature()
    }
    PaymentGatewayProvider <|.. RazorpayGatewayProvider : implements
    PaymentGatewayProvider <|.. StripeGatewayProvider : future extension
```

---

### 6.2 Razorpay Order Creation & Client Modal Integration
1. Client selects a plan and calls `POST /api/payments/create-order` with `{ subscriptionId: 100 }`.
2. Backend validates that the subscription belongs to the authenticated user and has not already been paid.
3. Backend calls `RazorpayGatewayProvider.createOrder()`.
4. The gateway returns a `gateway_order_id` (e.g., `order_MKz998xLt1`).
5. Backend creates a `Payment` record with status `CREATED` and returns the order details to the client.
6. The frontend opens the client-side Razorpay Checkout modal:
   ```javascript
   const options = {
       key: razorpayKeyId,
       amount: order.amount * 100, // paisa
       currency: order.currency,
       order_id: order.gatewayOrderId,
       handler: function (response) {
           verifyPaymentOnBackend(response.razorpay_order_id, 
                                  response.razorpay_payment_id, 
                                  response.razorpay_signature);
       }
   };
   const rzp = new window.Razorpay(options);
   rzp.open();
   ```

---

### 6.3 Dual-Channel Verification & Raw HMAC-SHA256 Cryptography

```mermaid
sequenceDiagram
    autonumber
    actor User as User Browser
    participant Rzp as Razorpay Gateway
    participant Backend as Subscribly Backend
    participant DB as PostgreSQL Database

    User->>Rzp: Submit Credit Card / UPI Credentials
    Rzp->>Rzp: Process Transaction Successfully
    par Synchronous Client Redirect
        Rzp-->>User: Return {order_id, payment_id, signature}
        User->>Backend: POST /api/payments/verify
        Backend->>Backend: Compute HMAC_SHA256(order_id + "|" + payment_id, secret)
        Backend->>Backend: Constant-time equals(computed, signature)
        Backend->>DB: Transition Payment -> SUCCESS
        Backend->>DB: Transition Subscription -> ACTIVE
        Backend-->>User: HTTP 200 { status: "SUCCESS" }
    and Asynchronous Webhook
        Rzp->>Backend: POST /api/payments/webhook (Raw Payload + Header)
        Backend->>DB: Check existsByEventId(eventId)
        alt Duplicate Event
            Backend-->>Rzp: HTTP 200 (Acknowledge Idempotently)
        else Fresh Event
            Backend->>Backend: Verify HMAC_SHA256(raw_body, webhook_secret)
            Backend->>DB: Insert WebhookEventRecord
            Backend->>DB: Idempotently update Payment & Subscription
            Backend-->>Rzp: HTTP 200 (Success)
        end
    end
```

#### Why Raw Request Bodies Are Mandatory for Webhooks
When Razorpay sends a webhook, it calculates the HMAC signature over the exact byte stream sent across the wire:
$$Signature = HMAC\_SHA256(RawBodyBytes, WebhookSecret)$$
If your Spring controller takes a deserialized object, Jackson will re-serialize it when verifying, altering whitespace, field ordering, and escaping, breaking the signature!
Subscribly takes `String rawPayload` directly, computes the HMAC, verifies constant-time equality, and only then deserializes the JSON.

---

### 6.4 Retry Strategy, Exponential Backoff & State Machines
When payment capture fails due to transient gateway issues (e.g., gateway timeout, bank network failure), Subscribly implements exponential backoff:
$$NextRetryAt = now() + (BaseInterval 	imes 2^{retry\_count}) + Jitter$$
In `PaymentService.java`:
- Maximum Retries: 3 attempts.
- Multiplier: Exponential ($2^0 	o 1m$, $2^1 	o 2m$, $2^2 	o 4m$).
- After 3 failed attempts, the payment permanently transitions to `FAILED`, and a `PaymentFailedEvent` is dispatched.

---

### 6.5 Financial Reconciliation & Drift Detection
In any real-world billing architecture, payments can fall out of synchronization between the gateway's ledger and the local database (e.g., due to catastrophic network partitioning or server restarts during transaction commit).
Subscribly incorporates a **Reconciliation Engine Specification**:
1. **Automated Settlement Ingestion:** A nightly scheduled task fetches the previous 24 hours of settled payments from the Razorpay Reconciliation API (`GET /v1/settlements`).
2. **Three-Way Matching:**
   - Gateway Order ID vs. Database `gateway_order_id`.
   - Captured Currency and Amount vs. Database `amount` and `currency`.
   - Fee and Tax Deduction vs. Expected Contract Rates.
3. **Discrepancy Remediation:** Any payment captured at the gateway but marked `FAILED` or `CREATED` locally is flagged as an `ANOMALY_CAPTURED_UNSYNCED` and automatically reconciled to `SUCCESS`, restoring customer entitlements.


---

# Chapter 7 — Concurrency, Locking, and Distributed Invariants

### 7.1 Optimistic vs. Pessimistic Locking: Engineering Depth

Concurrency control in financial and subscription systems is the thin line between reliable accounting and devastating double-billing incidents. In Subscribly, concurrency control is applied at multiple layers: application-level idempotent tokens, JPA entity versioning, and database isolation levels.

| Concurrency Mechanism | Implementation Pattern | Mechanics & SQL Generated | Latency & Contention Profile | Subscribly Use Case |
| :--- | :--- | :--- | :--- | :--- |
| **Optimistic Locking** | `@Version private Long version;` on JPA entities. | Generates `UPDATE subscriptions SET status = ?, version = version + 1 WHERE id = ? AND version = ?`. | Zero lock holding time; non-blocking reads; aborts conflicting writes via update count check (0 rows affected). | **Primary Architecture:** Subscription status transitions (cancellation, activation, renewal) and payment order updates. |
| **Pessimistic Locking** | `@Lock(LockModeType.PESSIMISTIC_WRITE)` on Spring Data repository methods. | Generates `SELECT * FROM accounts WHERE id = ? FOR UPDATE`. | Blocks all concurrent readers and writers; risks thread starvation and deadlocks if queries are unindexed or slow. | **Targeted Scenarios:** High-contention account balance deductions, wallet credits, or inventory decrementing. |

#### Optimistic Locking Implementation Details
In Subscribly, both `Subscription` and `Payment` entities declare `@Version private Long version;`. When Hibernate performs an entity update, it executes:

```sql
UPDATE subscriptions
SET status = 'CANCELLED',
    updated_at = '2026-10-18T19:30:00Z',
    version = 2
WHERE id = 101 AND version = 1;
```

Hibernate evaluates the JDBC return code (`PreparedStatement.executeUpdate()`). If the row was modified by an interleaved transaction, the return code is `0`. Hibernate converts this into:
```
org.springframework.orm.ObjectOptimisticLockingFailureException: Object of class [com.subscribly.model.Subscription] with identifier [101]: was updated or deleted by another transaction
```

#### Client Retry Algorithm with Exponential Backoff and Full Jitter
When an `ObjectOptimisticLockingFailureException` is caught, Subscribly uses an idempotent retry loop with jittered backoff rather than immediately failing:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionMutationService {

    private final SubscriptionRepository subscriptionRepository;
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 50;

    public Subscription cancelSubscriptionWithRetry(Long subscriptionId, Long userId) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                return executeCancel(subscriptionId, userId);
            } catch (ObjectOptimisticLockingFailureException ex) {
                attempt++;
                log.warn("Optimistic lock collision for subscription {} on attempt {}/{}", subscriptionId, attempt, MAX_RETRIES);
                if (attempt >= MAX_RETRIES) {
                    throw new ConflictException("Subscription state changed concurrently. Please refresh and retry.");
                }
                backoffWithJitter(attempt);
            }
        }
        throw new ConflictException("Unable to acquire consistent subscription state.");
    }

    private void backoffWithJitter(int attempt) {
        long exponentialDelay = BASE_DELAY_MS * (1L << attempt);
        long jitteredDelay = ThreadLocalRandom.current().nextLong(0, exponentialDelay);
        try {
            Thread.sleep(jitteredDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SystemException("Thread interrupted during optimistic retry", e);
        }
    }
}
```

---

### 7.2 Race Condition Deep-Dives in Subscribly

Financial subscription systems are prone to subtle edge-case races. Below are the four critical race conditions solved in Subscribly:

#### Race Condition 1: Double-Submit on Payment Order Creation
- **Trigger:** A customer clicks the "Subscribe Now" button twice in rapid succession (within 15 milliseconds), sending two concurrent `POST /api/payments/create-order` requests.
- **Vulnerability:** Without safeguards, two distinct Razorpay order IDs are generated, creating two pending payment records. If the user completes payment on both modals, their card is billed twice.
- **Subscribly Mitigation:**
  1. The frontend disables the checkout button immediately upon click.
  2. The client generates an idempotency key (UUID) sent in `X-Idempotency-Key` or derived from `userId + planId + currentCycleTimestamp`.
  3. The database enforces a `UNIQUE(idempotency_key)` constraint on the `payments` table.
  4. If Thread B attempts to insert a record with the same key while Thread A is committing, PostgreSQL throws `23505 (unique_violation)`. Subscribly intercepts `DataIntegrityViolationException` and returns HTTP 409 Conflict or retrieves Thread A's existing pending order.

#### Race Condition 2: Simultaneous Webhook Arrival and Client Redirect Verification
- **Trigger:** Razorpay fires a `payment.captured` webhook at the exact millisecond the user's browser executes the checkout callback redirect (`POST /api/payments/verify`).
- **Sequence Diagram:**

```mermaid
sequenceDiagram
    autonumber
    participant Browser as User Browser
    participant Gateway as API Gateway
    participant Razorpay as Razorpay Cloud
    participant PaymentSvc as Payment Service
    participant DB as PostgreSQL Database

    Razorpay->>Gateway: POST /api/payments/webhook (payment.captured)
    Browser->>Gateway: POST /api/payments/verify (razorpay_signature)
    
    par Concurrent Ingestion
        Gateway->>PaymentSvc: Thread A (Webhook: order_101)
        Gateway->>PaymentSvc: Thread B (Verify: order_101)
    end

    PaymentSvc->>DB: Thread A: SELECT * FROM payments WHERE order_id = 'order_101' FOR UPDATE
    Note over DB: Row locked by Thread A. Thread B blocks on lock acquisition.
    
    PaymentSvc->>PaymentSvc: Thread A: Verify HMAC-SHA256 signature
    PaymentSvc->>DB: Thread A: UPDATE payments SET status = 'SUCCESS', version = version + 1
    PaymentSvc->>PaymentSvc: Thread A: Activate Subscription
    PaymentSvc-->>Razorpay: Thread A returns HTTP 200 OK

    Note over DB: Lock released. Thread B acquires lock and reads committed row.
    PaymentSvc->>PaymentSvc: Thread B checks: status == SUCCESS?
    Note over PaymentSvc: Already SUCCESS! Skip activation, return idempotent 200 OK.
    PaymentSvc-->>Browser: Thread B returns HTTP 200 OK (Payment Verified)
```

#### Race Condition 3: Scheduler Renewal vs. User Cancellation
- **Trigger:** At 00:00:01 UTC, the automated renewal scheduler queries subscriptions expiring today and initiates an auto-debit charge. At 00:00:02 UTC, the user clicks "Cancel Subscription".
- **Resolution:**
  1. Scheduler reads Subscription 101 with `version = 3`.
  2. User cancellation controller reads Subscription 101 with `version = 3`.
  3. User cancellation commits first: updates `status = 'CANCELLED'`, increments `version = 4`.
  4. Scheduler attempts to charge and update subscription with `WHERE id = 101 AND version = 3`.
  5. The query updates 0 rows; Spring throws `ObjectOptimisticLockingFailureException`.
  6. The renewal transaction aborts immediately, preventing the credit card from being charged for a cancelled plan!

---

### 7.3 Transactional Outbox Pattern vs. Distributed 2PC

When a payment succeeds, multiple actions must occur:
1. Update `payment` record status to `SUCCESS`.
2. Update `subscription` record status to `ACTIVE`.
3. Emit a notification event to dispatch an invoice email to the customer.

In a microservices architecture, executing step 3 via a direct HTTP call or a synchronous message broker publish inside the database transaction creates a dual-write problem:

```
[Service Process]
   │
   ├── 1. DB Transaction Begin
   ├── 2. UPDATE payments SET status = 'SUCCESS'
   ├── 3. kafkaProducer.send(PaymentSuccessEvent) ──► [Network Glitch / Broker Timeout!]
   └── 4. DB Transaction Commit (Or Rollback?)
```

- If Kafka fails or times out, the DB transaction rolls back, but Kafka may have actually received the message (duplicate invoice sent for uncommitted payment).
- If Kafka succeeds, but the database crashes before commit, the customer receives an invoice for a payment that does not exist in the database.

#### The Transactional Outbox Solution
Subscribly utilizes the Transactional Outbox pattern. Rather than publishing directly to an external message broker inside the transactional boundary, the event payload is inserted into a local `outbox_events` table in the exact same database transaction:

```sql
-- Executed inside a single atomic ACID transaction:
UPDATE payments 
SET status = 'SUCCESS', updated_at = NOW(), version = version + 1 
WHERE id = 501;

UPDATE subscriptions 
SET status = 'ACTIVE', expires_at = NOW() + INTERVAL '30 days', version = version + 1 
WHERE id = 101;

INSERT INTO outbox_events (
    id, aggregate_type, aggregate_id, event_type, payload, status, created_at
) VALUES (
    gen_random_uuid(), 'PAYMENT', '501', 'PAYMENT_CAPTURED',
    '{"paymentId":501,"userId":42,"amount":49.00,"currency":"USD"}',
    'PENDING', NOW()
);
```

#### Event Dispatch Mechanics
An asynchronous publisher process reads the `outbox_events` table:
1. **Polling Publisher:** A scheduled Spring task runs every 500ms querying `SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 100 FOR UPDATE SKIP LOCKED`.
2. **Debezium Change Data Capture (CDC):** In production, Debezium streams PostgreSQL Write-Ahead Logs (WAL) directly into Kafka topics with zero polling latency and zero overhead on database tables.
3. Once published to Kafka, the outbox record is marked `PUBLISHED` or archived.

---

# Chapter 8 — Modular Monolith vs. Distributed Microservices

### 8.1 Exhaustive Architectural Trade-Off Matrix

Subscribly was engineered specifically to demonstrate both a high-efficiency **Modular Monolith** (`/monolith`) and a distributed **Spring Cloud Microservices System** (`/services`). Understanding when to choose each architecture is one of the highest-yield discussion topics in senior engineering interviews.

| Architectural Dimension | Subscribly Modular Monolith (`/monolith`) | Distributed Microservices (`/services`) | Architectural Winner & Context |
| :--- | :--- | :--- | :--- |
| **Deployment Complexity** | Single artifact (`subscribly.jar`), 1 Docker container, single CI/CD deployment pipeline. | 10 independent artifacts, Eureka registry, Config Server, Gateway, 5+ container pipelines. | **Monolith:** Massive operational advantage for small teams and early-stage products. |
| **Cross-Domain Latency** | Sub-millisecond ($< 0.1\text{ ms}$). In-memory method invocations via JVM stack and heap pointers. | 5–45 ms network overhead per call (TCP socket open, TLS handshake, HTTP/1.1 parsing, JSON serialization). | **Monolith:** Zero network tax for inter-domain transactions. |
| **Transaction Boundaries** | Native ACID transactions using Spring `@Transactional`. Rollbacks span user, subscription, and payment tables atomically. | Dual-writes, eventual consistency. Requires Outbox pattern, Saga orchestrators, and compensating transactions. | **Monolith:** Zero risk of partial failure during multi-entity mutations. |
| **Fault Isolation** | Shared memory space. An OutOfMemoryError, thread leak, or infinite loop in `notification` can crash `payment`. | Total process isolation. If `notification-service` runs out of memory, `payment-service` continues capturing revenue. | **Microservices:** Resilient blast radius control for mission-critical paths. |
| **Horizontal Scalability** | Coarse-grained scaling. Must scale the entire monolith even if only `payment-service` experiences high traffic. | Fine-grained scaling. Can run 50 pods of `payment-service` during flash sales while leaving `user-service` at 2 pods. | **Microservices:** Drastically lower cloud infrastructure costs at extreme scale. |
| **Data Ownership & Isolation** | Unified PostgreSQL database schema. Foreign keys directly link `users`, `subscriptions`, and `payments`. | Bounded contexts with private schemas. No cross-service foreign keys. Data queried via REST or replicated via Kafka. | **Microservices:** Clean domain boundaries; prevents accidental coupling. |
| **Observability Overhead** | Standard application logs, single thread trace, centralized exception tracking. | Mandatory distributed tracing (Zipkin, Jaeger, OpenTelemetry, Correlation IDs across network hops). | **Monolith:** Trivial debugging and stack-trace readability. |
| **Engineering Team Cadence** | Single shared git repository. Requires branch protection, merge coordination, and trunk-based discipline. | Independent repositories, isolated release cycles, independent team ownership per microservice domain. | **Microservices:** Allows 100+ engineers across 10 teams to deploy without blocking each other. |

---

### 8.2 Why Microservices Are Not Automatically Better

One of the key traits distinguishing an SDE-3 from an SDE-1 is the refusal to treat microservices as an automatic virtue. Distributed systems introduce significant complexity known as the **Fallacies of Distributed Computing**:
1. *The network is reliable:* Networks drop packets, DNS lookups fail, and routers experience packet corruption.
2. *Latency is zero:* Making 5 sequential REST calls across microservices adds 150ms of pure latency before any database query executes.
3. *Bandwidth is infinite:* Serializing and deserializing large JSON payloads across 20 services consumes massive CPU and network bandwidth.
4. *The network is secure:* Traffic between microservices requires internal mTLS encryption and network policies.
5. *Topology does not change:* Pods are ephemeral; IP addresses change constantly, necessitating service registries like Netflix Eureka.

**The Subscribly Verdict:** If your engineering organization is fewer than 25 developers and your traffic fits comfortably on a single well-provisioned PostgreSQL instance with read replicas (handling up to 10,000 requests/sec), a **Modular Monolith** is vastly superior in development velocity, operational cost, and correctness. Microservices should be adopted only when team coordination overhead or independent scaling requirements demand process boundaries.

---

### 8.3 Subscribly Service Boundaries & Domain Ownership

In Subscribly, the distributed architecture is organized around strict domain-driven bounded contexts:

```mermaid
flowchart TD
    Client[Web Browser / Mobile Client] -->|HTTPS :8080| Gateway[Spring Cloud API Gateway]
    
    subgraph Registry & Configuration
        Eureka[Netflix Eureka :8761]
        Config[Spring Cloud Config Server :8888]
    end

    Gateway -.->|Route Lookup| Eureka
    Gateway -->|/api/auth/**| UserSvc[User Service :8081]
    Gateway -->|/api/subscriptions/**| SubSvc[Subscription Service :8082]
    Gateway -->|/api/payments/**| PaySvc[Payment Service :8083]

    SubSvc -->|Feign Client| UserSvc
    PaySvc -->|Feign Client| SubSvc
    PaySvc -->|Feign Client| UserSvc

    PaySvc -.->|Publish 'payment-captured'| Kafka[(Apache Kafka :9092)]
    SubSvc -.->|Publish 'subscription-cancelled'| Kafka
    Kafka -.->|Subscribe| NotifSvc[Notification Service :8084]
```

#### Service Inventory
1. **`api-gateway` (Port 8080):** Central entry point. Validates JWT tokens, strips sensitive headers, injects `X-User-Id` and `X-User-Role`, manages rate limits, and routes requests to backend services.
2. **`discovery-server` (Port 8761):** Netflix Eureka registry. Services send heartbeats every 30 seconds to register health.
3. **`config-server` (Port 8888):** Externalized configuration management backed by git or native filesystem profiles.
4. **`common-lib`:** Shared domain DTOs (`UserDto`, `SubscriptionDto`), global exception envelopes (`ApiResponse`), and security constants.
5. **`user-service` (Port 8081):** User registration, BCrypt authentication, JWT generation and validation, profile management.
6. **`subscription-service` (Port 8082):** Plan definitions, subscription state transitions, lifecycle boundaries, auto-renew scheduler.
7. **`payment-service` (Port 8083):** Razorpay order creation, raw-body HMAC-SHA256 webhook validation, state machine transitions, audit logging.
8. **`notification-service` (Port 8084):** Asynchronous email/SMS dispatcher consuming Kafka domain events.

---

### 8.4 Inter-Service Communication & Failure Semantics

Subscribly divides inter-service communication into two distinct patterns:

#### 1. Synchronous REST with OpenFeign (Strict Dependency Check)
Used when a service cannot proceed without immediate data:
```java
@FeignClient(name = "subscription-service", fallback = SubscriptionFallback.class)
public interface SubscriptionServiceClient {
    @GetMapping("/api/subscriptions/{id}")
    ApiResponse<SubscriptionDto> getSubscriptionById(@PathVariable("id") Long id);
}
```
**Failure Mode & Fallback:** If `subscription-service` is down, OpenFeign triggers the fallback class:
```java
@Component
public class SubscriptionFallback implements SubscriptionServiceClient {
    @Override
    public ApiResponse<SubscriptionDto> getSubscriptionById(Long id) {
        log.error("Subscription Service unavailable for ID {}. Triggering fallback.", id);
        throw new ServiceUnavailableException("Subscription service temporarily unavailable. Please retry shortly.");
    }
}
```

#### 2. Asynchronous Event-Driven Messaging with Apache Kafka
Used for decoupled notifications and analytical side effects:
- Publisher writes `PaymentCompletedEvent` to Kafka topic `subscribly.payments`.
- If `notification-service` crashes, Kafka retains the message on the partition log. When the notification service restarts, its consumer group resumes from the committed offset, guaranteeing zero dropped notifications.

---

# Chapter 9 — Caching, Redis, and High-Throughput Optimization

### 9.1 PostgreSQL as the Financial Source of Truth

In high-throughput financial architectures, there is an absolute rule: **Redis is an acceleration layer, never an authoritative ledger.**
- **PostgreSQL Properties:** ACID transactions, Write-Ahead Logging (WAL) flushed to durable non-volatile disk via `fsync`, Point-in-Time Recovery (PITR), and strict foreign key integrity.
- **Redis Properties:** In-memory key-value store with asynchronous persistence (RDB snapshots or AOF append). In the event of a power outage or master node crash before AOF sync, data in memory is permanently lost.
- **Architectural Policy in Subscribly:** Payment records, active subscription state, and user balances are written directly to PostgreSQL. Redis is deployed exclusively for cache-aside reads, distributed rate limiting, and short-lived ephemeral coordination.

---

### 9.2 Legitimate Redis Workloads in Subscribly

```mermaid
flowchart TD
    Client[Incoming API Request] --> Gateway[API Gateway]
    
    subgraph Redis Cluster
        RateLimit[(Key: 'ratelimit:user_42' / TTL: 60s)]
        PlanCache[(Key: 'catalog:plans' / TTL: 24h)]
        TokenBlacklist[(Key: 'blacklist:token_xyz' / TTL: Expiry)]
    end
    
    Gateway -->|Check Sliding Window| RateLimit
    Gateway -->|Verify Token Revoked?| TokenBlacklist
    Gateway --> Service[Subscription Service]
    Service -->|Read Cached Plans| PlanCache
    Service -.->|On Cache Miss| PG[(PostgreSQL Database)]
```

1. **Sliding-Window Rate Limiting:** Enforces maximum 60 requests per minute per IP or authenticated user at the API Gateway using a Redis sorted set (`ZADD` and `ZREMRANGEBYSCORE`).
2. **Plan Catalog Caching:** Subscription plans (Basic $10, Pro $50, Enterprise $200) change rarely (once per quarter) but are queried on every single page load and checkout flow. Cached in Redis with a 24-hour TTL and explicit cache eviction on admin update.
3. **Stateless JWT Blacklist:** When a user logs out before their JWT expires, the token JTI is written to Redis with a TTL equal to the token's remaining lifespan. The Gateway checks this key in $< 1\text{ ms}$.

---

### 9.3 Cache-Aside Pattern, Invalidation, and Memory Policies

#### Cache-Aside Implementation
```java
@Service
@RequiredArgsConstructor
public class PlanCatalogService {

    private final PlanRepository planRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_KEY = "catalog:plans";

    @SuppressWarnings("unchecked")
    public List<PlanDto> getActivePlans() {
        List<PlanDto> cached = (List<PlanDto>) redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            return cached;
        }
        
        List<PlanDto> dbPlans = planRepository.findAllActive().stream()
                .map(this::toDto)
                .toList();
                
        redisTemplate.opsForValue().set(CACHE_KEY, dbPlans, Duration.ofHours(24));
        return dbPlans;
    }

    @Transactional
    public PlanDto updatePlan(Long planId, PlanUpdateRequest request) {
        Plan plan = planRepository.findById(planId).orElseThrow();
        plan.setPrice(request.getPrice());
        planRepository.save(plan);
        
        // Explicit Cache Eviction to prevent stale reads
        redisTemplate.delete(CACHE_KEY);
        return toDto(plan);
    }
}
```

#### Cache Eviction Policy Selection
In `redis.conf`, Subscribly configures:
```
maxmemory 256mb
maxmemory-policy volatile-lru
```
- `volatile-lru` evicts keys with an expiration (TTL) set, using the Least Recently Used algorithm when memory exceeds 256MB. This protects permanent operational keys while shedding stale read caches.

---

### 9.4 Cache Stampedes, Penetration, and Avalanche Mitigations

| Failure Mode | Trigger Mechanism | Engineering Defense Implemented |
| :--- | :--- | :--- |
| **Cache Stampede (Thundering Herd)** | A popular key (`catalog:plans`) expires under 10,000 QPS. All 10,000 threads miss cache and simultaneously query PostgreSQL, causing database connection exhaustion. | **Distributed Mutex Lock:** The first thread to detect a miss acquires a Redis lock (`SET lock:plans 1 NX PX 3000`). Only this thread queries the DB; the other 9,999 wait or return stale cache. |
| **Cache Penetration** | An attacker sends 50,000 requests/sec for non-existent IDs (`/api/subscriptions/99999999`). Every request bypasses the cache and queries PostgreSQL. | **Bloom Filter & Null Object Caching:** Cache the non-existent key with a short 2-minute TTL (`SET sub:99999999 NULL EX 120`), or evaluate against a Bloom filter before hitting the database. |
| **Cache Avalanche** | 100,000 keys are seeded at application startup with identical 24-hour TTLs. At hour 24, all 100,000 keys expire at the same second. | **Jittered Expiration:** Add random noise to TTLs: `TTL = BASE_TTL + ThreadLocalRandom.current().nextInt(0, 300)` (seconds). |

---

### 9.5 Database Performance Engineering: Keyset Pagination & N+1 Elimination

#### The Fatal Cost of `OFFSET` Pagination
When querying large payment tables, developers frequently write:
```sql
SELECT * FROM payments WHERE user_id = 42 ORDER BY created_at DESC LIMIT 20 OFFSET 50000;
```
PostgreSQL must scan 50,020 rows in index order, discard the first 50,000, and return the remaining 20. Under high offset values, response time degrades from 2ms to 1,500ms.

#### Keyset (Cursor-Based) Pagination
Subscribly replaces offset queries with keyset pagination:
```sql
SELECT id, amount, status, created_at 
FROM payments 
WHERE user_id = 42 AND id < :lastSeenPaymentId 
ORDER BY id DESC 
LIMIT 20;
```
Because `id` is the primary key (indexed in a B-Tree), PostgreSQL jumps directly to `:lastSeenPaymentId` in $O(\log N)$ time, executing in 0.8ms regardless of whether the table contains 10,000 rows or 50,000,000 rows.

#### Eliminating the N+1 Query Disaster
In the monolithic `Subscription` entity, mapping `@ManyToOne(fetch = FetchType.LAZY) private User user;` causes an N+1 query disaster if queried via `findAll()`: 1 query for 100 subscriptions, followed by 100 separate queries for each user.
- **The Solution:** Use JPQL `JOIN FETCH`:
```java
@Query("SELECT s FROM Subscription s JOIN FETCH s.user WHERE s.status = :status")
List<Subscription> findAllActiveWithUser(@Param("status") SubscriptionStatus status);
```
This executes a single SQL inner join, collapsing 101 database roundtrips into 1.

---

# Chapter 10 — Security Architecture, Cryptography, and RBAC

### 10.1 BCrypt Password Hashing & Salt Physics

Subscribly strictly forbids storing plaintext passwords or fast hashes (MD5, SHA-256) which can be cracked at billions of guesses per second using consumer GPUs.

```mermaid
flowchart LR
    Password[Plaintext Password: 'SecretPassword123!'] --> Salt[128-bit Cryptographic Salt]
    Salt --> BCrypt[BCrypt Engine - Cost Factor 12]
    BCrypt -->|2^12 = 4,096 Key Expansion Rounds| HashResult["$2a$12$e8uqY9zP1... (60 Chars)"]
    HashResult --> PG[(PostgreSQL 'users' table)]
```

#### The Physics of Work Factor 12
- BCrypt's work factor is logarithmic: $2^{\text{cost}}$. Cost 12 requires 4,096 iterations of Blowfish key scheduling.
- On a modern server CPU, calculating a cost 12 hash takes approximately **250 to 350 milliseconds**.
- An attacker with an RTX 4090 GPU capable of computing 10,000,000,000 MD5 hashes/second can only compute ~2,500 BCrypt cost 12 hashes/second, making brute-force dictionary attacks economically impossible.
- The 128-bit salt is uniquely generated per user and embedded in the output string, completely neutralizing precomputed rainbow tables.

---

### 10.2 JWT Architecture & Cryptographic Verification

Authentication across Subscribly is built on JSON Web Tokens (RFC 7519).

#### Token Structure
```
[Header: {"alg":"HS256","typ":"JWT"}] .
[Payload: {"sub":"42","email":"alex@subscribly.com","role":"ROLE_USER","iat":1776510000,"exp":1776513600}] .
[Signature: HMACSHA256(base64UrlEncode(Header) + "." + base64UrlEncode(Payload), secret)]
```

#### Symmetric (HS256) vs. Asymmetric (RS256) Trade-Offs
- **Current Subscribly Implementation (HS256):** A shared secret string is configured in `api-gateway` and internal services. Fast, simple, and mathematically identical on both sides.
- **Enterprise Asymmetric Architecture (RS256):** `user-service` holds a 2048-bit RSA **private key** and signs tokens. Downstream services (`payment-service`, `subscription-service`, Gateway) only hold the corresponding **public key**.
  - *Advantage:* A security breach in `notification-service` cannot expose the signing key; compromised worker services cannot mint fraudulent admin tokens.

#### Mitigating Token Revocation Limitations
Stateless JWTs cannot be revoked natively until they expire (`exp`). Subscribly implements a dual-defense strategy:
1. **Short Lifespan:** Access tokens expire in 15 minutes.
2. **Redis Revocation Blacklist:** On password reset or logout, the token identifier (`jti`) is added to Redis with a TTL equal to the token's remaining time. The JWT authentication filter checks Redis in $< 0.5\text{ ms}$.

---

### 10.3 Role-Based Access Control (RBAC) & Ownership Invariants

Security in Subscribly follows the **Principle of Least Privilege**:
- Roles: `ROLE_USER` and `ROLE_ADMIN`.
- Method-level authorization is enforced via Spring Security `@PreAuthorize`:

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/api/admin/metrics")
public ApiResponse<AdminMetricsDto> getSystemMetrics() {
    return ApiResponse.success(analyticsService.calculateMetrics());
}
```

#### Ownership Verification Check (Horizontal Privilege Escalation Defense)
A common vulnerability (Insecure Direct Object Reference - IDOR) occurs when an authenticated user with ID 42 accesses `/api/subscriptions/105` belonging to user 99.
Subscribly enforces strict resource ownership at the service layer:

```java
public SubscriptionDto getSubscription(Long subscriptionId, UserPrincipal principal) {
    Subscription sub = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
            
    // Verify ownership unless caller has administrative privileges
    if (!sub.getUserId().equals(principal.getId()) && !principal.isAdmin()) {
        log.warn("Security Alert: User {} attempted to access subscription {} owned by {}",
                principal.getId(), subscriptionId, sub.getUserId());
        throw new AccessDeniedException("You do not have permission to view this resource");
    }
    return toDto(sub);
}
```

#### Last Admin Demotion Safeguard
To prevent accidental administrative lockout, the user service verifies that demoting an admin or deactivating an account leaves at least one active user with `ROLE_ADMIN` in the database.

---

### 10.4 Webhook Authenticity & Anti-Replay Safeguards

Webhooks from payment gateways are vulnerable to two attack vectors:
1. **Man-In-The-Middle / Forgery:** An attacker sends fake `payment.captured` webhooks to credit user accounts without charging a card.
2. **Replay Attacks:** An attacker captures a valid signed webhook and replays it 100 times to extend a subscription by 100 months.

#### Cryptographic HMAC-SHA256 Verification
Subscribly captures the **exact raw request body bytes** before any JSON parsing occurs. It computes an HMAC-SHA256 hash using the configured webhook secret and verifies it against the `X-Razorpay-Signature` header:

```java
public boolean verifyWebhookSignature(String rawBody, String signature, String secret) {
    try {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(secretKey);
        byte[] hash = hmac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
        String expectedSignature = HexFormat.of().formatHex(hash);
        
        // Constant-time comparison to prevent side-channel timing attacks
        return MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            signature.getBytes(StandardCharsets.UTF_8)
        );
    } catch (Exception ex) {
        log.error("HMAC verification failed", ex);
        return false;
    }
}
```

#### Anti-Replay Deduplication Table
To defeat replay attacks, Subscribly stores every verified webhook event ID in a `webhook_events` table:

```sql
CREATE TABLE webhook_events (
    id VARCHAR(128) PRIMARY KEY, -- 'evt_live_xxxx' from Razorpay
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

When a webhook arrives, Subscribly checks:
```java
if (webhookEventRepository.existsById(eventPayload.getId())) {
    log.info("Duplicate webhook event {} detected. Acknowledging with HTTP 200 without reprocessing.", eventPayload.getId());
    return ResponseEntity.ok().build();
}
```
This guarantees strict idempotency even if an attacker or the gateway replays the event.

---

### 10.5 Safe Secret Management & Historical Remediation

#### The Golden Rules of Production Secret Hygiene
1. **Never Commit Secrets:** Passwords, API keys, and private certificates must never exist in git repositories or Dockerfiles.
2. **Environment Variable Injection:** Credentials must be supplied via OS environment variables (`$env:RAZORPAY_KEY_SECRET`) or secure vaults (HashiCorp Vault, AWS Secrets Manager).
3. **Log Redaction:** SLF4J log formatters must sanitize credit card numbers, CVVs, authorization headers, and JWT tokens to prevent secrets leaking into logging pipelines (Datadog, CloudWatch).

#### Remediation Protocol for Historical Key Exposure
If an API key is ever committed to a public or semi-private repository:
1. **Assume Immediate Compromise:** Automated bot crawlers scrape public commits in $< 30\text{ seconds}$.
2. **Rotate Immediately at the Provider:** Generate a new key in the Razorpay / Stripe dashboard immediately and revoke the exposed key.
3. **Do Not Rely on History Rewriting:** Rewriting git history via `git filter-branch` or `BFG Repo-Cleaner` changes commit hashes, breaking all downstream branches and clone integrity. Furthermore, forks, local clones, and web scrapers retain the exposed commit. **The only real defense is rotation.**

---

# Chapter 11 — REST API Design, Error Contracts, and Frontend Integration

### 11.1 REST Conventions & Standardized HTTP Semantics

Subscribly strictly adheres to HTTP/1.1 and REST standards:

| HTTP Verb | Path | Action & Idempotency | Expected Status Codes |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/auth/register` | Create user resource (Non-idempotent) | 201 Created (Success), 409 Conflict (Email taken) |
| **POST** | `/api/payments/create-order` | Initialize checkout order (Idempotent via key) | 200 OK, 400 Bad Request, 409 Conflict |
| **GET** | `/api/subscriptions/my` | Retrieve current user's subscription (Idempotent) | 200 OK, 401 Unauthorized |
| **PUT** | `/api/subscriptions/{id}/cancel` | Transition subscription to cancelled (Idempotent) | 200 OK, 404 Not Found, 409 State Conflict |
| **DELETE**| `/api/admin/plans/{id}` | Deactivate subscription plan (Idempotent) | 204 No Content, 404 Not Found |

---

### 11.2 Standardized Error Envelope (RFC 7807)

Rather than returning disparate stack traces or plain strings, Subscribly uses a standardized `ApiResponse<T>` envelope compliant with RFC 7807 (Problem Details for HTTP APIs):

```json
{
  "success": false,
  "timestamp": "2026-10-18T19:40:12.185Z",
  "status": 409,
  "error": "ConflictException",
  "message": "Subscription 101 is already active and cannot be re-purchased.",
  "path": "/api/payments/create-order",
  "validationErrors": null
}
```

#### Global Exception Handler Implementation
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex, HttpServletRequest request) {
        log.warn("Business conflict at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.validationError(errors, request.getRequestURI()));
    }
}
```

---

### 11.3 Frontend Integration Architecture (React 18 + Vite)

The Subscribly client is a single-page application built with React 18, Vite, TypeScript, and Tailwind CSS located in `/frontend`.

#### Axios Token Interceptor & Silent 401 Handling
```typescript
import axios from 'axios';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' }
});

// Automatic JWT Injection
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('subscribly_token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 401 Expiration Interceptor
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('subscribly_token');
      window.location.href = '/login?expired=true';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

#### Razorpay Modal Checkout Integration
```typescript
export const triggerRazorpayCheckout = (orderData: OrderResponse, onSuccess: () => void) => {
  const options = {
    key: orderData.razorpayKeyId,
    amount: orderData.amountInPaise,
    currency: orderData.currency,
    name: 'Subscribly',
    description: `Subscription: ${orderData.planName}`,
    order_id: orderData.razorpayOrderId,
    handler: async (response: RazorpayPaymentResponse) => {
      // Direct verification call
      await apiClient.post('/api/payments/verify', {
        razorpayOrderId: response.razorpay_order_id,
        razorpayPaymentId: response.razorpay_payment_id,
        razorpaySignature: response.razorpay_signature
      });
      onSuccess();
    },
    prefill: {
      email: orderData.userEmail
    },
    theme: {
      color: '#6366F1'
    }
  };

  const rzp = new (window as any).Razorpay(options);
  rzp.open();
};
```

---

# Chapter 12 — Testing Strategy, Verification Matrix, and Test Doubles

### 12.1 The Testing Pyramid in Subscribly

```mermaid
pie title Subscribly Test Distribution (205 Automated Tests)
    "Monolith Unit & Integration Tests" : 172
    "Payment Service (Gateway, Webhook HMAC, State Machine)" : 23
    "User Service (JWT, BCrypt, Auth Filters)" : 7
    "Subscription Service (FSM, Expiry Calculations)" : 3
```

Subscribly maintains a strict test hierarchy:
1. **Unit Tests (Fast, Isolated, Deterministic):** Business state machine validation, HMAC-SHA256 calculation, JWT generation, and entity mathematical calculations. Dependencies mocked with Mockito. Executed in $< 20\text{ seconds}$.
2. **Service Integration Tests (`@SpringBootTest`):** Spring context loading, transactional rollbacks, Spring Data repository queries, and custom JPQL query verification.
3. **Web Layer Slice Tests (`@WebMvcTest`):** Spring Security filter chains, authorization rules, JSON serialization/deserialization, and validation annotations (`@NotBlank`, `@Min`).

---

### 12.2 Verified Maven Reactor Test Matrix (205 Passed Tests)

On **October 18, 2026**, the entire Maven reactor was executed with Azul Zulu OpenJDK 21 LTS (`21.0.7`):

```powershell
$env:JAVA_HOME = "C:\Users\HIMANSHU\.jdks\azul-21.0.7"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

#### Reactor Module Verification Results:

| Module Directory | Artifact ID | Tests Run | Failures | Errors | Skipped | Elapsed Time | Build Status |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| `.` (Root) | `subscribly-parent` | 0 | 0 | 0 | 0 | 0.003s | `SUCCESS` |
| `monolith/` | `subscribly` | **172** | **0** | **0** | **0** | 24.795s | `SUCCESS` |
| `discovery-server/` | `discovery-server` | 0 | 0 | 0 | 0 | 0.181s | `SUCCESS` |
| `api-gateway/` | `api-gateway` | 0 | 0 | 0 | 0 | 0.154s | `SUCCESS` |
| `config-server/` | `config-server` | 0 | 0 | 0 | 0 | 0.126s | `SUCCESS` |
| `common-lib/` | `common-lib` | 0 | 0 | 0 | 0 | 0.039s | `SUCCESS` |
| `user-service/` | `user-service` | **7** | **0** | **0** | **0** | 1.083s | `SUCCESS` |
| `notification-service/` | `notification-service` | 0 | 0 | 0 | 0 | 0.075s | `SUCCESS` |
| `subscription-service/`| `subscription-service`| **3** | **0** | **0** | **0** | 2.402s | `SUCCESS` |
| `payment-service/` | `payment-service` | **23** | **0** | **0** | **0** | 2.679s | `SUCCESS` |
| **REACTOR TOTAL** | **10 Modules** | **205** | **0** | **0** | **0** | **31.91s** | **100% PASS** |

---

### 12.3 Deterministic Clock Injection in Lifecycle Tests

Testing expiration boundaries without brittle `Thread.sleep()` is achieved by injecting a `java.time.Clock` bean:

```java
@Configuration
public class ClockConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
```

In test suites, time is manipulated deterministically:
```java
@Test
void subscriptionShouldExpireAfterExactly30Days() {
    Instant start = Instant.parse("2026-01-01T00:00:00Z");
    Clock fixedClock = Clock.fixed(start, ZoneOffset.UTC);
    
    Subscription sub = subscriptionService.createMonthlySubscription(userId, planId, fixedClock);
    assertThat(sub.getExpiresAt()).isEqualTo(Instant.parse("2026-01-31T00:00:00Z"));
    
    // Time travel 31 days into the future
    Clock futureClock = Clock.fixed(start.plus(31, ChronoUnit.DAYS), ZoneOffset.UTC);
    boolean isExpired = subscriptionService.evaluateExpired(sub, futureClock);
    assertThat(isExpired).isTrue();
}
```

---

# Chapter 13 — Reliability, Observability, and Operational Telemetry

### 13.1 Distributed Tracing & Correlation IDs

In a microservices topology, tracking a single payment request across Gateway, User Service, and Payment Service requires an immutable **Correlation ID**:

```mermaid
flowchart LR
    Client[Client Request] -->|Inject X-Correlation-ID: uuid-99| GW[API Gateway]
    GW -->|MDC: uuid-99| UserSvc[User Service]
    GW -->|MDC: uuid-99| PaySvc[Payment Service]
    PaySvc -->|Log: [uuid-99] Order created| AppLog[(Centralized Logs / Loki)]
```

#### Gateway Ingress Filter
```java
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {
    private static final String CORRELATION_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_HEADER, correlationId)
                .build();
                
        MDC.put("correlationId", correlationId);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
```

---

### 13.2 Spring Boot Actuator & Health Indicators

Subscribly exposes standard production health endpoints:
- `GET /actuator/health/liveness`: Checks if the JVM is alive and responsive.
- `GET /actuator/health/readiness`: Verifies that the PostgreSQL connection pool has available connections and Eureka heartbeat is active.
- `GET /actuator/metrics/hikaricp.connections.active`: Monitors connection pool utilization.

---

### 13.3 Graceful Shutdown & Zero-Downtime Deployment

When a Docker container receives `SIGTERM`, Spring Boot initiates a graceful shutdown:
```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```
1. The container stops accepting new incoming HTTP connections at the port level.
2. Active in-flight payment capture requests are given up to 30 seconds to finish committing to PostgreSQL.
3. Database connections are gracefully returned to the Hikari pool.
4. The JVM process terminates cleanly with exit code 0.

---

# Chapter 14 — Architectural Decision Matrix (10 Direct Comparisons)

Senior engineering interviews evaluate decision trade-offs rather than dogmatic tool choices. Below is Subscribly's definitive architectural decision matrix:

### 1. PostgreSQL vs. MongoDB
- **The Problem:** Selecting the persistence engine for user accounts, subscriptions, and financial payment transactions.
- **Current Choice:** PostgreSQL 18 (Relational DBMS).
- **Alternatives Considered:** MongoDB (Document Store).
- **Benefits:** Strict multi-table ACID transactions; foreign key constraints prevent orphaned subscriptions; storage-level unique constraints eliminate double-billing; SQL join expressiveness.
- **Costs:** Schema migrations require Flyway scripts; vertical scaling limits.
- **Failure Modes:** Schema migration locks during un-indexed table alters.
- **When to Overturn:** If Subscribly expanded into clickstream tracking or un-structured device telemetry generating 500,000 writes/second with zero financial consistency requirements.

### 2. Redis Caching vs. Direct Database Reads
- **The Problem:** Optimizing high-frequency reads for plan catalogs and user session validation.
- **Current Choice:** Direct DB reads for transactions; Redis cache-aside exclusively for static plan catalogs.
- **Alternatives Considered:** Caching all subscription and payment state in Redis.
- **Benefits:** Eliminates stale financial reads; avoids distributed cache invalidation bugs.
- **Costs:** Extra read queries hit PostgreSQL read replicas.
- **Failure Modes:** Cache stampede if TTL expires under spike load.
- **When to Overturn:** When read traffic on the plan catalog exceeds 5,000 QPS.

### 3. Modular Monolith vs. Distributed Microservices
- **The Problem:** Structuring code organization and deployment topology.
- **Current Choice:** Dual implementation: fully functional Modular Monolith (`/monolith`) alongside decompiled Spring Cloud microservices (`/services`).
- **Alternatives Considered:** Pure monolithic or pure microservices architecture.
- **Benefits:** Combines fast local developer iteration and single-command builds with the enterprise patterns of distributed services.
- **Costs:** Maintaining dual modules in the repository.
- **Failure Modes:** Inadvertent coupling if common libraries become too thick.
- **When to Overturn:** Deploy microservices independently once individual team sizes exceed 15 engineers per domain.

### 4. Synchronous REST (Feign) vs. Asynchronous Messaging (Kafka)
- **The Problem:** Coordinating inter-service workflows between Payment, Subscription, and Notification.
- **Current Choice:** OpenFeign for synchronous dependency validation; Kafka for decoupled notifications.
- **Alternatives Considered:** Pure synchronous REST for all flows.
- **Benefits:** Slow email providers (SendGrid) cannot block or roll back payment capture.
- **Costs:** Operational overhead of running a Kafka cluster.
- **Failure Modes:** Consumer lag on notification topics during bursts.
- **When to Overturn:** If notification delivery must be guaranteed within $< 100\text{ ms}$ synchronously.

### 5. Scheduled Polling vs. Event-Driven Expiration
- **The Problem:** Detecting and transitioning expired subscriptions.
- **Current Choice:** Scheduled batch polling via `@Scheduled(cron = "0 0 * * * ?")`.
- **Alternatives Considered:** Event-driven delay queues (RabbitMQ dead-letter exchange) or Redis keyspace notifications.
- **Benefits:** Deterministic batch execution; trivial recovery from missed runs; simple database query plans.
- **Costs:** Expiration occurs up to 59 minutes after the exact expiration second.
- **Failure Modes:** Multiple instances running cron without distributed locks.
- **When to Overturn:** When active subscriptions exceed 1,000,000, requiring continuous stream evaluation.

### 6. Optimistic Locking vs. Pessimistic Locking
- **The Problem:** Preventing concurrent updates to subscriptions and payments.
- **Current Choice:** Optimistic Locking via Hibernate `@Version`.
- **Alternatives Considered:** Pessimistic `SELECT FOR UPDATE` locking.
- **Benefits:** Non-blocking reads; zero database lock holding time; no risk of database-level deadlocks.
- **Costs:** Requires application-level retry loops with jitter.
- **Failure Modes:** Write collisions under extreme contention.
- **When to Overturn:** High-contention inventory decrementing or wallet balance deduction where collision probability exceeds 20%.

### 7. Stateless JWT vs. Stateful Server-Side Sessions
- **The Problem:** Managing client authentication across multiple backend services.
- **Current Choice:** Stateless JWT with HMAC-SHA256 signature.
- **Alternatives Considered:** Spring Session with Redis backing.
- **Benefits:** Zero database/cache lookups on internal microservice requests; trivial horizontal scaling.
- **Costs:** Cannot immediately revoke tokens without an auxiliary Redis blacklist.
- **Failure Modes:** Token payload bloat if too many claims are stored.
- **When to Overturn:** Strict compliance environments (banking/healthcare) requiring instantaneous session kill-switches.

### 8. Synchronous Payment Redirect vs. Webhook Ingestion
- **The Problem:** Capturing payment completion authoritatively.
- **Current Choice:** Dual-channel verification: synchronous client redirect verification + raw HMAC-SHA256 webhook ingestion.
- **Alternatives Considered:** Webhook-only or redirect-only verification.
- **Benefits:** Captures revenue even if the user closes their browser tab immediately after checkout.
- **Costs:** Requires idempotent state machines to handle concurrent confirmation.
- **Failure Modes:** Webhook delivery delays from gateway during outages.
- **When to Overturn:** Never. Dual-channel verification is mandatory for financial integrity.

### 9. Docker Compose vs. Kubernetes
- **The Problem:** Local developer environment orchestration vs production workload management.
- **Current Choice:** Docker Compose for local reproduction; Kubernetes architecture designs for production.
- **Alternatives Considered:** Minikube/Kind for local development.
- **Benefits:** Lightweight, single-command startup (`docker-compose up`); runs on developer laptops with 8GB RAM.
- **Costs:** Lacks automated autoscaling and canary rollouts.
- **Failure Modes:** Local port collisions if host ports conflict.
- **When to Overturn:** Production multi-region deployments with auto-healing and HPA requirements.

### 10. Self-Hosted Infrastructure vs. Managed Cloud Services
- **The Problem:** Hosting PostgreSQL, Kafka, and Redis.
- **Current Choice:** Local containerized instances for zero-cost development; AWS RDS / Confluent Cloud designs for production.
- **Alternatives Considered:** Self-hosting production PostgreSQL and Kafka on bare EC2 instances.
- **Benefits:** Managed automated backups, multi-AZ failover, automated patching.
- **Costs:** Higher cloud vendor billing.
- **Failure Modes:** Vendor lock-in.
- **When to Overturn:** If data sovereignty laws or extreme data transfer costs make cloud economics unviable.



# Chapter 15 — 60 In-Depth SDE-2/SDE-3 Interview Challenge Questions & Model Answers

### Domain 1: Java & Spring Boot Core Internals (Questions 1–10)

#### Q1: How does Spring Boot's `@Transactional` annotation work under the hood, and what causes it to silently fail?
**Model Answer:**
`@Transactional` relies on Spring AOP dynamic proxies (CGLIB or JDK dynamic proxies). When a caller invokes a transactional method on an injected bean, the proxy intercepts the call, obtains a connection from `DataSourceUtils`, sets `autoCommit = false`, and invokes the target method. If the method completes normally, the proxy commits the transaction; if an unhandled unchecked exception occurs, it initiates a rollback.
**Silent Failure Modes:**
1. **Self-Invocation:** Calling a `@Transactional` method from within the *same* class (e.g., `this.helperMethod()`) bypasses the proxy entirely! The call executes as plain Java without a transaction.
2. **Incorrect Exception Handling:** By default, Spring only rolls back on unchecked exceptions (`RuntimeException` and `Error`). If code throws a checked `Exception` without specifying `@Transactional(rollbackFor = Exception.class)`, Spring will commit the transaction anyway!
3. **Non-Public Methods:** Annotating `private` or `protected` methods with `@Transactional` is ignored by Spring AOP proxies.
**Subscribly Context:** In `payment-service`, `PaymentAuditService` is placed in its own dedicated class rather than being a helper method in `PaymentService` so that its `Propagation.REQUIRES_NEW` proxy can intercept calls properly.
**Adversarial Challenge:** *"What if method A has `@Transactional(propagation = Propagation.REQUIRES_NEW)` and is called by method B in the same class?"*
**Answer:** Because it is a self-invocation, the `REQUIRES_NEW` attribute is completely ignored; it runs within method B's transaction without creating a new physical transaction.
**SDE-3 Scale Reasoning:** Under 10,000 QPS, keeping transactions open across external network calls (e.g., calling Stripe/Razorpay inside a `@Transactional` block) holds database connections hostage, depleting HikariCP and taking down the entire database. Always execute external I/O *outside* database transaction boundaries.

#### Q2: What is the exact difference between `BeanFactory` and `ApplicationContext`?
**Model Answer:**
`BeanFactory` is the root interface providing basic IoC dependency injection and lazy bean initialization. `ApplicationContext` extends `BeanFactory`, adding enterprise features: eager singleton pre-instantiation, message resource internationalization (i18n), application event publishing (`ApplicationEventPublisher`), and integration with Spring AOP and environment profiles. Production applications always use `ApplicationContext`.
**Adversarial Challenge:** *"When would you ever prefer `BeanFactory` in production?"*
**Answer:** In memory-constrained embedded devices or AWS Lambda functions where cold-start time and minimal footprint are prioritized over enterprise features.

#### Q3: How do you prevent circular dependencies in Spring Boot 3?
**Model Answer:**
Spring Boot 2.6+ disables circular references by default, throwing `BeanCurrentlyInCreationException`.
**Solutions:**
1. **Redesign / Extract Shared Logic:** The cleanest fix is refactoring the mutual dependency into a third service.
2. **Lazy Initialization:** Annotating one injection point with `@Lazy` instructs Spring to inject a synthetic CGLIB proxy, resolving the actual bean instance only on first method invocation.
3. **Event-Driven Decoupling:** Replace the direct call with an `ApplicationEventPublisher` or Kafka event.

#### Q4: Explain the Spring Bean Lifecycle from classloading to destruction.
**Model Answer:**
1. Classloading & Bean Definition scanning (`@Component`, XML, `@Bean`).
2. Instantiation (reflection via constructor).
3. Populate properties (dependency injection).
4. `BeanNameAware`, `BeanFactoryAware`, `ApplicationContextAware` callbacks.
5. `BeanPostProcessor.postProcessBeforeInitialization()`.
6. `@PostConstruct` annotated methods.
7. `InitializingBean.afterPropertiesSet()`.
8. Custom `init-method`.
9. `BeanPostProcessor.postProcessAfterInitialization()` (AOP dynamic proxies generated here).
10. Bean is in-service.
11. On shutdown: `@PreDestroy` $	o$ `DisposableBean.destroy()` $	o$ custom `destroy-method`.

#### Q5: Why is `Propagation.REQUIRES_NEW` susceptible to database connection pool deadlocks under heavy load?
**Model Answer:**
When Method A (running in a transaction) calls Method B with `Propagation.REQUIRES_NEW`, Method A's database connection remains open and blocked while Method B requests a *second* connection from the HikariCP pool.
If your HikariCP pool size is 10, and 10 concurrent HTTP threads simultaneously execute Method A, all 10 connections are checked out. When each thread invokes Method B, every thread blocks waiting for an available connection from the empty pool! The threads deadlock each other until HikariCP throws `ConnectionTimeoutException`.
**Rule:** Ensure maximum pool size $\ge (TotalThreads 	imes 2) + 1$.

#### Q6: How does Spring Security handle authentication in async threads (`@Async`)?
**Model Answer:**
By default, `SecurityContextHolder` uses a `ThreadLocal` strategy, which does **not** propagate to newly spawned child threads.
**Fixes:**
1. Set `SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)`.
2. Wrap async executors with `DelegatingSecurityContextAsyncTaskExecutor`.

#### Q7: What is the performance impact of using Lombok's `@Data` on JPA entities?
**Model Answer:**
`@Data` automatically generates `@ToString`, `@EqualsAndHashCode`, and getters/setters.
1. **StackOverflowError:** If two entities have a bidirectional `@OneToMany` / `@ManyToOne` relationship, `@ToString` and `@EqualsAndHashCode` call each other infinitely.
2. **Performance:** Calling `equals()` or `hashCode()` triggers lazy loading of all relationship collections, causing massive unintended database queries.
**Subscribly Standard:** Use `@Getter`, `@Setter`, and `@Builder`. Avoid `@Data` and `@EqualsAndHashCode` on JPA entities; base identity solely on `@Id`.

#### Q8: How does Spring Cloud Feign handle client-side load balancing?
**Model Answer:**
Feign integrates with Spring Cloud LoadBalancer. When a Feign client specifies `@FeignClient(name = "user-service")`, it queries Eureka for all registered IP:port instances of `user-service`. The LoadBalancer applies a client-side algorithm (Round-Robin or Random) to route the HTTP request to an available healthy instance without requiring a hardware reverse proxy.

#### Q9: What happens when an unhandled exception escapes a `@Scheduled` method?
**Model Answer:**
If an unhandled exception is thrown, Spring's `ScheduledAnnotationBeanPostProcessor` catches it, logs an error, and the current execution aborts. By default, future scheduled runs continue, but if using a single-threaded task scheduler, an uncaught error in one task can block subsequent tasks. Always wrap scheduler bodies in `try-catch` blocks and log with structured contextual data.

#### Q10: How does Java 21 Virtual Threads (Project Loom) change Spring Boot concurrency?
**Model Answer:**
Traditional Spring Boot uses platform threads (1:1 mapping with OS threads). 1,000 idle threads waiting on database I/O consume ~1GB of RAM. Virtual threads are lightweight user-mode threads managed by the JVM. Enabling `spring.threads.virtual.enabled=true` allows Spring Boot to spawn a virtual thread per incoming HTTP request. When a virtual thread blocks on socket/database I/O, the underlying carrier OS thread is unmounted and serves other requests, enabling massive concurrency for I/O-bound microservices with near-zero memory overhead.

---

### Domain 2: Concurrency, Locking, and Transactions (Questions 11–20)

#### Q11: Explain how Hibernate Optimistic Locking detects concurrent modifications.
**Model Answer:**
Hibernate adds a version column (integer or timestamp) mapped via `@Version`. When an entity is loaded, Hibernate caches its version (e.g., 5). When writing changes, Hibernate issues:
```sql
UPDATE subscriptions 
SET status = 'CANCELLED', version = 6 
WHERE id = 100 AND version = 5;
```
Hibernate inspects the JDBC update count. If another thread already updated the row (changing version to 6), the update count is 0. Hibernate immediately throws `OptimisticLockException`, aborting the second transaction.

#### Q12: Walk through the exact race condition when a user cancels their subscription at the same moment an auto-renewal job triggers.
**Model Answer:**
1. Thread A (Scheduler) reads Subscription 100 (`status = ACTIVE`, `version = 2`).
2. Thread B (User Cancel API) reads Subscription 100 (`status = ACTIVE`, `version = 2`).
3. Thread B executes cancellation logic, setting `status = CANCELLED`. It issues `UPDATE subscriptions SET status = 'CANCELLED', version = 3 WHERE id = 100 AND version = 2`. The database updates 1 row; Thread B commits successfully.
4. Thread A attempts to renew: It issues `UPDATE subscriptions SET expires_at = '...', version = 3 WHERE id = 100 AND version = 2`.
5. Database returns `0 rows affected`.
6. Thread A catches `OptimisticLockingFailureException`. The renewal aborts. The user is **not** billed, preserving consistency.

#### Q13: When is Optimistic Locking insufficient, requiring Pessimistic Locking?
**Model Answer:**
When write contention is extremely high. If 1,000 users attempt to purchase the last 5 concert tickets simultaneously, 995 threads will fail optimistic locking and require costly retries. Pessimistic locking (`SELECT ... FOR UPDATE`) serializes access at the database row level, ensuring predictable FIFO processing under heavy contention.

#### Q14: Explain Database Isolation Levels and the anomalies they prevent.
**Model Answer:**
- **Read Uncommitted:** Prevents nothing; allows Dirty Reads, Non-repeatable Reads, Phantom Reads.
- **Read Committed (PostgreSQL Default):** Prevents Dirty Reads (queries only see committed data). Allows Non-repeatable reads and Phantom reads.
- **Repeatable Read:** Prevents Dirty Reads and Non-repeatable Reads (re-reading a row returns identical data).
- **Serializable:** Prevents all anomalies by enforcing strict serial execution order.

#### Q15: What is a "Phantom Read" and how does PostgreSQL prevent it in `Repeatable Read`?
**Model Answer:**
A Phantom Read occurs when Transaction A queries a range of rows (e.g., `WHERE status = 'ACTIVE'`), Transaction B inserts a new row matching that condition and commits, and Transaction A re-executes the query and sees the new "phantom" row.
PostgreSQL prevents this in `Repeatable Read` using **Multiversion Concurrency Control (MVCC)**: Queries only see a snapshot of committed data taken at the transaction's start timestamp.

#### Q16: How do you handle deadlocks in PostgreSQL?
**Model Answer:**
A deadlock occurs when Transaction 1 locks Row A and requests Row B, while Transaction 2 locks Row B and requests Row A. PostgreSQL's deadlock detector periodically scans the lock wait-graph, selects one transaction as the victim, and aborts it with `ERROR: deadlock detected`.
**Prevention:**
1. Always acquire locks in a globally consistent order (e.g., sort entity IDs before batch updating).
2. Keep transactions short.
3. Configure `deadlock_timeout` appropriately.

#### Q17: What is the difference between `@Transactional(readOnly = true)` and standard `@Transactional`?
**Model Answer:**
`readOnly = true` is a performance optimization:
1. **Hibernate:** Disables dirty checking on loaded entities. Hibernate does not maintain snapshot copies in memory, reducing heap usage.
2. **JDBC Driver:** In cloud setups with read replicas (e.g., AWS Aurora), the JDBC driver routes `readOnly` connections directly to read-only replica endpoints, offloading the master database.

#### Q18: What is the "Double Submit" problem, and how is it mitigated across the stack?
**Model Answer:**
A user rapidly double-clicks the "Pay" button on a slow network connection.
**Mitigation Layers:**
1. **Frontend:** Disable the button immediately on first click; show loading spinner.
2. **Gateway:** Rate limit requests per user token to 1 per second.
3. **Backend Service:** Require a client-generated `idempotency_key` (UUID). Cache in Redis with `NX PX 10000`. If key exists, reject with HTTP 409 Conflict.
4. **Database:** Unique constraint on `payment_reference`.

#### Q19: Why are Distributed Locks (e.g., Redlock) controversial in financial systems?
**Model Answer:**
Distributed systems expert Martin Kleppmann demonstrated that Redlock is unsafe under asynchronous network assumptions. A process acquiring a lock can experience an unexpected GC pause or network delay. While paused, the lock TTL expires, another process acquires the lock, and both processes execute concurrently!
**Safe Alternative:** Use **fencing tokens** (monotonically increasing version numbers verified by the database on every write).

#### Q20: Explain the Saga Pattern (Choreography vs. Orchestration).
**Model Answer:**
A Saga manages cross-service distributed transactions via compensating transactions.
- **Choreography:** Services listen to Kafka events and trigger local actions independently (e.g., Payment Service emits `PaymentCompleted`, Subscription Service listens and activates). Simple, but difficult to track flow.
- **Orchestration:** A central Orchestrator (e.g., Temporal) explicitly sends commands to services: "Charge Payment", then "Activate Subscription". If step 2 fails, the orchestrator issues "Refund Payment".

---

### Domain 3: Database Modeling & Performance (Questions 21–30)

#### Q21: What is the B-Tree index column ordering rule (Leftmost Prefix Rule)?
**Model Answer:**
In a composite index `CREATE INDEX idx_test ON table(A, B, C)`, the B-tree is sorted by A first, then B, then C.
- `WHERE A = 1` $	o$ Uses index.
- `WHERE A = 1 AND B = 2` $	o$ Uses index.
- `WHERE B = 2` $	o$ **Cannot use index!** (violates leftmost prefix rule).

#### Q22: Why should you avoid `OFFSET` pagination on large PostgreSQL tables?
**Model Answer:**
`SELECT * FROM payments ORDER BY id LIMIT 20 OFFSET 1000000;`
PostgreSQL must read 1,000,020 rows from disk, sort them, discard the first 1,000,000 rows, and return 20. Latency degrades linearly $O(N)$ with page depth.
**Solution: Keyset (Cursor) Pagination:**
`SELECT * FROM payments WHERE id > :lastSeenId ORDER BY id ASC LIMIT 20;`
Executes in $O(\log N)$ via index-range scan regardless of how deep the pagination is.

#### Q23: How do you size a HikariCP connection pool accurately?
**Model Answer:**
Use the formula proven by PostgreSQL performance engineers:
$$PoolSize = (CoreCount 	imes 2) + EffectiveSpindleCount$$
For an 8-core server with SSD storage: $(8 	imes 2) + 1 = 17$ connections.
Allocating 500 connections degrades performance because CPU context switching and disk queue contention overwhelm the hardware.

#### Q24: What is Table Partitioning and when should you partition the `payments` table?
**Model Answer:**
Partitioning splits a large logical table into smaller physical tables (e.g., by range: `payments_2026_q1`, `payments_2026_q2`).
**When to Partition:** When table size exceeds available RAM (typically > 50-100M rows) and queries frequently filter by date (`created_at`). Queries filtering by date perform "partition pruning", scanning only the relevant quarter's partition.

#### Q25: Explain PostgreSQL VACUUM and why it is critical in financial systems.
**Model Answer:**
PostgreSQL's MVCC creates a new row version on every `UPDATE` and marks the old version as dead. Dead tuples consume disk space and memory buffers. `VACUUM` reclaims dead tuple storage so it can be reused. If `autovacuum` is misconfigured or disabled, tables suffer catastrophic "bloat", slowing down queries and consuming excessive disk space.

#### Q26: What is a covering index and how does it enable Index-Only Scans?
**Model Answer:**
A covering index contains all columns requested by a `SELECT` query:
`CREATE INDEX idx_user_status ON subscriptions(user_id) INCLUDE (status, expires_at);`
PostgreSQL satisfies the query entirely from the B-tree index pages without reading the underlying heap table blocks from disk, cutting I/O operations in half.

#### Q27: How do you identify slow queries in production PostgreSQL?
**Model Answer:**
1. Enable `pg_stat_statements` extension to track total execution time, call counts, and mean latency per normalized query.
2. Configure `log_min_duration_statement = 500ms` in `postgresql.conf` to log slow queries to disk.
3. Run `EXPLAIN (ANALYZE, BUFFERS)` on the query to inspect execution plans, table scans, and memory buffer hits.

#### Q28: What is the danger of unconstrained `VARCHAR` vs fixed `VARCHAR(n)`?
**Model Answer:**
In PostgreSQL, `VARCHAR` and `TEXT` have identical underlying physical storage and performance. However, `VARCHAR(n)` enforces business domain constraints at the database boundary (e.g., `VARCHAR(50)` ensures a currency or status field is never polluted with multi-megabyte strings).

#### Q29: How do foreign keys impact write performance?
**Model Answer:**
Every time a child row is inserted (e.g., `payments`), PostgreSQL must execute an internal read lock on the parent table (`subscriptions`) to verify the foreign key exists. If the parent table lacks an index on the referenced column, or if parent updates lock entire ranges, write throughput drops.

#### Q30: Explain the difference between `NOW()` and `STATEMENT_TIMESTAMP()` in PostgreSQL.
**Model Answer:**
`NOW()` returns the timestamp at the **start of the current transaction** and remains constant across all statements within that transaction. `STATEMENT_TIMESTAMP()` updates with the start of each individual SQL statement. In financial ledgers, `NOW()` ensures all entities created within a single transaction share an identical, synchronized timestamp.

---

### Domain 4: Payment Processing & Razorpay Integrations (Questions 31–40)

#### Q31: Why should an application never trust the client-side redirect alone for payment capture?
**Model Answer:**
A malicious user can intercept the network traffic, forge the response JSON, or call the backend verification endpoint with fake payment IDs. Furthermore, legitimate users frequently close browser tabs, lose battery, or encounter WiFi drops before the redirect executes. The gateway webhook is the only authoritative server-to-server confirmation.

#### Q32: How is an HMAC-SHA256 signature calculated for Razorpay payment verification?
**Model Answer:**
The backend concatenates `order_id + "|" + payment_id`. It hashes this payload using HMAC-SHA256 initialized with the secret `keySecret`:
```java
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
byte[] hash = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
String generatedSignature = Hex.encodeHexString(hash);
return MessageDigest.isEqual(generatedSignature.getBytes(), receivedSignature.getBytes());
```

#### Q33: Why is `MessageDigest.isEqual()` required instead of standard `String.equals()`?
**Model Answer:**
Standard `String.equals()` compares characters sequentially and returns `false` on the first mismatched byte. An attacker can measure nanosecond timing variations to deduce valid signature bytes one by one (a **timing attack**). `MessageDigest.isEqual()` executes in **constant time**, comparing all bytes regardless of mismatches.

#### Q34: What happens if a webhook arrives BEFORE the client redirect verification?
**Model Answer:**
Subscribly handles this gracefully via its Finite State Machine:
1. Webhook arrives, verifies signature, marks `Payment = SUCCESS`, activates `Subscription`.
2. 500ms later, client redirect arrives and calls `verifyPayment()`.
3. `PaymentService` checks: `if (payment.getStatus() == PaymentStatus.SUCCESS) { return toResponse(payment); }`.
4. The service returns success immediately without attempting invalid state re-transitions or throwing errors.

#### Q35: What happens if a webhook arrives multiple times (duplicate delivery)?
**Model Answer:**
Razorpay guarantees **at-least-once** webhook delivery; network retries can send the identical event 5 times.
Subscribly checks:
```java
if (webhookEventRecordRepository.existsByEventId(webhookData.getEventId())) {
    log.warn("Duplicate webhook ignored: {}", webhookData.getEventId());
    return true; // Return HTTP 200 immediately
}
```
The duplicate event is acknowledged with HTTP 200 to silence gateway retries, but zero business logic executes.

#### Q36: How do you handle a currency conversion mismatch during payment capture?
**Model Answer:**
Before transitioning a payment to `SUCCESS`, the backend verifies that the gateway order's currency and amount match the internal record exactly:
`if (!payment.getAmount().equals(gatewayOrder.getAmount())) { throw new FraudException("Amount mismatch!"); }`
This prevents tampering where a user modifies client-side parameters to pay 100 JPY instead of 100 USD.

#### Q37: What is an Idempotency Key and where should it originate?
**Model Answer:**
An idempotency key is a unique string identifying an intentional business transaction. It should originate at the **client** (UUID generated before submitting the form) and accompany the request across all downstream systems. If the client retries the request, the backend detects the key and returns the cached result of the original execution.

#### Q38: How do you test payments in automated CI/CD without making real financial transactions?
**Model Answer:**
Inject a mock `PaymentGatewayProvider` (or use Razorpay Test Mode with test cards). In unit tests, Mockito verifies method interactions. In integration tests, a WireMock server stubs external Razorpay HTTP endpoints, validating payload structures and returning synthetic HMAC-signed responses.

#### Q39: What is Payment Reconciliation and why is it necessary?
**Model Answer:**
Reconciliation is an automated daily audit job that downloads the gateway's settlement CSV report and compares it row-by-row against the internal database. It flags discrepancies: payments captured on the gateway but missing internally, or gateway fees exceeding negotiated rates.

#### Q40: How do you store credit card numbers securely under PCI-DSS?
**Model Answer:**
**You don't!** Subscribly achieves **PCI-DSS SAQ-A** compliance by using Razorpay Checkout SDK. Credit card details are entered inside an iframe hosted by Razorpay and transmitted directly to their PCI-compliant servers. Subscribly only handles tokenized references (`gateway_payment_id`), never touching raw Primary Account Numbers (PANs).

---

### Domain 5: Distributed Systems & Microservices (Questions 41–50)

#### Q41: What is the "Dual-Write" problem in distributed systems?
**Model Answer:**
Occurs when an application must update two separate storage systems (e.g., write to PostgreSQL and emit to Kafka). Because distributed two-phase commits are impractical, any crash between the first and second write creates permanent data inconsistency. Mitigated via the **Transactional Outbox Pattern** or Change Data Capture (CDC).

#### Q42: How does Netflix Eureka detect failed service instances?
**Model Answer:**
Microservice instances send heartbeats to Eureka every 30 seconds. If Eureka receives no heartbeat for 90 seconds, it evicts the instance from its registry.
**Self-Preservation Mode:** If Eureka detects a sudden drop in heartbeats (> 15% missing), it assumes a network partition rather than all services crashing, and stops evicting instances to prevent cascading routing collapse.

#### Q43: What is a Circuit Breaker (Resilience4j) and what are its states?
**Model Answer:**
A Circuit Breaker prevents cascading failures when downstream services fail:
1. **CLOSED (Normal):** Requests pass through. Failure counts are tracked.
2. **OPEN (Tripped):** If error rate exceeds threshold (e.g., 50%), circuit trips. All requests fail fast immediately or return cached fallbacks without hitting the downstream service.
3. **HALF-OPEN (Testing):** After a sleep window, a trial batch of requests is allowed through. If successful, circuit returns to CLOSED; if failing, returns to OPEN.

#### Q44: What is the difference between at-least-once, at-most-once, and exactly-once message processing?
**Model Answer:**
- **At-most-once:** Commit offset before processing. Messages can be lost, never duplicated.
- **At-least-once (Kafka Standard):** Process message, then commit offset. If consumer crashes before commit, message is redelivered. Can cause duplicates; **requires idempotent consumers**.
- **Exactly-once:** Requires transactional producers and consumers (Kafka 2-phase commit). High performance overhead.

#### Q45: How do you prevent Kafka Consumer group rebalance storms?
**Model Answer:**
A consumer group rebalances when a consumer is assumed dead:
1. Increase `max.poll.interval.ms` so lengthy batch processing doesn't trigger false timeouts.
2. Tune `max.poll.records` to smaller batches so each poll completes well within the timeout.
3. Use Cooperative Sticky Assignor (`CooperativeStickyAssignor`) to avoid revoking all partitions during a rebalance.

#### Q46: What is the CAP Theorem and where does Subscribly sit?
**Model Answer:**
In the presence of a network **Partition (P)**, a distributed system must choose between **Consistency (C)** and **Availability (A)**.
- **PostgreSQL Ledger:** **CP** system. Rejects writes if quorum/consistency cannot be guaranteed. Financial records prioritize consistency over availability.
- **Eureka Discovery:** **AP** system. Returns stale registry data during network partitions rather than rejecting calls.

#### Q47: How does Spring Cloud Gateway route requests asynchronously?
**Model Answer:**
Spring Cloud Gateway is built on **Project Reactor** and **Netty** (non-blocking reactive I/O). Unlike Spring MVC which dedicates one thread per connection, Netty uses a small event loop thread pool (typically 1 thread per CPU core). Thousands of concurrent HTTP connections are handled multiplexed on a single thread, reducing memory usage and handling massive socket I/O.

#### Q48: What is Distributed Tracing (OpenTelemetry / Zipkin)?
**Model Answer:**
A monitoring pattern where every distributed request is tagged with:
- **Trace ID:** Globally unique ID shared across all microservices for a single user journey.
- **Span ID:** Unique ID representing work done within a specific service.
Visualized in Grafana/Zipkin as a timeline showing exact latency spent inside Gateway $	o$ Payment Service $	o$ PostgreSQL.

#### Q49: What is a Dead Letter Queue (DLQ)?
**Model Answer:**
When a consumer repeatedly fails to process a Kafka message (e.g., due to corrupt JSON or unexpected database errors), retrying infinitely blocks the partition. After $N$ failed retries, the message is routed to a Dead Letter Queue topic (`payment-events-dlq`) for offline inspection, allowing the main consumer queue to proceed.

#### Q50: How do you handle database credentials securely across microservices?
**Model Answer:**
Never commit credentials to `application.yml` or Docker images.
1. Inject via environment variables (`${DB_PASSWORD}`) injected by Kubernetes Secrets.
2. Integrate with HashiCorp Vault / AWS Secrets Manager to dynamically lease short-lived credentials that rotate automatically every 24 hours.

---

### Domain 6: Security, Testing & System Reliability (Questions 51–60)

#### Q51: How do you defend against Cross-Site Request Forgery (CSRF) in a REST API?
**Model Answer:**
CSRF exploits browser cookie auto-attachment. Because Subscribly uses **stateless Bearer JWTs** stored in application memory or web storage (not cookies), browsers do not automatically attach tokens to cross-site requests. Thus, CSRF attacks are fundamentally mitigated, allowing CSRF protection to be safely disabled (`csrf().disable()`).

#### Q52: What is the risk of storing JWTs in browser `localStorage` vs. `httpOnly` Cookies?
**Model Answer:**
- **`localStorage`:** Vulnerable to Cross-Site Scripting (XSS). Any malicious script injected via an npm dependency can read `localStorage.getItem('token')` and exfiltrate it.
- **`httpOnly` Cookie:** Immune to XSS exfiltration (JavaScript cannot read the cookie). However, it re-introduces CSRF vulnerability, requiring SameSite attributes (`SameSite=Strict`) and CSRF tokens.

#### Q53: What is SQL Injection and how does Hibernate/JPA prevent it?
**Model Answer:**
SQLi occurs when untrusted user input is concatenated directly into SQL queries: `"WHERE email = '" + input + "'"` (input `' OR '1'='1`).
Hibernate uses **Parameterized Prepared Statements**:
`WHERE email = :email`
The database treats the parameter strictly as literal data, never as executable SQL instructions, completely neutralizing injection attacks.

#### Q54: How do you write deterministic tests for time-sensitive subscription logic?
**Model Answer:**
Never call `Instant.now()` or `LocalDateTime.now()` directly in domain logic! Inject a `java.time.Clock` bean:
```java
@Service
public class SubscriptionService {
    private final Clock clock;
    public boolean isExpired(Subscription s) {
        return s.getExpiresAt().isBefore(Instant.now(clock));
    }
}
```
In unit tests, inject `Clock.fixed(Instant.parse("2026-10-18T00:00:00Z"), ZoneOffset.UTC)`. The test executes identically regardless of the real system clock.

#### Q55: What is the difference between `@Mock` and `@Spy` in Mockito?
**Model Answer:**
- **`@Mock`:** Creates a dummy surrogate object. All methods return default values (`null`, `0`, `false`) unless explicitly stubbed with `when(...).thenReturn(...)`.
- **`@Spy`:** Wraps a real object instance. Real methods execute by default unless explicitly stubbed. Used in Subscribly for `PaymentStateMachine`.

#### Q56: How do you verify that an event was published to Kafka in a unit test?
**Model Answer:**
Use Mockito `ArgumentCaptor`:
```java
ArgumentCaptor<PaymentCompletedEvent> captor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
verify(kafkaTemplate).send(eq("payment-events"), anyString(), captor.capture());
assertEquals(100L, captor.getValue().getSubscriptionId());
```

#### Q57: How do you test database migrations with Flyway in CI/CD?
**Model Answer:**
Run an automated test container using Testcontainers:
1. Spin up a disposable PostgreSQL Docker container.
2. Let Flyway execute all migration scripts from `V1` to `V4`.
3. Assert that schema matches JPA entity definitions using Hibernate schema validation:
   `spring.jpa.hibernate.ddl-auto=validate`.

#### Q58: What is Graceful Shutdown and why is it critical in Kubernetes?
**Model Answer:**
When Kubernetes terminates a pod, it sends a `SIGTERM` signal. Without graceful shutdown, in-flight payment HTTP requests are severed immediately.
Enabling `server.shutdown=graceful` in Spring Boot instructs the server to stop accepting new requests while giving in-flight requests a grace period (e.g., 30s) to finish database commits cleanly before JVM termination.

#### Q59: How do you mitigate rate-limiting abuse without blocking legitimate users?
**Model Answer:**
Implement a **Token Bucket algorithm** at the API Gateway using Redis:
- Bucket capacity: 100 tokens. Refill rate: 10 tokens/second.
- Rate limit by authenticated `userId`, falling back to IP address for anonymous endpoints.
- Return HTTP 429 Too Many Requests with a `Retry-After: 5` header.

#### Q60: An interviewer asks: *"How would Subscribly handle 100x traffic (10,000 requests/second)?"*
**Model Answer:**
1. **Edge Caching & CDN:** Cloudflare/CloudFront caches static frontend assets and pricing plan metadata.
2. **API Gateway Horizontal Scaling:** Deploy 10 gateway instances behind an AWS Application Load Balancer.
3. **Database Read/Write Splitting:** Route read traffic (profile checks, entitlement checks) to 3 PostgreSQL Read Replicas via PgBouncer connection poolers.
4. **Asynchronous Webhook Buffering:** Ingest webhooks into an AWS SQS / Kafka queue immediately upon receipt (HTTP 200 in 10ms); process database state updates asynchronously via consumer workers.
5. **Table Partitioning:** Partition `payments` and `webhook_event_records` by month.


---

# Chapter 16 — Comprehensive System Design Blueprints (6 Scenarios)

### Scenario 1: High-Scale Subscription Billing Engine (10M Active Users)

#### 1. Requirements & Quantitative Scale Estimation
- **Active Subscriptions:** 10,000,000.
- **Billing Frequency:** Monthly.
- **Daily Billing Volume:** $10,000,000 / 30 \approx 333,333$ renewals/day.
- **Execution Window:** 02:00 AM – 05:00 AM UTC (3 hours = 10,800 seconds).
- **Required Throughput:** $\approx 31$ recurring charges/second sustained (burst capacity: 100 QPS).
- **Storage Calculation:**
  - `subscriptions` row size $\approx 250$ bytes. Total table size: $2.5\text{ GB}$.
  - `payments` row size $\approx 350$ bytes. Annual growth: $120\text{M payments/decade} \approx 42\text{ GB}$.

#### 2. Architecture Diagram
```mermaid
flowchart TD
    Airflow[Distributed Orchestrator / Cron Leader] -->|02:00 UTC Trigger| Coordinator[Batch Job Coordinator]
    Coordinator -->|Query ID Ranges| MasterDB[(PostgreSQL Primary)]
    Coordinator -->|Produce Partitioned Messages| KafkaTopic[(Kafka Topic: 'billing-renewals')]
    
    subgraph Worker Pool (10 Scaled Pods)
        W1[Worker Pod 1]
        W2[Worker Pod 2]
        W3[Worker Pod 3]
    end
    
    KafkaTopic -->|Partition 0-3| W1
    KafkaTopic -->|Partition 4-7| W2
    KafkaTopic -->|Partition 8-11| W3
    
    W1 -->|Token Bucket Rate Limiter: 10 QPS| Gateway[Razorpay Auto-Debit API]
    W1 -->|Optimistic Lock Commit| MasterDB
    W1 -->|Publish Event| ResultTopic[(Kafka Topic: 'payment-events')]
```

#### 3. Database Schema & Keyset Partitioned Query
```sql
-- Coordinator Keyset Cursor Query (Zero Table Scan, Uses Composite B-Tree Index)
SELECT id, user_id, plan, price, payment_method_token 
FROM subscriptions 
WHERE status = 'ACTIVE' 
  AND auto_renew = TRUE 
  AND expires_at <= :executionDayEnd 
  AND id > :lastBatchId 
ORDER BY id ASC 
LIMIT 1000;
```

#### 4. Failure Recovery & Gateway Backpressure
- **Rate Limit Adherence:** Razorpay enforces a maximum 50 QPS merchant rate limit. Workers use a distributed Redis Token Bucket (`resilience4j.ratelimiter`) to ensure the cluster never exceeds 40 QPS.
- **Gateway Outage / 504 Gateway Timeout:** Failed charges enter a retry queue with exponential backoff: Retry 1 (+4 hours), Retry 2 (+24 hours), Retry 3 (+72 hours). After 3 failed attempts, subscription enters `GRACE_PERIOD` before transitioning to `SUSPENDED`.

---

### Scenario 2: Resilient Payment Webhook Ingestion Pipeline (10,000 Webhooks/Sec)

#### 1. Requirements & Ingestion SLA
- **Peak Flash Sale Throughput:** 10,000 incoming webhook HTTP requests/second.
- **Gateway Timeout Threshold:** Gateway drops connections after 5,000ms.
- **Ingestion SLA:** Return HTTP 200 OK within $< 50\text{ ms}$.
- **Core Invariant:** Zero dropped webhooks; 100% idempotent deduplication.

#### 2. Architecture & Ingestion Flow
```mermaid
flowchart LR
    Razorpay[Razorpay Webhook Dispatcher] -->|POST /webhook| ALB[AWS Application Load Balancer]
    ALB --> Pod1[Ingestion Pod 1]
    ALB --> Pod2[Ingestion Pod 2]
    ALB --> Pod3[Ingestion Pod 3]
    
    subgraph Ultra-Fast Memory Buffer Layer
        Pod1 -->|1. HMAC Check (2ms)| Verify{Valid?}
        Verify -->|Yes| KafkaProd[Produce to Kafka: 'raw-webhooks']
        KafkaProd --> FastAck[2. Return HTTP 200 OK (15ms)]
    end
    
    KafkaProd -.-> KafkaCluster[(Kafka Cluster - 16 Partitions)]
    KafkaCluster -.-> ConsumerGroup[Payment Processor Workers]
    ConsumerGroup --> DedupCheck[(PostgreSQL: webhook_events)]
    ConsumerGroup --> StateUpdate[(PostgreSQL: payments / subscriptions)]
```

#### 3. High-Throughput Ingestion Controller
```java
@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
@Slf4j
public class HighThroughputWebhookController {

    private final WebhookSignatureVerifier signatureVerifier;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "subscribly.raw-webhooks";

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> ingestWebhook(
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestBody String rawPayload) {

        // 1. Synchronous Cryptographic Verification (In-Memory, 1.8ms)
        if (!signatureVerifier.isValid(rawPayload, signature)) {
            log.warn("Unauthorized webhook attempt rejected");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Extract Event ID without full JSON deserialization
        String eventId = FastJsonParser.extractField(rawPayload, "id");

        // 3. Buffer immediately into partitioned Kafka topic
        kafkaTemplate.send(TOPIC, eventId, rawPayload);

        // 4. Return HTTP 200 in < 20ms to prevent gateway retry storms
        return ResponseEntity.ok().build();
    }
}
```

---

### Scenario 3: Reliable Distributed Renewal Scheduler (Multi-Pod Cluster)

#### 1. The Multi-Pod Double-Charge Problem
When running 5 instances of `subscription-service` in Kubernetes, Spring's `@Scheduled` annotation triggers on all 5 pods simultaneously at 00:00:00 UTC. Without coordination, all 5 pods charge the customer's credit card 5 times.

#### 2. ShedLock Distributed Coordination Blueprint
Subscribly utilizes database-backed distributed locking via **ShedLock**:

```mermaid
sequenceDiagram
    autonumber
    participant Pod1 as Subscription Pod 1
    participant Pod2 as Subscription Pod 2
    participant DB as PostgreSQL (shedlock table)

    Note over Pod1,Pod2: 00:00:00 UTC Cron Trigger Fires
    Pod1->>DB: UPDATE shedlock SET locked_until = NOW() + INTERVAL '30m' WHERE name = 'renewal' AND locked_until <= NOW()
    DB-->>Pod1: 1 row updated (Lock Acquired!)
    
    Pod2->>DB: UPDATE shedlock SET locked_until = NOW() + INTERVAL '30m' WHERE name = 'renewal' AND locked_until <= NOW()
    DB-->>Pod2: 0 rows updated (Lock Refused!)

    Pod1->>Pod1: Executes Renewal Batch Queries
    Pod2->>Pod2: Skips Execution and Logs "Lock currently held by Pod 1"
    
    Pod1->>DB: UPDATE shedlock SET locked_until = NOW() WHERE name = 'renewal' (Lock Released)
```

#### 3. Database Schema for ShedLock
```sql
CREATE TABLE shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
```

#### 4. Handling Node Crashes & Clock Skew
- `lockAtMostFor = "30m"`: If Pod 1 suffers an unrecoverable kernel panic or JVM OOM during the batch, the lock automatically expires after 30 minutes, allowing the next scheduled run or standby node to recover without human intervention.
- `lockAtLeastFor = "5m"`: Prevents rapid re-execution if clock skew between servers causes a node to fire twice within seconds.

---

### Scenario 4: Multi-Tenant SaaS Billing Platform

#### 1. Tenant Isolation Models
- **Database-per-Tenant:** Maximum security, highest operational cost (1,000 tenants = 1,000 DB instances).
- **Schema-per-Tenant:** Good isolation, connection pool exhaustion at scale.
- **Shared Database, Shared Schema with PostgreSQL Row-Level Security (RLS) — Subscribly Choice:** Optimal resource efficiency, strict hardware utilization.

#### 2. PostgreSQL Row-Level Security (RLS) Implementation
```sql
-- 1. Enable RLS on core financial tables
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;

-- 2. Define Tenant Isolation Policy
CREATE POLICY tenant_isolation_policy ON subscriptions
    FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', true)::uuid);
```

#### 3. Dynamic Tenant Context Propagation in Spring Boot
```java
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String tenantId = request.getHeader("X-Tenant-ID");
        if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.setCurrentTenant(tenantId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear(); // Mandatory to prevent ThreadLocal memory leak in pool
        }
    }
}
```

Before each database query, the datasource connection sets the session setting:
```sql
SET LOCAL app.current_tenant_id = 'e7b1a23c-...';
```
Any accidental `SELECT * FROM subscriptions` executed without a `WHERE` clause will only return rows belonging to the authenticated tenant.

---

### Scenario 5: High-Volume Event-Driven Notification Engine (Bulkhead Architecture)

#### 1. The Slow Provider Thread Pool Exhaustion Problem
If external email provider SendGrid experiences a 10-second network latency spike, all worker threads block waiting on HTTP responses. Consequently, SMS verification codes and push alerts are blocked, crippling system login.

#### 2. Bulkhead Architecture Blueprint
```mermaid
flowchart TD
    Kafka[Kafka Consumer: 'notification-events'] --> Router[Event Dispatcher Router]
    
    subgraph Isolated Thread Pool Bulkheads
        Router -->|Email Events| PoolEmail[Email Thread Pool: 20 Threads]
        Router -->|SMS Events| PoolSMS[SMS Thread Pool: 15 Threads]
        Router -->|Push Events| PoolPush[Push Notification Pool: 25 Threads]
    end
    
    PoolEmail --> SendGrid[SendGrid API]
    PoolSMS --> Twilio[Twilio SMS API]
    PoolPush --> Firebase[Firebase Cloud Messaging]
```

#### 3. Resilience4j Bulkhead & Circuit Breaker Configuration
```yaml
resilience4j:
  bulkhead:
    instances:
      emailService:
        maxConcurrentCalls: 20
        maxWaitDuration: 100ms
      smsService:
        maxConcurrentCalls: 15
        maxWaitDuration: 50ms
  circuitbreaker:
    instances:
      emailService:
        slidingWindowSize: 50
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
```

If SendGrid goes down, the `emailService` circuit breaker trips to `OPEN`, immediately diverting failed email notifications to a Dead-Letter Queue (DLQ) without consuming thread pool resources. SMS and push notifications continue operating at full speed.

---

### Scenario 6: Zero-Downtime Monolith to Microservices Migration (Strangler Fig)

#### 1. The 5-Phase Migration Execution Roadmap

```mermaid
flowchart LR
    subgraph Phase 1: Baseline
        Client1[Client] --> Monolith1[Monolith All-in-One]
    end
    
    subgraph Phase 2: Ingress Gateway
        Client2[Client] --> GW2[API Gateway]
        GW2 -->|All Traffic 100%| Monolith2[Monolith]
    end
    
    subgraph Phase 3: Extract Identity
        Client3[Client] --> GW3[API Gateway]
        GW3 -->|/api/auth/**| UserSvc3[User Service]
        GW3 -->|Remaining /api/**| Monolith3[Monolith]
    end
    
    subgraph Phase 4: Extract Payments
        Client4[Client] --> GW4[API Gateway]
        GW4 -->|/api/auth/**| UserSvc4[User Service]
        GW4 -->|/api/payments/**| PaySvc4[Payment Service]
        GW4 -->|/api/subscriptions/**| Monolith4[Monolith]
    end
```

#### 2. Expand-Contract Database Migration Strategy
When moving the `payments` table from the monolith's database to `payment-service`'s private schema:
1. **Expand (Dual-Write):** Monolith writes payment rows to both legacy `payments` table and publishes a CDC event to Kafka. A consumer writes to the new microservice PostgreSQL instance.
2. **Backfill:** Run an offline batch job backfilling historical rows from legacy DB to the new DB with keyset cursors.
3. **Verify:** Run a continuous reconciliation script comparing checksums of both databases in real time until drift is zero.
4. **Contract (Switch Read Authority):** Update API Gateway routing to direct all payment requests to `payment-service`.
5. **Drop Legacy:** Drop the legacy table in the monolith after 30 days of clean operation.

---

# Chapter 17 — Revision Toolkit, Navigation Guide, and Glossary

### 17.1 Source Code Navigation Guide

Every architectural concept documented in this handbook maps directly to real source code in the Subscribly repository:

| Architectural Component | Core Responsibility | Exact File Path in Repository |
| :--- | :--- | :--- |
| **Payment Service Core** | Checkout order creation, gateway dispatch, verification | `payment-service/src/main/java/com/subscribly/paymentservice/service/PaymentService.java` |
| **Payment State Machine** | Strict state transition validation & invariants | `payment-service/src/main/java/com/subscribly/paymentservice/service/PaymentStateMachine.java` |
| **Razorpay Provider** | HMAC-SHA256 verification & client SDK options | `payment-service/src/main/java/com/subscribly/paymentservice/gateway/RazorpayGatewayProvider.java` |
| **Payment Audit Service** | `REQUIRES_NEW` audit logging surviving rollbacks | `payment-service/src/main/java/com/subscribly/paymentservice/service/PaymentAuditService.java` |
| **Subscription Service** | Lifecycle state transitions, expiry math, renewal | `subscription-service/src/main/java/com/subscribly/subscriptionservice/service/SubscriptionService.java` |
| **Monolith Scheduler** | Cron-based batch expiration and auto-renewal | `monolith/src/main/java/com/subscribly/scheduler/SubscriptionScheduler.java` |
| **Database Migrations** | Versioned Flyway SQL schema and constraint scripts | `monolith/src/main/resources/db/migration/` |
| **JWT & Common DTOs** | Shared user contracts, token validation, API envelopes | `common-lib/src/main/java/com/subscribly/common/dto/` |
| **API Gateway Routing** | Route predicates, security filter, correlation ID filter | `api-gateway/src/main/resources/application.yml` |
| **Frontend Root & Client** | React 18 single-page app, Axios interceptors, UI | `frontend/src/App.tsx` and `frontend/src/api/client.ts` |

---

### 17.2 The Day-Before Interview Revision Cheat Sheet

```
+===================================================================================================+
|                                SUBSCRIBLY ARCHITECTURE CHEAT SHEET                                |
+===================================================================================================+
| 1. CORE NUMBERS & METRICS:                                                                        |
|    - 205 automated tests across 24 test suites (100% passing, 0 failures, 0 errors, 31.91s runtime).  |
|    - 10 Maven reactor modules. Java 21 LTS + Spring Boot 3.4.3 + Spring Cloud 2024.0.0.          |
|    - BCrypt Cost Factor 12 = 4,096 Blowfish rounds (~300ms calculation time per hash).             |
|                                                                                                   |
| 2. FINANCIAL INVARIANTS:                                                                          |
|    - Idempotency: Enforced by PostgreSQL UNIQUE(idempotency_key) & UNIQUE(payment_reference).     |
|    - Webhook Verification: HMAC-SHA256 calculated on raw byte stream before JSON deserialization.  |
|    - Concurrency Control: Hibernate @Version optimistic locking avoids distributed lock overhead. |
|    - Audit Trails: Propagation.REQUIRES_NEW ensures audit persistence even if business logic rolls|
|    - Temporal Accuracy: Java Instant (UTC) eliminates Daylight Saving Time and timezone drift.    |
|    - Decomposition: Modular Monolith first, decomposed via Strangler Fig into Spring Cloud.      |
+===================================================================================================+
```

---

### 17.3 20 High-Yield Flashcard Concepts

1. **Idempotency Key:** Unique client-generated token ensuring duplicate HTTP requests produce the identical result without duplicate mutations.
2. **Optimistic Locking:** Concurrency mechanism verifying `@Version` on SQL update; throws `OptimisticLockException` if updated by another transaction.
3. **Pessimistic Locking:** Row-level `SELECT FOR UPDATE` locking preventing concurrent access for the duration of the transaction.
4. **REQUIRES_NEW Propagation:** Suspends existing transaction to execute and commit an independent physical transaction.
5. **HMAC-SHA256:** Cryptographic keyed hash verifying data integrity and authenticity using a shared secret.
6. **Timing Attack:** Side-channel attack deducing secret bytes via comparison latency; neutralized via `MessageDigest.isEqual()`.
7. **Keyset Pagination:** $O(\log N)$ cursor pagination filtering `WHERE id < :cursor ORDER BY id DESC LIMIT 20`, avoiding `OFFSET` table scans.
8. **Cache-Aside:** Query cache first; load from database on cache miss and write to cache with TTL.
9. **Cache Stampede:** Thousands of threads querying database simultaneously on cache key expiration; mitigated via distributed mutex locks.
10. **Eureka Heartbeat:** 30-second ping from microservices to service registry; 90-second timeout triggers eviction.
11. **Circuit Breaker:** Resilience pattern with CLOSED, OPEN, and HALF-OPEN states preventing cascading failure to downstream services.
12. **Outbox Pattern:** Atomically writing domain state and outgoing events to a local database table to guarantee at-least-once message publishing.
13. **Strangler Fig Pattern:** Incrementally decomposing a monolith by intercepting traffic at an API gateway and extracting services domain-by-domain.
14. **Virtual Threads (Project Loom):** Lightweight JVM-managed threads decoupling I/O blocking from OS carrier threads.
15. **N+1 Query Problem:** Executing 1 query for parents and $N$ queries for children; resolved via JPQL `JOIN FETCH`.
16. **BCrypt Salt:** 128-bit cryptographically secure random value prepended to password to defeat precomputed rainbow tables.
17. **Stateless JWT:** Self-contained token avoiding database session lookups, enabling trivial horizontal scaling.
18. **Expand-Contract:** Database migration methodology adding non-breaking columns, dual-writing, backfilling, and dropping legacy columns later.
19. **Flyway Migration:** Deterministic versioned SQL tool tracking schema evolution via SHA-256 checksums in `flyway_schema_history`.
20. **ShedLock:** Distributed scheduler lock utilizing a shared database table to prevent concurrent execution across multi-pod clusters.

---

### 17.4 4-Week SDE-2/SDE-3 Mastery Study Plan

- **Week 1: Java & Spring Boot Internals**
  - Master Spring AOP proxies, dynamic byte-buddy generation, and `@Transactional` self-invocation pitfalls.
  - Implement custom Spring Security filters and examine the exact filter order.
  - Benchmark Java 21 Virtual Threads vs. Platform Thread pools under simulated socket latency.
- **Week 2: PostgreSQL Data Modeling & Concurrency**
  - Deep-dive into PostgreSQL B-Tree indexing, index selectivity, composite index ordering, and `EXPLAIN ANALYZE`.
  - Simulate race conditions: double-submit, simultaneous webhook arrival, scheduler vs. cancellation.
  - Test optimistic locking retries with jittered backoff.
- **Week 3: Financial Architecture & Distributed Systems**
  - Trace Razorpay order creation, client modal integration, and raw-body HMAC-SHA256 verification.
  - Implement the Transactional Outbox pattern with PostgreSQL and Apache Kafka.
  - Study circuit breakers, rate limiters, and bulkhead thread pool isolation using Resilience4j.
- **Week 4: System Design & Interview Defense**
  - Rehearse the 2-minute, 5-minute, and 15-minute Subscribly architectural pitch.
  - Master all 60 challenge questions in Chapter 15.
  - Walk through the 6 system design blueprints in Chapter 16 on a whiteboard or digital canvas.

---

### 17.5 Architectural Glossary (40 Key Terms)

1. **ACID:** Atomicity, Consistency, Isolation, Durability — the fundamental guarantees of relational database transactions.
2. **AOP (Aspect-Oriented Programming):** Modularizing cross-cutting concerns (transactions, security, logging) via dynamic proxies.
3. **API Gateway:** Centralized reverse proxy handling authentication, rate limiting, and request routing.
4. **BCrypt:** Adaptive cryptographic key derivation function designed specifically for secure password hashing.
5. **Bulkhead Pattern:** Partitioning system resources (thread pools, memory) to prevent one failing component from taking down the whole system.
6. **Cache Invalidation:** The process of purging or replacing stale cached entries when underlying data changes.
7. **CDC (Change Data Capture):** Streaming low-level database transaction log changes directly into message brokers.
8. **Circuit Breaker:** Design pattern that trips to fail-fast when a remote service fails, preventing thread pool exhaustion.
9. **Clock Skew:** Time discrepancy between physical hardware server clocks in a distributed cluster.
10. **Composite Index:** Database index spanning multiple columns where column order determines query indexability.
11. **Correlation ID:** Unique UUID attached to incoming HTTP requests and propagated through all distributed network hops.
12. **Dead-Letter Queue (DLQ):** Secondary messaging queue holding messages that failed processing after maximum retry attempts.
13. **Eureka:** Service discovery registry enabling microservices to find each other dynamically without static IP configuration.
14. **Eventual Consistency:** Consistency model where data replicas become consistent over time given no further updates.
15. **Expand-Contract:** Schema migration strategy enabling zero-downtime database updates across rolling application deployments.
16. **Flyway:** Open-source database migration tool tracking schema changes via versioned SQL scripts.
17. **Graceful Shutdown:** Allowing active in-flight requests to complete before terminating an application process.
18. **HikariCP:** High-performance, zero-overhead JDBC connection pool library standard in Spring Boot.
19. **HMAC-SHA256:** Hash-based Message Authentication Code combining a secret key with the SHA-256 cryptographic hash.
20. **Idempotency:** The property of an operation whereby multiple identical requests have the identical effect as a single request.
21. **JWT (JSON Web Token):** Compact, URL-safe standard (RFC 7519) representing claims verified cryptographically.
22. **Keyset Pagination:** Cursor-based pagination filtering on indexed columns (`WHERE id < :cursor`) avoiding offset scans.
23. **Loom (Project Loom):** Java enhancement introducing lightweight virtual threads to simplify high-throughput concurrent programming.
24. **MDC (Mapped Diagnostic Context):** Logging mechanism binding contextual key-value pairs to the executing thread.
25. **MVCC (Multi-Version Concurrency Control):** Relational database design where updates create new row versions to enable non-blocking reads.
26. **N+1 Query:** Performance anti-pattern executing 1 query for parents followed by $N$ separate queries for child rows.
27. **Optimistic Locking:** Concurrency mechanism verifying row version at commit time without holding database locks.
28. **Outbox Pattern:** Pattern ensuring reliable message delivery by saving events in the local database transaction before publishing.
29. **Pessimistic Locking:** Database mechanism acquiring exclusive locks on rows (`SELECT FOR UPDATE`) before modification.
30. **Rate Limiting:** Controlling the rate of traffic sent or received by an API to prevent abuse and resource starvation.
31. **RFC 7807:** Standardized specification defining a machine-readable format for HTTP API error responses.
32. **Row-Level Security (RLS):** Database security feature restricting row visibility based on the executing user's session context.
33. **Saga Pattern:** Sequence of local transactions coordinating distributed workflows across microservices with compensating actions.
34. **ShedLock:** Distributed locking library ensuring scheduled tasks execute only once across multi-instance clusters.
35. **Strangler Fig Pattern:** Architecture modernization strategy incrementally replacing legacy systems with microservices.
36. **Synthetic Key:** System-generated unique identifier (e.g., auto-increment Long, UUID) having no real-world business meaning.
37. **Timing Attack:** Side-channel attack measuring the time taken to execute cryptographic operations to infer secret data.
38. **Token Bucket:** Algorithm used for network traffic shaping and rate limiting allowing bursts up to a defined capacity.
39. **Virtual Thread:** Lightweight user-mode thread managed directly by the JVM rather than the underlying host OS.
40. **WAL (Write-Ahead Logging):** Database architecture where data changes are written to append-only disk logs before modifying table pages.

---

### 17.6 "Why This and Not That?" Defense Checklist

- **Why PostgreSQL instead of MongoDB?** Subscribly manages financial ledgers. Relational multi-table ACID transactions, storage-level unique constraints, and foreign keys guarantee that double-billing and orphaned records cannot occur.
- **Why Optimistic Locking instead of Redis Distributed Locks?** Redis locks introduce distributed systems failure modes (clock drift, GC pause lock expiration, network partitions). Optimistic locking is handled natively by PostgreSQL with zero additional infrastructure dependencies.
- **Why Dual-Channel Verification?** Relying solely on client redirects fails when browsers crash or users lose internet connectivity. Webhooks provide authoritative asynchronous confirmation directly from the gateway.
- **Why Raw-Payload Webhook Verification?** Parsing JSON into objects prior to verification alters whitespace and key ordering, breaking the cryptographic HMAC-SHA256 hash calculation.
- **Why Modular Monolith first before Microservices?** Domain boundaries must stabilize before code is partitioned into distributed services. Monoliths allow instant refactoring, compile-time safety, sub-millisecond in-memory calls, and fast local test cycles.
- **Why BCrypt over SHA-256 for Passwords?** SHA-256 is designed to be as fast as possible (gigahashes/second on GPUs), making it trivial to brute-force. BCrypt includes a configurable work factor making hashing deliberately slow and GPU-resistant.
- **Why Java 21 LTS over Java 17?** Java 21 introduces Virtual Threads (Project Loom), Sequenced Collections, Record Patterns, and enhanced pattern matching, providing massive concurrency benefits for I/O-bound microservices.

---

### 17.7 Known Limitations & Top 3 High-Value Roadmap Items

1. **Live Gateway Webhook Simulator:** Currently verified via comprehensive Mockito and unit tests. Next step: deploy a local mock server container simulating Razorpay signature handshakes under simulated network packet drops.
2. **Distributed Tracing Integration:** Add OpenTelemetry agent with Jaeger/Zipkin visualization across the API Gateway and OpenFeign calls.
3. **Automated Reconciliation Cron:** Implement a nightly reconciliation job parsing simulated bank clearing settlement files against PostgreSQL payment records to detect and alert on financial drift.
