package com.raya.order_service.saga.event;

public record OrderConfirmedEvent(
        String orderId,
        String transactionId
) {
}
