package com.raya.order_service.service;

import com.raya.order_service.dto.PaymentRequest;
import com.raya.order_service.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "PAYMENT-SERVICE",
        path = "/api/v1/payments"
)
public interface PaymentServiceClient {
    @PostMapping
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
}