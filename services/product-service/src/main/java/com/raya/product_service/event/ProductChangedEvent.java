package com.raya.product_service.event;

public record ProductChangedEvent(Long productId, String changeType) {}
