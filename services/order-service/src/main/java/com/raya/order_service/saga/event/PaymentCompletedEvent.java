package com.raya.order_service.saga.event;

public record PaymentCompletedEvent(
        String orderId,
        String transactionId
) {
}
