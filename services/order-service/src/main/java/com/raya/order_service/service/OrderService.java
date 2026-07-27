package com.raya.order_service.service;

import com.raya.order_service.dto.OrderRequest;
import com.raya.order_service.dto.OrderResponse;
import com.raya.order_service.model.Order;
import com.raya.order_service.model.OrderStatus;
import com.raya.order_service.repository.OrderRepository;
import com.raya.order_service.saga.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * OrderService — Session 7 (Choreography Saga).
 * createOrder() persists the order in PENDING state and publishes
 * OrderPlacedEvent to start the saga. It returns immediately with PENDING —
 * the final CONFIRMED / CANCELLED outcome arrives later via Kafka events
 * handled by OrderSagaEventHandler.
 */
@Service
public class OrderService {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public OrderResponse createOrder(OrderRequest request) {
        // Step 1: Create order in PENDING state (local transaction)
        Order order = new Order(UUID.randomUUID().toString(), request.productId(),
                request.quantity(), request.amount(), OrderStatus.PENDING, request.customerId());
        orderRepository.save(order);

        // Step 2: Publish event to start the Saga (async — no waiting)
        kafkaTemplate.send("order-events", order.orderId(),
                new OrderPlacedEvent(order.orderId(), request.productId(),
                        request.quantity(), request.amount(), request.customerId()));
        log.info("[SAGA] OrderPlacedEvent published for order: {}", order.orderId());

        // Return immediately — client gets PENDING, not final state
        return new OrderResponse(order.orderId(), OrderStatus.PENDING.name(), "Order received — processing...");
    }

    public java.util.Optional<Order> findById(String orderId) {
        return orderRepository.findById(orderId);
    }
}
