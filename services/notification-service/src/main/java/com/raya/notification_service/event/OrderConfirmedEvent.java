package com.raya.notification_service.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public record OrderConfirmedEvent(
        String orderId,
        BigDecimal amount,
        String customerId,
        String transactionId
) {
}
