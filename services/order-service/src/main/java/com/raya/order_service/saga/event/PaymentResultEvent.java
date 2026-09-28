package com.raya.order_service.saga.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Reply to ProcessPaymentCommand. Carries transactionId when the payment
 * succeeded and reason when it failed — the confirmation notification needs
 * the transaction id, so the two must not share a field.
 *
 * Must stay field-identical to the payment-service copy.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public record PaymentResultEvent(
        String orderId,
        boolean success,
        String reason,
        String transactionId
) {
}
