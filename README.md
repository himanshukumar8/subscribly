# Subscribly

Subscription management platform built with Java, Spring Boot, PostgreSQL, and React. The project includes subscription lifecycle and payment workflows, authentication, and a multi-module backend architecture.

## Overview

Subscribly provides APIs and a web frontend for managing users, subscriptions, and payment-related workflows. The repository contains a core backend application as well as separate service modules for exploring a microservices architecture.

## Technology Stack

- **Backend:** Java 21, Spring Boot, Spring Security
- **Database:** PostgreSQL, Flyway
- **Authentication:** JWT, role-based access control
- **Payments:** Razorpay integration layer and webhook handling
- **Frontend:** React, TypeScript, Vite
- **Build and testing:** Maven, JUnit, Mockito
- **Infrastructure:** Spring Cloud Gateway, Eureka Discovery, Config Server, Docker Compose configuration

## Repository Structure

| Module | Responsibility |
| --- | --- |
| `monolith/` | Core subscription-management application |
| `user-service/` | User-related service |
| `subscription-service/` | Subscription-related service |
| `payment-service/` | Payment workflows and gateway integration |
| `notification-service/` | Notification service |
| `common-lib/` | Shared code and DTOs |
| `api-gateway/` | API gateway |
| `discovery-server/` | Service discovery |
| `config-server/` | Centralized configuration server |
| `config-repo/` | Externalized service configuration |
| `frontend/` | React and TypeScript web application |

The repository contains both the core application and a multi-module service architecture. Running all modules together requires the relevant services, configuration, database, and infrastructure to be available.

## Main Functionality

- JWT-based authentication and role-based authorization
- User and subscription ownership checks
- Subscription creation, validation, expiry, cancellation, and renewal workflows
- Payment order and verification workflows
- Payment webhook signature validation and duplicate-event handling
- Payment idempotency and retry-related handling
- Database schema migrations
- React frontend for interacting with the application

The exact behavior available in a running environment depends on the selected application configuration and the services started.

## Getting Started

### Prerequisites

- JDK 21
- Node.js and npm for the frontend
- PostgreSQL
- Git

Docker is optional for local development, but the Docker Compose setup has not been verified by running containers in the development environment.

### Backend

From the repository root, configure Java 21 and the required database and application settings. Do not commit real credentials.

On Windows PowerShell:

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

java -version
.\mvnw.cmd test
```

To package the Maven reactor:

```powershell
.\mvnw.cmd package
```

Configure the required database connection, JWT signing secret, and any external payment settings using the application's supported configuration properties before starting the relevant application or services. Use strong, unique secrets outside development and keep them out of source control.

### Frontend

```powershell
cd frontend
npm install
npm run build
```

Use the repository's lockfile and existing scripts when installing dependencies. A successful frontend build does not by itself mean the frontend is connected to a running backend.

## Testing

The latest local verification report recorded **205 passing Maven tests** and a successful frontend production build. These results cover the automated checks executed in that environment; they do not establish that every distributed integration or external service has been tested.

## Current Limitations

- A live Razorpay sandbox transaction and external webhook round trip have not been demonstrated.
- Docker Compose configuration has not been validated by running the containers in the reported environment.
- The microservice modules are present in the repository, but a complete distributed deployment has not been demonstrated.

## Project Status

The current codebase and automated test suite have been developed and locally verified to the extent described above. 
