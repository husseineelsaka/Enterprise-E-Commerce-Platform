package com.raya.inventory_service.controller;


import com.raya.inventory_service.dto.StockCheckResponse;
import com.raya.inventory_service.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/inventory")
public class InventoryController {
    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/check")
    public ResponseEntity<StockCheckResponse> checkStock(
            @RequestParam String productId,
            @RequestParam int quantity) {
        StockCheckResponse response = inventoryService.checkStock(productId, quantity);
        if (!response.available()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }
}

