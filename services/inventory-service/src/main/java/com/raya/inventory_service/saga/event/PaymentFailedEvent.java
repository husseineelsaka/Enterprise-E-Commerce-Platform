package com.raya.inventory_service.saga.event;

public record PaymentFailedEvent(
        String orderId,
        String reason
) {
}
