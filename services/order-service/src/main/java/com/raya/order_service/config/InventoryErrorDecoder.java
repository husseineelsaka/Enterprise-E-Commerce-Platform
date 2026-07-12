package com.raya.order_service.config;

import com.raya.order_service.exception.InsufficientStockException;
import com.raya.order_service.exception.ProductNotFoundException;
import com.raya.order_service.exception.ServiceUnavailableException;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

@Component
public class InventoryErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            case 409 -> new InsufficientStockException("Product out of stock");
            case 404 -> new ProductNotFoundException("Product not found");
            case 503 -> new ServiceUnavailableException("Inventory unavailable");
            default -> new FeignException.InternalServerError(
                    "Unexpected error", null, null, null);
        };
    }
}
