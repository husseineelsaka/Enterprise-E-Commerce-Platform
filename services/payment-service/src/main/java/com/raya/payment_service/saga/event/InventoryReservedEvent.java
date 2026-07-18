package com.raya.payment_service.saga.event;

public record InventoryReservedEvent(
        String orderId,
        String productId,
        int quantity
) {
}
