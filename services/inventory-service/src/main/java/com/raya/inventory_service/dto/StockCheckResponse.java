package com.raya.inventory_service.dto;

public record StockCheckResponse(
        String productId,
        int requestedQuantity,
        boolean available,
        int remainingStock
) {}