package com.raya.inventory_service.model;

public record StockItem(
        String productId,
        int availableQuantity,
        int reservedQuantity) {
    public boolean hasStock(int requested) {
        return availableQuantity - reservedQuantity >= requested;
    }
}