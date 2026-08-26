package com.raya.payment_service.dto;

import java.math.BigDecimal;
public record PaymentResponse(
        String transactionId,
        String status,
        BigDecimal amount
) {}