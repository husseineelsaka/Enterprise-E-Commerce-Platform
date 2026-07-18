package com.raya.inventory_service.saga.event;

public record PaymentCompletedEvent(
        String orderId,
        String transactionId
) {
}
