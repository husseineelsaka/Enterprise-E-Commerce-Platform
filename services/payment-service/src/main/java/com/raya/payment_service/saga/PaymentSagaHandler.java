package com.raya.payment_service.saga;

import com.raya.payment_service.saga.event.InventoryReservedEvent;
import com.raya.payment_service.saga.event.PaymentCompletedEvent;
import com.raya.payment_service.saga.event.PaymentFailedEvent;
import com.raya.payment_service.service.PaymentException;
import com.raya.payment_service.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentSagaHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // STEP 3 of Saga: Process payment when inventory reserved
    @KafkaListener(topics = "inventory-events", groupId = "payment-service")
    public void handleInventoryReserved(String rawEvent) {
        if (!rawEvent.contains("InventoryReserved")) return;  // ignore other events
        InventoryReservedEvent event = parseInventoryReserved(rawEvent);

        log.info("[SAGA] Processing payment for order: {}", event.orderId());
        try {
            String txId = paymentService.processPayment(event.orderId());
            kafkaTemplate.send("payment-events", event.orderId(),
                new PaymentCompletedEvent(event.orderId(), txId));
            log.info("[SAGA] Payment COMPLETED for order: {} ✅", event.orderId());
        } catch (PaymentException e) {
            kafkaTemplate.send("payment-events", event.orderId(),
                new PaymentFailedEvent(event.orderId(), e.getMessage()));
            log.warn("[SAGA] Payment FAILED for order: {} ❌ — triggering compensation", event.orderId());
        }
    }

    private InventoryReservedEvent parseInventoryReserved(String rawEvent) {
        JsonNode node = readTree(rawEvent);
        return new InventoryReservedEvent(
                node.get("orderId").asText(),
                node.get("productId").asText(),
                node.get("quantity").asInt());
    }

    private JsonNode readTree(String rawEvent) {
        try {
            return objectMapper.readTree(rawEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse saga event: " + rawEvent, e);
        }
    }
}
