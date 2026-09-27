# Technical Debt Register

## Resolved Items
* **Transactional Outbox (Saga dual-write gap)** - RESOLVED in commit `session-22: close-outbox-technical-debt`. Fixed the issue where OrderService could commit to the DB but crash before publishing to Kafka.

## Prioritized Debt Items
* **Idempotency Keys (Payment Service)** - Prioritized for next phase: high risk of double-charging customers during retry loops.
* **API Versioning (Product Service)** - Prioritized for next phase: high risk of breaking consumers simultaneously if response shape changes without a transition window.
