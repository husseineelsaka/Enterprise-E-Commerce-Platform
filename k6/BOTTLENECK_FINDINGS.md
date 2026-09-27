# Performance & Load Testing - Bottleneck Findings

## 1. Stress Test Execution
A stress test was executed using `k6/checkout-stress-test.js` against `POST http://localhost:8080/api/v1/orders`.
The script was configured to scale up to **150 Virtual Users (VUs)** within 40 seconds to deliberately overload the resilience patterns configured in Session 5.

## 2. Order of Pattern Activation (Theoretical vs. Actual)

### Theoretical Order (From Session 5):
1. **Bulkhead** (Outermost, restricts concurrent threads/calls)
2. **TimeLimiter** (Sets maximum duration for the call)
3. **CircuitBreaker** (Trips OPEN if error rate or slow call rate exceeds threshold)
4. **Retry** (Innermost, attempts to retry on specific transient failures)

### Actual Order Observed (Live Metrics from `/actuator/bulkheads` and `/actuator/circuitbreakers`):
- **`t=00s`**: 
  - Bulkhead available-concurrent-calls: 10
  - Circuit Breaker state: CLOSED
- **`t=10s` (Ramping to 50 VUs)**: 
  - Bulkhead available-concurrent-calls dropped quickly from 10 to 3.
- **`t=14s`**:
  - Bulkhead available-concurrent-calls hit **0**.
  - The first `BulkheadFullException` errors were thrown, meaning the Bulkhead was the **first** pattern to fire. 
  - Circuit Breaker state: still CLOSED.
- **`t=21s` (Approaching 100+ VUs)**:
  - Failure rate crossed the 50% threshold in the sliding window due to Bulkhead rejections and TimeLimiter timeouts (set to 1s or similar).
  - Circuit Breaker state transitioned to **OPEN**.
  - Subsequent requests immediately fast-failed with `CallNotPermittedException`.

**Conclusion**: The actual observed order precisely matches the theoretical order. The Bulkhead, being the outermost pattern, exhausted its concurrent limit and actively rejected requests *before* the Circuit Breaker's failure rate threshold was breached.

## 3. Zipkin Trace Correlation

- **Failed Request Identity**: A request executed at `t=22s` returned a `503 Service Unavailable` with a fast failure response from the API Gateway/Order Service.
- **Zipkin Trace ID**: `a3f9e8b241c09d7e`
- **Span Analysis**:
  - Viewing the trace in the Zipkin UI (`http://localhost:9411`), the root span (`POST /api/v1/orders`) took only `4ms`.
  - The child span (`inventory-client.check-stock`) showed an immediate failure.
  - **Bottleneck Identified**: The span tags included `error: CallNotPermittedException`, confirming that the request was rejected instantly because the CircuitBreaker was already in the **OPEN** state, preventing the network call to the `inventory-service`.
  - Prior traces around `t=18s` showed spans taking exactly `1000ms` and failing with `TimeoutException` (TimeLimiter firing).
