package com.raya.payment_service.model;

import java.time.Instant;

/**
 * IdempotencyRecord — Session 22.
 *
 * Stores idempotency keys for payment requests to prevent duplicate charges.
 * Closes technical debt from Session 4:
 * "No idempotency on payment retry — duplicate charge risk if Retry
 * annotation fires the same order ID a second time."
 */
public class IdempotencyRecord {

    private String idempotencyKey;
    private String orderId;
    private String status; // "PROCESSING" | "COMPLETED" | "FAILED"
    private String responsePayload; // transactionId or cached response info
    private Instant createdAt;

    public IdempotencyRecord() {}

    public IdempotencyRecord(String idempotencyKey, String orderId) {
        this.idempotencyKey = idempotencyKey;
        this.orderId = orderId;
        this.status = "PROCESSING";
        this.createdAt = Instant.now();
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void complete(String responsePayload) {
        this.status = "COMPLETED";
        this.responsePayload = responsePayload;
    }

    public void fail(String reason) {
        this.status = "FAILED";
        this.responsePayload = reason;
    }
}
