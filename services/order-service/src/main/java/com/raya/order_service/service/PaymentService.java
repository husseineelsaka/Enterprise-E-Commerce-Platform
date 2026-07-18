package com.raya.order_service.service;

import com.raya.order_service.dto.PaymentRequest;
import com.raya.order_service.dto.PaymentResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Random;

/**
 * PaymentService — Session 4, Lab 3A.
 *
 * IMPORTANT — scope note (read before touching this in Session 6+):
 * This is an in-process Spring bean stub, NOT an HTTP call to the real
 * services/payment-service module. See docs/labs/session-04-lab-3a.md and
 * docs/architecture/platform-overview.md for the full rationale (Feign /
 * real network calls are Session 6 content).
 *
 * Implement the TODO below. See docs/labs/session-04-lab-3a.md.
 */
@Service
public class PaymentService {

    private final Random random = new Random();
//    private final RestTemplate restTemplate;

    // TODO: Simulate a 50% failure rate for demo purposes.
    //       If random.nextInt(10) < 5, throw a RuntimeException("Payment Service unavailable").
    //       Otherwise return a new PaymentResponse(request.amount()).
    public PaymentResponse processPayment(PaymentRequest request) {
        if(random.nextInt(10) < 5)
        {
            throw new RuntimeException("Payment Service unavailable");

        }
        return new PaymentResponse(java.util.UUID.randomUUID().toString(), "APPROVED", request.amount());
    }
}