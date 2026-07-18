package com.raya.order_service.saga.event;

public record PaymentFailedEvent(
        String orderId,
        String reason
) {
}
