package com.raya.payment_service.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        BigDecimal amount
) {}