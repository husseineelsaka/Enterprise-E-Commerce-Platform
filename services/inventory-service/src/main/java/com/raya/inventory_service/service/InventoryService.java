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

    // Track what each order reserved, so compensation can release the exact stock
    private final Map<String, ReservedLine> reservations = new ConcurrentHashMap<>();

    public StockCheckResponse checkStock(String productId, int requestedQty) {
        StockItem item = stock.getOrDefault(productId,
                new StockItem(productId, 0, 0));
        boolean available = item.hasStock(requestedQty);
        return new StockCheckResponse(productId, requestedQty,
                available, item.availableQuantity() - item.reservedQuantity());
    }

    public synchronized void reserveStock(String productId, int quantity, String orderId) {
        StockItem item = stock.get(productId);
        if (item == null) {
            throw new InsufficientStockException("Product not found: " + productId);
        }
        if (!item.hasStock(quantity)) {
            throw new InsufficientStockException(
                    "Insufficient stock for " + productId + ": requested " + quantity
                            + ", available " + (item.availableQuantity() - item.reservedQuantity()));
        }
        stock.put(productId, new StockItem(productId, item.availableQuantity(),
                item.reservedQuantity() + quantity));
        reservations.put(orderId, new ReservedLine(productId, quantity));
    }

    public synchronized void releaseStock(String orderId) {
        ReservedLine line = reservations.remove(orderId);
        if (line == null) {
            return;
        }
        StockItem item = stock.get(line.productId());
        if (item == null) {
            return;
        }
        int released = Math.min(item.reservedQuantity(), line.quantity());
        stock.put(line.productId(), new StockItem(line.productId(), item.availableQuantity(),
                item.reservedQuantity() - released));
    }

    /**
     * Force a product back to a known quantity and drop any reservation held
     * against it. Stock lives in an in-memory map, so a test that needs a
     * specific starting point has no database to seed — this is how the Pact
     * provider states put the service into the state the contract assumes.
     */
    public synchronized void resetStock(String productId, int availableQuantity, int reservedQuantity) {
        stock.put(productId, new StockItem(productId, availableQuantity, reservedQuantity));
        reservations.entrySet().removeIf(entry -> entry.getValue().productId().equals(productId));
    }

    public record ReservedLine(String productId, int quantity) {
    }
}

