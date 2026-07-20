package com.raya.order_service.service;

import com.raya.order_service.dto.*;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * OrderService — Session 4 (Circuit Breaker + Retry), extended in Session 5
 * (Bulkhead + TimeLimiter, full resilience stack).
 *
 * ANNOTATION ORDER IS DELIBERATE AND TAUGHT EXPLICITLY — do not reorder:
 *   @Bulkhead → @TimeLimiter → @CircuitBreaker → @Retry
 * See docs/labs/session-05-lab-3b.md for why this order matters.
 *
 * Implement the TODOs below. See docs/labs/session-04-lab-3a.md and
 * docs/labs/session-05-lab-3b.md for the full lab instructions.
 */
@Service
public class OrderService {

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private PaymentService paymentService;
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);


    // TODO 3 (Session 5): annotate with @Bulkhead(name="paymentService", fallbackMethod="bulkheadFallback")
    @Bulkhead(name = "paymentService", fallbackMethod = "bulkheadFallback")


    // TODO 4 (Session 5): annotate with @TimeLimiter(name="paymentService", fallbackMethod="timeoutFallback")
    //         Apply all four IN THIS ORDER, top to bottom:
    //         @Bulkhead, @TimeLimiter, @CircuitBreaker, @Retry
    @TimeLimiter(name = "paymentService", fallbackMethod = "timeoutFallback")


    // TODO 1 (Session 4): annotate with @CircuitBreaker(name="paymentService", fallbackMethod="paymentFallback")
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")

    // TODO 2 (Session 4): annotate with @Retry(name="paymentService")
    @Retry(name = "paymentService")


    // TODO 5: implement the method body — wrap the payment call in
    //         CompletableFuture.supplyAsync(), call paymentService.processPayment(),
    //         and return a CONFIRMED OrderResponse with the payment's transactionId
    public CompletableFuture<OrderResponse> createOrderAsync(OrderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // Step 1: Check inventory BEFORE payment
            StockCheckResponse stock = inventoryClient.checkStock(
                    request.productId(), request.quantity());
            if (!stock.available()) {
                return new OrderResponse("REJECTED",
                        "Insufficient stock: only " + stock.remainingStock() + " available");
            }
            // Step 2: Process payment (only if stock is OK)
            PaymentResponse payment = paymentService.processPayment(
                    new PaymentRequest(request.amount()));
            return new OrderResponse("CONFIRMED", payment.transactionId());
        });
    }

    // TODO 6 (Session 4): implement paymentFallback — must have the same
    //         params as createOrderAsync() PLUS Throwable as the last param.
    //         Return a CompletableFuture<OrderResponse> with status "PENDING".
    public CompletableFuture<OrderResponse> paymentFallback(OrderRequest request, Throwable ex) {
        log.warn("Payment failed, returning PENDING order. Reason: {}", ex.getMessage());
        return CompletableFuture.completedFuture(new OrderResponse(
                "PENDING",
                "Will retry payment"
        ));
    }

    // TODO 7 (Session 5): implement bulkheadFallback — params must be
    //         (OrderRequest, BulkheadFullException). Return status "QUEUED".
    public CompletableFuture<OrderResponse> bulkheadFallback(OrderRequest request, BulkheadFullException ex) {
        log.warn("[BULKHEAD] Concurrent limit reached: {}", ex.getMessage());
        return CompletableFuture.completedFuture(new OrderResponse(
                "QUEUED",
                "System busy — your order is queued"
        ));
    }

    // TODO 8 (Session 5): implement timeoutFallback — params must be
    //         (OrderRequest, TimeoutException). Return type MUST be
    //         CompletableFuture<OrderResponse> (matching the annotated method).
    //         Return status "PENDING".
    public CompletableFuture<OrderResponse> timeoutFallback(OrderRequest request, TimeoutException ex) {
        log.warn("[TIMEOUT] Payment exceeded the configured time limit: {}", ex.getMessage());
        return CompletableFuture.completedFuture(new OrderResponse(
                "PENDING",
                "Payment timed out — will retry asynchronously"
        ));
    }
}