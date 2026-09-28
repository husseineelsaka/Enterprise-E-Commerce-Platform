package com.raya.payment_service.service;

public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
