package com.raya.payment_service.saga.event;

public record PaymentCompletedEvent(
        String orderId,
        String transactionId
) {
}
