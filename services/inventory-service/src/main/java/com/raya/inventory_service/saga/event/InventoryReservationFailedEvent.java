package com.raya.inventory_service.saga.event;

public record InventoryReservationFailedEvent(
        String orderId,
        String reason
) {
}
