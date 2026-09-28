package com.raya.order_service.repository;

import com.raya.order_service.model.Order;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OrderRepository (PostgreSQL added in a later session).
 * Implements the same save/findById contract a Spring Data JPA repository
 * would expose, so swapping to JPA later requires no changes in OrderService.
 */
@Repository
public class OrderRepository {

    private final Map<String, Order> store = new ConcurrentHashMap<>();

    public Order save(Order order) {
        store.put(order.orderId(), order);
        return order;
    }

    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(store.get(orderId));
    }
}
