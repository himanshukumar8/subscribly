# Subscribly

Subscription management platform built with Java, Spring Boot, PostgreSQL, and React. The project includes subscription lifecycle and payment workflows, authentication, and a multi-module backend architecture.

## Overview

Subscribly provides APIs and a web frontend for managing users, subscriptions, and payment-related workflows. The repository contains a hardened modular monolith backend application as well as separate service modules for exploring a microservices architecture.

## Technology Stack

- **Backend:** Java 21 (Azul Zulu 21.0.7), Spring Boot 3.2.x, Spring Security 6.x
- **Database:** PostgreSQL, Flyway (V1 through V5 migrations)
- **Monetary Precision:** Exact `BigDecimal` and `NUMERIC(12,2)` precision
- **Authentication:** JWT, role-based access control (RBAC), BCrypt password hashing
- **Payments:** Razorpay integration layer, HMAC-SHA256 webhook signature verification, idempotency tracking
- **Frontend:** React 18, TypeScript, Vite, Tailwind CSS
- **Build and testing:** Maven Multi-Module Reactor, JUnit 5, Mockito, MockMvc
- **Infrastructure:** Spring Cloud Gateway, Eureka Discovery, Config Server, Docker Compose configuration

## Repository Structure

| Module | Responsibility |
| --- | --- |
| `monolith/` | Hardened subscription-management application (188 tests) |
| `user-service/` | User-related microservice (7 tests) |
| `subscription-service/` | Subscription-related microservice (9 tests) |
| `payment-service/` | Payment workflows and gateway integration microservice (23 tests) |
| `notification-service/` | Notification event consumer and dispatch service |
| `analytics-service/` | Event stream metrics service |
| `audit-service/` | Audit logging service |
| `common-lib/` | Shared code and common DTOs |
| `api-gateway/` | Spring Cloud API gateway |
| `discovery-server/` | Netflix Eureka service discovery |
| `config-server/` | Centralized Spring Cloud configuration server |
| `config-repo/` | Externalized service configuration |
| `frontend/` | React 18 and TypeScript SPA (Vite) |

The repository contains both the core monolithic application and a multi-module microservice architecture. Running all modules together in a distributed topology requires the relevant services, configuration, database, and message brokers (Kafka) to be available.

## Main Functionality & Hardened Behavior

- **Security & Authorization:** JWT-based authentication, RBAC, ownership checks on subscriptions and payments.
- **Admin Provisioning:** Disabled by default (`subscribly.security.admin-bootstrap.enabled: false`). Rejects weak passwords (<12 chars or dictionary tokens) and checks historical records to prevent resurrecting deleted administrators.
- **Monetary Precision:** All subscription prices and payment amounts persist as `NUMERIC(12,2)` via Flyway V5 with zero floating-point imprecision.
- **Payment Lifecycle & Webhooks:** Constant-time HMAC-SHA256 signature verification on raw request bodies, duplicate webhook idempotency, and bounded renewal retries.
- **Fail-Closed Payment Simulation:** Simulation is strictly opt-in (`simulation-enabled: false` by default). Live mode fails closed and never records simulated charges or false entitlements.
- **DTO Isolation:** Controllers return encapsulated DTOs (`PaymentResponse`, etc.) to prevent accidental exposure of password hashes or internal Hibernate proxies.

## Getting Started

### Prerequisites

- JDK 21 (tested on Azul Zulu JDK 21.0.7)
- Node.js (v18+) and npm for the frontend
- PostgreSQL 15+
- Git

### Backend

From the repository root, configure Java 21 and the required database and application settings. Do not commit real credentials.

On Windows PowerShell:

```powershell
$env:JAVA_HOME = "C:\Users\HIMANSHU\.jdks\azul-21.0.7"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

java -version
.\mvnw.cmd clean test
```

To package the Maven reactor:

```powershell
.\mvnw.cmd package -DskipTests
```

### Frontend

```powershell
cd frontend
npm install
npm run build
```

The production build generates optimized static assets in `frontend/dist/`.

## Testing & Quality Gates

The automated verification suite records **227 passing Maven tests** across all 10 modules with 0 failures, 0 errors, and 0 skipped tests:

| Module | Tests Run | Result |
| --- | :---: | :---: |
| `monolith` | 188 | Pass |
| `payment-service` | 23 | Pass |
| `subscription-service` | 9 | Pass |
| `user-service` | 7 | Pass |
| Other modules | Compile/Lib | Pass |
| **Total Reactor** | **227** | **100% Pass** |

The frontend build also passes cleanly with zero TypeScript or Vite bundle errors.

## Current Limitations & Staging Scope

- **Off-Session Recurring Mandates:** Scheduled auto-renewals fail closed in production until Razorpay customer mandate registration and pre-debit notifications are configured with live gateway credentials.
- **Distributed Microservices Staging:** The microservices modules compile, link, and pass isolated tests, but full multi-container orchestration in Docker Compose (Kafka, Eureka, Gateway) has not been run in a live staging environment.
- **Database Safety:** Migration V5 has been verified with representative legacy data on isolated test schemas. Real production migrations should always be run against backed-up data.
