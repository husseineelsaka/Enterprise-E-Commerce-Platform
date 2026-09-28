package com.raya.inventory_service.controller;

import com.raya.inventory_service.dto.StockCheckResponse;
import com.raya.inventory_service.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController inventoryController;

    @Test
    void checkStock_returnsOk_whenStockAvailable() {
        StockCheckResponse response = new StockCheckResponse("PROD-001", 10, true, 90);
        when(inventoryService.checkStock("PROD-001", 10)).thenReturn(response);

        ResponseEntity<StockCheckResponse> result = inventoryController.checkStock("PROD-001", 10);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().available());
    }

    @Test
    void checkStock_returnsConflict_whenStockUnavailable() {
        StockCheckResponse response = new StockCheckResponse("PROD-002", 10, false, 5);
        when(inventoryService.checkStock("PROD-002", 10)).thenReturn(response);

        ResponseEntity<StockCheckResponse> result = inventoryController.checkStock("PROD-002", 10);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
        assertNotNull(result.getBody());
        assertFalse(result.getBody().available());
    }

    @Test
    void checkStock_callsServiceWithCorrectParameters() {
        when(inventoryService.checkStock("PROD-001", 5)).thenReturn(mock(StockCheckResponse.class));

        inventoryController.checkStock("PROD-001", 5);

        verify(inventoryService, times(1)).checkStock("PROD-001", 5);
    }
}
