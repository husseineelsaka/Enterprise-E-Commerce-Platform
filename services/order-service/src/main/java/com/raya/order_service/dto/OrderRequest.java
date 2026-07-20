package com.raya.order_service.dto;

import java.math.BigDecimal;

public record OrderRequest(String productId, int quantity, BigDecimal amount, String customerId) {

    public OrderRequest(BigDecimal amount) {
        this(null, 0, amount, null);
    }
}
