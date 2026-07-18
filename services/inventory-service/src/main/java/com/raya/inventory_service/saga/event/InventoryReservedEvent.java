package com.raya.inventory_service.saga.event;

public record InventoryReservedEvent(
        String orderId,
        String productId,
        int quantity
) {
}
