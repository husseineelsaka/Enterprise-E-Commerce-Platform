package com.raya.order_service.dto;

public record StockCheckResponse(
        String productId,
        int requestedQuantity,
        boolean available,
        int remainingStock
) {}