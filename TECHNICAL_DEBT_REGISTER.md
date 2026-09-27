# Technical Debt Register

## Resolved Items
* **Transactional Outbox (Saga dual-write gap)** - RESOLVED in commit `session-22: close-outbox-technical-debt`. Fixed the issue where OrderService could commit to the DB but crash before publishing to Kafka.

## STILL OPEN — Carried Into Phase 4 Capstone
* **In-memory SagaState** in OrderSagaOrchestrator (Session 12) — lost on restart.
* **No database migrations** (Flyway/Liquibase) — schema managed manually since Session 1.
* **Dead-letter queue handling maturity** — DLT events logged (S13) but no automated replay/alerting.
* **Hardcoded config patterns** — PUBLIC_ROUTES-style patterns that may still exist in older session code.
* **Idempotency Keys (Payment Service)** - Prioritized for next phase: high risk of double-charging customers during retry loops.
* **API Versioning (Product Service)** - Prioritized for next phase: high risk of breaking consumers simultaneously if response shape changes without a transition window.

## NEWLY SURFACED (Architecture Clinic #2)
* **CQRS Sync Latency (Session 18)** - The `product-service` CQRS split introduced a small replication delay; we lack monitoring for when the read database falls significantly behind the write database.
* **Service Mesh Overhead (Session 21)** - Istio sidecars consume considerable resources per pod; for a platform this size, the mTLS and routing benefits might not outweigh the operational complexity and resource cost.
