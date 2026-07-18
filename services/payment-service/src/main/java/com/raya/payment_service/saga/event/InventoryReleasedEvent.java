package com.raya.payment_service.saga.event;

public record InventoryReleasedEvent(
        String orderId
) {
}
