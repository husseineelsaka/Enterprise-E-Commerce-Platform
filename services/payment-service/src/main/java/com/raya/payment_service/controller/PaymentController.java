package com.raya.payment_service.controller;

import com.raya.payment_service.dto.PaymentRequest;
import com.raya.payment_service.dto.PaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;
import java.util.UUID;

/**
 * PaymentController — Session 4, Lab 3A, Task 1.
 *
 * Implement the TODOs below. See docs/labs/session-04-lab-3a.md.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Value("${payment.failure-rate:0.5}")
    private double failureRate;

    @Value("${payment.delay-ms:0}")
    private long delayMs;

    private final Random random = new Random();

    // TODO 1: Add a POST /api/payments endpoint, accepting a PaymentRequest body
    // TODO 2: If delayMs > 0, Thread.sleep(delayMs) to simulate slowness (Session 5)
    // TODO 3: Using a Random, throw a RuntimeException with probability failureRate
    // TODO 4: Otherwise return 200 OK with a PaymentResponse
    //         (transactionId = a random UUID, status = "APPROVED", amount = request.amount())
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) throws InterruptedException {
        if(delayMs > 0){
            Thread.sleep(delayMs);
        }

        if (random.nextDouble() < failureRate) {
            throw new RuntimeException("Payment gateway timeout");
        }


        return ResponseEntity.ok(new PaymentResponse(
                UUID.randomUUID().toString(),
                "APPROVED",
                request.amount()
        ));
    }
}