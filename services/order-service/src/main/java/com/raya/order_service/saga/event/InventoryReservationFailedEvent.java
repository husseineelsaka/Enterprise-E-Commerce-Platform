package com.raya.order_service.saga.event;

public record InventoryReservationFailedEvent(
        String orderId,
        String reason
) {
}
