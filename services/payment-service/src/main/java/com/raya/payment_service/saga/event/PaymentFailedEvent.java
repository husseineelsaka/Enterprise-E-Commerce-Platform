package com.raya.payment_service.saga.event;

public record PaymentFailedEvent(
        String orderId,
        String reason
) {
}
