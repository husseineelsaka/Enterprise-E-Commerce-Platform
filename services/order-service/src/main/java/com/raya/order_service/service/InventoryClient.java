package com.raya.order_service.service;

import com.raya.order_service.dto.StockCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "INVENTORY-SERVICE",
        path = "/api/v1/inventory",
        configuration = com.raya.order_service.config.FeignConfig.class
)
public interface InventoryClient {
    @GetMapping("/check")
    StockCheckResponse checkStock(
            @RequestParam("productId") String productId,
            @RequestParam("quantity")  int quantity
    );
}