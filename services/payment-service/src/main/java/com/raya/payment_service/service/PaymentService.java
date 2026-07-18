package com.raya.payment_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Value("${payment.failure-rate:0.5}")
    private double failureRate;

    private final Random random = new Random();

    /**
     * Process payment for an order. Simulates a payment gateway:
     * fails with probability = failureRate, otherwise returns a transaction id.
     */
    public String processPayment(String orderId) {
        if (random.nextDouble() < failureRate) {
            throw new PaymentException("Payment gateway declined for order " + orderId);
        }
        String txId = UUID.randomUUID().toString();
        log.info("[PAYMENT] Payment approved for order {} — txId {}", orderId, txId);
        return txId;
    }
}
