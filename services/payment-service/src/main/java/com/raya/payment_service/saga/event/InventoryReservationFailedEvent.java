package com.raya.payment_service.saga.event;

public record InventoryReservationFailedEvent(
        String orderId,
        String reason
) {
}
