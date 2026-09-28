# Enterprise E-Commerce Platform - Demo & Showcase Guide

This document is a step-by-step guide to running the platform locally and demonstrating its enterprise-grade features (Security, Sagas, Resilience, Observability, CQRS, and Outbox) to stakeholders, senior engineers, or interviewers.

---

## Phase 1: Startup & Infrastructure

Due to the heavy infrastructure backing this platform, we rely on Docker for our base services and Spring Boot for our microservices.

### 1. Start the Backing Infrastructure
Navigate to the directory containing your `docker-compose.yml` file and start the backing services (Kafka, Zookeeper, Zipkin, Keycloak, and Databases):
```bash
docker-compose up -d
```
*Wait ~2-3 minutes to ensure Keycloak creates its realms and Kafka is fully ready to accept connections.*

### 2. Start the Microservices
The startup order is critical due to service dependencies. Run these using your IDE or via `mvn spring-boot:run` in this exact sequence:

1. **`config-server`**: Wait until it starts on `port 8888`. All other services depend on this for their `application.yaml` configurations.
2. **`eureka-server`**: Wait until it starts on `port 8761`. 
3. **`api-gateway`**: Starts on `port 8080`. This is the single entry point for all client traffic.
4. **Core Services**: Start `product-service`, `order-service`, `inventory-service`, `payment-service`, and `notification-service`.

**Verification:** Open `http://localhost:8761` (Eureka Dashboard) in your browser. You should see all of your services registered and marked as `UP`.

---

## Phase 2: Feature Demonstrations (The Showcase)

Structure your demo around **"The Happy Path"** followed by **"The Chaos Path"** to prove the resilience and architectural maturity of the platform.

### Demo 1: The Security Perimeter (OAuth2 & Keycloak)
* **The Action:** Send a `POST` request to `http://localhost:8080/api/v1/orders` without any headers.
* **The Indicator:** You immediately receive a `401 Unauthorized` response.
* **The Proof:** Fetch a valid JWT token from your local Keycloak instance (`http://localhost:8090/.../token`) and send the request again with the `Authorization: Bearer <token>` header. The request succeeds. This proves the **API Gateway OAuth2 Resource Server** is actively blocking unauthenticated traffic and enriching headers for downstream services.

### Demo 2: Distributed Sagas (Choreography / Orchestration)
* **The Action:** Place a valid order using your JWT token.
* **The Indicator:** The immediate HTTP response will show `"status": "PENDING"`.
* **The Proof:** Wait 1 second, then make a `GET` request to check the order status. It will now show `"status": "CONFIRMED"`. This proves the **Event-Driven Saga** is working: `order-service` successfully communicated with `inventory-service` and `payment-service` asynchronously via Kafka to finalize the distributed transaction without blocking the initial HTTP thread.

### Demo 3: Observability & Distributed Tracing
* **The Action:** Open the Zipkin dashboard in your browser at `http://localhost:9411`.
* **The Indicator:** Search for recent traces.
* **The Proof:** Click on the trace for the order you just placed. You will see a waterfall diagram detailing the request as it enters the `api-gateway`, routes to the `order-service`, and triggers Kafka events consumed by the `inventory-service` and `notification-service`. This proves **Micrometer and Zipkin** are successfully injecting and extracting trace IDs across HTTP and Kafka network boundaries.

### Demo 4: Resilience & Circuit Breakers (Resilience4j)
* **The Action:** Use the load testing script to blast the order-service: run `k6 run k6/checkout-stress-test.js` to simulate 150 concurrent users. (Alternatively, hit the `payment-service` repeatedly, which has a simulated 50% failure rate).
* **The Indicator:** Open `http://localhost:8080/actuator/circuitbreakers` or the specific service's actuator endpoint.
* **The Proof:** You will observe the circuit breaker state transition from `CLOSED` to `OPEN`. Subsequent requests will instantly fail with a `CallNotPermittedException` (fast failure) instead of hanging or waiting for timeouts. This proves **Resilience4j** is protecting the system from cascading failures.

### Demo 5: The Transactional Outbox Pattern
* **The Action:** Simulate a broker outage by stopping your Kafka Docker container (`docker stop <kafka-container-id>`). Place an order.
* **The Indicator:** The order creation HTTP request *succeeds*, meaning the database committed the order, but Kafka is down so no events are sent.
* **The Proof:** Check the `outbox_events` table (or in-memory repository) in the `order-service`. The event is safely persisted with `published = false`. Turn Kafka back on. Within 1 second, the background `OutboxPublisher` scheduler wakes up, grabs the event, publishes it to Kafka, and updates it to `true`. This proves you have eliminated the **Dual-Write Technical Debt**, ensuring absolute eventual consistency even during message broker outages.

### Demo 6: CQRS (Command Query Responsibility Segregation)
* **The Action:** Create a new product in the `product-service` via a `POST` request (The Command path).
* **The Proof:** Fetch the product list via a `GET` request (The Query path). Explain that, behind the scenes, the write hit the Command service and emitted an event, while the Query service independently updated its optimized read-model. This proves the architecture separates read vs. write workloads for maximum scalability.
