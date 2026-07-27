package com.raya.payment_service.saga.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public record OrderPlacedEvent(
        String orderId,
        String productId,
        int quantity,
        BigDecimal amount,
        String customerId
) {
}
