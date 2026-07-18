package com.raya.inventory_service.saga;

import com.raya.inventory_service.saga.event.InventoryReleasedEvent;
import com.raya.inventory_service.saga.event.InventoryReservedEvent;
import com.raya.inventory_service.saga.event.InventoryReservationFailedEvent;
import com.raya.inventory_service.saga.event.OrderPlacedEvent;
import com.raya.inventory_service.saga.event.PaymentFailedEvent;
import com.raya.inventory_service.service.InsufficientStockException;
import com.raya.inventory_service.service.InventoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventorySagaHandler {

    private static final Logger log = LoggerFactory.getLogger(InventorySagaHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    // STEP 2 of Saga: Reserve inventory when order is placed
    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("[SAGA] Handling OrderPlaced for order: {}", event.orderId());
        try {
            inventoryService.reserveStock(event.productId(), event.quantity(), event.orderId());
            // Success: publish InventoryReserved
            kafkaTemplate.send("inventory-events", event.orderId(),
                new InventoryReservedEvent(event.orderId(), event.productId(), event.quantity()));
            log.info("[SAGA] Inventory reserved for order: {} ✅", event.orderId());
        } catch (InsufficientStockException e) {
            // Failure: publish InventoryReservationFailed (starts compensation)
            kafkaTemplate.send("inventory-events", event.orderId(),
                new InventoryReservationFailedEvent(event.orderId(), e.getMessage()));
            log.warn("[SAGA] Inventory reservation FAILED for order: {} ❌", event.orderId());
        }
    }

    // COMPENSATION: Release inventory when payment fails
    @KafkaListener(topics = "payment-events", groupId = "inventory-compensation")
    public void handlePaymentFailed(String rawEvent) {
        if (rawEvent.contains("PaymentFailed")) {
            PaymentFailedEvent event = parsePaymentFailed(rawEvent);
            inventoryService.releaseStock(event.orderId());  // undo reservation
            kafkaTemplate.send("inventory-events", event.orderId(),
                new InventoryReleasedEvent(event.orderId()));
            log.info("[SAGA] COMPENSATION: Inventory released for order: {} ✅", event.orderId());
        }
    }

    private PaymentFailedEvent parsePaymentFailed(String rawEvent) {
        JsonNode node = readTree(rawEvent);
        return new PaymentFailedEvent(node.get("orderId").asText(), node.get("reason").asText());
    }

    private JsonNode readTree(String rawEvent) {
        try {
            return objectMapper.readTree(rawEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse saga event: " + rawEvent, e);
        }
    }
}
