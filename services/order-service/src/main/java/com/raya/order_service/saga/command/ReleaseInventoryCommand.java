package com.raya.order_service.saga.command;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public record ReleaseInventoryCommand(
        String orderId
) {
}
