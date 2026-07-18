package com.raya.order_service.saga.event;

public record OrderCancelledEvent(
        String orderId,
        String reason
) {
}
