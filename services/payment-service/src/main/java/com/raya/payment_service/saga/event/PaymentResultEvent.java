package com.raya.payment_service.saga.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Reply to ProcessPaymentCommand (Session 12 — Orchestration).
 *
 * The choreography events (PaymentCompletedEvent / PaymentFailedEvent) are
 * broadcasts anyone may subscribe to. This is a reply addressed to the
 * orchestrator: one record carrying the success flag plus whichever detail
 * applies — transactionId when it succeeded, reason when it did not.
 *
 * Must stay field-identical to the order-service copy: the JSON crosses the
 * service boundary and the orchestrator dispatches on the "type" name.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public record PaymentResultEvent(
        String orderId,
        boolean success,
        String reason,
        String transactionId
) {
}
