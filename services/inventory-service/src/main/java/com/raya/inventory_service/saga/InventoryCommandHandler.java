package com.raya.inventory_service.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raya.inventory_service.saga.event.InventoryReleasedEvent;
import com.raya.inventory_service.saga.event.InventoryResultEvent;
import com.raya.inventory_service.service.InsufficientStockException;
import com.raya.inventory_service.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * InventoryCommandHandler — Session 12 (Orchestration Saga).
 *
 * The choreography counterpart (InventorySagaHandler) decides for itself what
 * to do next: it watches order-events and payment-events and picks its own
 * moment to compensate. This handler decides nothing. It executes the command
 * the orchestrator sent and reports the outcome back — including the release,
 * which is now ordered by the orchestrator instead of triggered by observing
 * someone else's failure.
 *
 * Both handlers can run at the same time: they listen on different topics, so
 * the Session 7 flow and the Session 12 flow never collide.
 */
@Service
public class InventoryCommandHandler {

    static final String COMMANDS_TOPIC = "saga-commands";
    static final String RESULTS_TOPIC = "saga-results";

    private static final Logger log = LoggerFactory.getLogger(InventoryCommandHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Every participant on "saga-commands" receives every command, so the
     * payment commands land here too — they are ignored, not failed.
     */
    @KafkaListener(topics = COMMANDS_TOPIC, groupId = "inventory-command-handler")
    public void handleCommand(String rawCommand) {
        JsonNode node = readTree(rawCommand);
        String type = node.path("type").asText();
        String orderId = node.path("orderId").asText();

        switch (type) {
            case "ReserveInventoryCommand" -> reserve(orderId,
                    node.path("productId").asText(), node.path("quantity").asInt());
            case "ReleaseInventoryCommand" -> release(orderId);
            default -> log.debug("[SAGA] Not an inventory command: '{}' — ignored", type);
        }
    }

    private void reserve(String orderId, String productId, int quantity) {
        log.info("[SAGA] Command ReserveInventory for order: {}", orderId);
        try {
            inventoryService.reserveStock(productId, quantity, orderId);
            reply(orderId, new InventoryResultEvent(orderId, true, null));
            log.info("[SAGA] Inventory reserved for order: {} ✅", orderId);
        } catch (InsufficientStockException e) {
            reply(orderId, new InventoryResultEvent(orderId, false, e.getMessage()));
            log.warn("[SAGA] Inventory reservation FAILED for order: {} ❌ — {}", orderId, e.getMessage());
        }
    }

    private void release(String orderId) {
        log.info("[SAGA] Command ReleaseInventory (compensation) for order: {}", orderId);
        inventoryService.releaseStock(orderId);
        reply(orderId, new InventoryReleasedEvent(orderId));
        log.info("[SAGA] COMPENSATION: inventory released for order: {} ✅", orderId);
    }

    private void reply(String orderId, Object result) {
        kafkaTemplate.send(RESULTS_TOPIC, orderId, result);
    }

    private JsonNode readTree(String rawCommand) {
        try {
            return objectMapper.readTree(rawCommand);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse saga command: " + rawCommand, e);
        }
    }
}
