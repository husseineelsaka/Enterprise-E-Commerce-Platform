package com.raya.order_service.saga.event;

public record InventoryReleasedEvent(
        String orderId
) {
}
