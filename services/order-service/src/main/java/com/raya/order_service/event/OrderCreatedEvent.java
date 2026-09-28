package com.raya.order_service.event;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        String orderId,
        BigDecimal amount,
        String customerId
) {
}
