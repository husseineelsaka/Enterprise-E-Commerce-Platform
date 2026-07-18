package com.raya.inventory_service.saga.event;

public record OrderConfirmedEvent(
        String orderId,
        String transactionId
) {
}
