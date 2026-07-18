package com.raya.payment_service.saga.event;

public record OrderCancelledEvent(
        String orderId,
        String reason
) {
}
