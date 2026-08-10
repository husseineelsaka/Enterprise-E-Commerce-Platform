package com.raya.payment_service.saga.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Reply to ProcessPaymentCommand (Session 12 — Orchestration).
 *
 * The choreography events (PaymentCompletedEvent / PaymentFailedEvent) are
 * broadcasts anyone may subscribe to. This is a reply addressed to the
 * orchestrator: one record, a success flag, and the reason when it failed.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public record PaymentResultEvent(
        String orderId,
        boolean success,
        String reason
) {
}
