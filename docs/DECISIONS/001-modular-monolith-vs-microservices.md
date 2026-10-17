# ADR 001: Architecture Selection — Modular Monolith over Distributed Microservices

**Date:** 2026-10-17  
**Status:** Accepted  
**Deciders:** Principal Software Architect, Lead Backend Engineer  
**Context:** Subscribly Portfolio & Enterprise Demonstration Backend  

---

## 1. Context and Problem Statement

The repository was found in an uncommitted state on branch `microservices-v2`, where the original monolith code had been decomposed into 8 separate microservice directories (`discovery-server`, `api-gateway`, `config-server`, `common-lib`, `user-service`, `subscription-service`, `payment-service`, `notification-service`). 

Running this distributed system requires:
- 8 separate Spring Boot JVM processes.
- An Apache Kafka cluster + Apache Zookeeper.
- Redis.
- PostgreSQL.

Total local RAM consumption exceeds 3.5 GB to 5.0 GB. Furthermore, hosting 8 JVMs and Kafka on public cloud infrastructure exceeds all free-tier limits (Render, Railway, Fly.io, Neon). 

At the same time, the existing microservice code lacks automated tests, version-controlled database migrations, distributed transaction coordination (Sagas), and network-level security isolation.

We must decide the architectural deployment topology for the production-grade Subscribly backend.

---

## 2. Decision Drivers

1. **Demonstrable Engineering Rigor:** SDE-1/2 interviews at PayPal, Amazon, Uber, IBM, and Cisco evaluate deep understanding of transactional boundaries, concurrency safety, data precision, security, and trade-offs — not the sheer quantity of boilerplate services.
2. **Zero-Budget Cloud Deployability:** The system must be continuously hosted and live on the public internet so interviewers can test real APIs with zero infrastructure maintenance cost.
3. **Data Integrity & Consistency:** Subscription and payment operations must not experience split-brain states or orphaned records due to inter-service network partitions.
4. **Preservation of Existing Work:** Existing microservice decomposition code must be preserved safely without losing any work.

---

## 3. Considered Options

* **Option A:** Continue with the 8-service distributed microservices architecture.
* **Option B:** Revert and discard the microservices work back to the flat legacy monolith.
* **Option C (Chosen):** Safely archive the uncommitted microservices work to a dedicated branch (`archive/microservices-wip`), and harden Subscribly as a **Domain-Driven Modular Monolith**.

---

## 4. Decision Outcome

**Chosen Option: Option C — Domain-Driven Modular Monolith with Safe Microservices Preservation.**

### Rationale:
1. **Transactional Guarantees:** A modular monolith allows ACID transactions (`@Transactional`) across the subscription and payment tables in PostgreSQL. There is zero risk of unhandled `FeignException` leaving subscriptions in inconsistent states.
2. **Ultra-Low Memory Footprint:** The entire backend runs in a single optimized JVM container consuming <400 MB of RAM, allowing free-tier hosting on Render/Railway paired with serverless PostgreSQL (Neon).
3. **Decoupled Domain Modules:** Code will be organized into high-cohesion domain packages (`user`, `subscription`, `payment`, `notification`). Domain modules interact via clean service interfaces and in-memory Spring Domain Events (`ApplicationEventPublisher`), completely avoiding tight coupling.
4. **Microservices-Ready Extraction:** Because domain boundaries are strictly encapsulated, any module can be extracted into an independent microservice in the future if scale requires it.
5. **Interview Defensibility:** Demonstrating why a modular monolith was deliberately chosen over microservices shows architectural maturity and cost-consciousness — traits highly prized by principal engineers.

---

## 5. Consequences

### Positive:
- Single-command local setup (`.\mvnw.cmd test` / `docker compose up`).
- Zero network latency for inter-domain service calls.
- Atomic database migrations with Flyway.
- Testcontainers can test the complete system in under 30 seconds.
- Live public URL deployable on zero-budget infrastructure.

### Negative / Trade-offs:
- Entire system scales as a single deployable unit (though for portfolio scale, this is completely negligible).
- Microservice fan-out requires reading the archived branch rather than the live deployment.
