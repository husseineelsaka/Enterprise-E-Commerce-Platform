package com.raya.order_service.saga.command;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.math.BigDecimal;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public record ProcessPaymentCommand(
        String orderId,
        BigDecimal amount
) {
}
