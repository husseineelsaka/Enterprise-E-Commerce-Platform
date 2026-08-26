package com.raya.order_service.model;

import java.math.BigDecimal;

public class Order {

    private final String orderId;
    private final String productId;
    private final int quantity;
    private final BigDecimal amount;
    private final String customerId;
    private OrderStatus status;

    public Order(String orderId, String productId, int quantity, BigDecimal amount, OrderStatus status) {
        this(orderId, productId, quantity, amount, status, null);
    }

    public Order(String orderId, String productId, int quantity, BigDecimal amount, OrderStatus status, String customerId) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
        this.status = status;
        this.customerId = customerId;
    }

    public String orderId() { return orderId; }
    public String productId() { return productId; }
    public int quantity() { return quantity; }
    public BigDecimal amount() { return amount; }
    public String customerId() { return customerId; }
    public OrderStatus status() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}
