package com.raya.payment_service.saga.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public record InventoryReservedEvent(
        String orderId,
        String productId,
        int quantity
) {
}
