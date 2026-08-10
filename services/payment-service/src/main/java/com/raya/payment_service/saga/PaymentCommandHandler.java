package com.raya.payment_service.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raya.payment_service.saga.event.PaymentResultEvent;
import com.raya.payment_service.service.PaymentException;
import com.raya.payment_service.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * PaymentCommandHandler — Session 12 (Orchestration Saga).
 *
 * In choreography (PaymentSagaHandler) payment infers its turn from someone
 * else's event: "inventory was reserved, therefore I should charge". Here it is
 * told explicitly, and it never has to know that inventory exists.
 */
@Service
public class PaymentCommandHandler {

    static final String COMMANDS_TOPIC = "saga-commands";
    static final String RESULTS_TOPIC = "saga-results";

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /** Inventory commands land here too — ignored, not failed. */
    @KafkaListener(topics = COMMANDS_TOPIC, groupId = "payment-command-handler")
    public void handleCommand(String rawCommand) {
        JsonNode node = readTree(rawCommand);
        String type = node.path("type").asText();

        if (!"ProcessPaymentCommand".equals(type)) {
            log.debug("[SAGA] Not a payment command: '{}' — ignored", type);
            return;
        }

        String orderId = node.path("orderId").asText();
        log.info("[SAGA] Command ProcessPayment for order: {}", orderId);
        try {
            String txId = paymentService.processPayment(orderId);
            reply(orderId, new PaymentResultEvent(orderId, true, null, txId));
            log.info("[SAGA] Payment COMPLETED for order: {} ✅ — txId {}", orderId, txId);
        } catch (PaymentException e) {
            reply(orderId, new PaymentResultEvent(orderId, false, e.getMessage(), null));
            log.warn("[SAGA] Payment FAILED for order: {} ❌ — {}", orderId, e.getMessage());
        }
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
