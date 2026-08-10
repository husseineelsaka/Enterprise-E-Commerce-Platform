package com.raya.order_service.saga.command;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public record ReserveInventoryCommand(
        String orderId,
        String productId,
        int quantity
) {
}
