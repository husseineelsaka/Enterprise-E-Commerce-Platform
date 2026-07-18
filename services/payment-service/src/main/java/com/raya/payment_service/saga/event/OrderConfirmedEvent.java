package com.raya.payment_service.saga.event;

public record OrderConfirmedEvent(
        String orderId,
        String transactionId
) {
}
