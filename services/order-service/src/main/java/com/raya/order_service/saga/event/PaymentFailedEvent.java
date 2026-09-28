package com.raya.order_service.saga.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public record PaymentFailedEvent(
        String orderId,
        String reason
) {
}
