package com.raya.inventory_service.service;

import com.raya.inventory_service.dto.StockCheckResponse;
import com.raya.inventory_service.model.StockItem;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InventoryService {

    // In-memory store (PostgreSQL added as homework)
    // In-memory: PROD-001 (100 units), PROD-002 (5 units), PROD-003 (0 units — out of stock)
    private final Map<String, StockItem> stock = new ConcurrentHashMap<>(Map.of(
            "PROD-001", new StockItem("PROD-001", 100, 0),
            "PROD-002", new StockItem("PROD-002", 5,  0),
            "PROD-003", new StockItem("PROD-003", 0,  0)  // out of stock
    ));

    public StockCheckResponse checkStock(String productId, int requestedQty) {
        StockItem item = stock.getOrDefault(productId,
                new StockItem(productId, 0, 0));
        boolean available = item.hasStock(requestedQty);
        return new StockCheckResponse(productId, requestedQty,
                available, item.availableQuantity() - item.reservedQuantity());
    }
}