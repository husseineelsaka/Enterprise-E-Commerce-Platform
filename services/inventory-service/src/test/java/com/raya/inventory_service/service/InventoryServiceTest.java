package com.raya.inventory_service.service;

import com.raya.inventory_service.dto.StockCheckResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void checkStock_returnsAvailable_whenSufficientStock() {
        StockCheckResponse result = inventoryService.checkStock("PROD-001", 10);

        assertNotNull(result);
        assertEquals("PROD-001", result.productId());
        assertEquals(10, result.requestedQuantity());
        assertTrue(result.available());
        assertEquals(100, result.remainingStock());
    }

    @Test
    void checkStock_returnsUnavailable_whenInsufficientStock() {
        StockCheckResponse result = inventoryService.checkStock("PROD-002", 10);

        assertNotNull(result);
        assertEquals("PROD-002", result.productId());
        assertEquals(10, result.requestedQuantity());
        assertFalse(result.available());
        assertEquals(5, result.remainingStock());
    }

    @Test
    void checkStock_returnsUnavailable_whenProductOutOfStock() {
        StockCheckResponse result = inventoryService.checkStock("PROD-003", 1);

        assertNotNull(result);
        assertEquals("PROD-003", result.productId());
        assertFalse(result.available());
        assertEquals(0, result.remainingStock());
    }

    @Test
    void checkStock_returnsUnavailable_whenProductNotFound() {
        StockCheckResponse result = inventoryService.checkStock("PROD-999", 1);

        assertNotNull(result);
        assertEquals("PROD-999", result.productId());
        assertFalse(result.available());
        assertEquals(0, result.remainingStock());
    }

    @Test
    void checkStock_returnsAvailable_whenRequestingExactAvailableQuantity() {
        StockCheckResponse result = inventoryService.checkStock("PROD-002", 5);

        assertNotNull(result);
        assertTrue(result.available());
        assertEquals(5, result.remainingStock());
    }

    @Test
    void checkStock_returnsAvailable_whenRequestingZeroQuantity() {
        StockCheckResponse result = inventoryService.checkStock("PROD-001", 0);

        assertNotNull(result);
        assertTrue(result.available());
    }
}
