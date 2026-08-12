package com.raya.order_service.controller;

import com.raya.order_service.dto.OrderRequest;
import com.raya.order_service.dto.OrderResponse;
import com.raya.order_service.saga.OrderSagaOrchestrator;
import com.raya.order_service.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OrderController — Session 7 (Choreography) + Session 12 (Orchestration).
 *
 * Both POSTs return PENDING immediately and both end in CONFIRMED or CANCELLED;
 * the difference is who drives the steps in between. Keeping them on separate
 * paths means one order is never processed by both sagas at once.
 *
 * GET returns the current persisted status, whichever saga wrote it.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderSagaOrchestrator sagaOrchestrator;

    public OrderController(OrderService orderService, OrderSagaOrchestrator sagaOrchestrator) {
        this.orderService = orderService;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    /** Session 7: publishes OrderPlacedEvent — the services choreograph themselves. */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    /** Session 12: hands the order to the orchestrator, which issues the commands. */
    @PostMapping("/orchestrated")
    public ResponseEntity<OrderResponse> createOrderOrchestrated(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(sagaOrchestrator.startSaga(request));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<String> getStatus(@PathVariable String id) {
        return orderService.findById(id)
                .map(order -> ResponseEntity.ok(order.status().name()))
                .orElse(ResponseEntity.notFound().build());
    }
}
