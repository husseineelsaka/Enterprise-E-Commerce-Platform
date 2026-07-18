package com.raya.inventory_service.saga.event;

public record InventoryReleasedEvent(
        String orderId
) {
}
