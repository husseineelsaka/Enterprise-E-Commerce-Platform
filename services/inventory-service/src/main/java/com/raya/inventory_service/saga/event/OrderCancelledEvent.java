package com.raya.inventory_service.saga.event;

public record OrderCancelledEvent(
        String orderId,
        String reason
) {
}
