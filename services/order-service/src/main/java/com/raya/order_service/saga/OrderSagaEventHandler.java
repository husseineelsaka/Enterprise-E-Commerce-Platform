package com.raya.order_service.saga;

import com.raya.order_service.event.OrderConfirmedEvent;
import com.raya.order_service.messaging.OrderEventPublisher;
import com.raya.order_service.model.Order;
import com.raya.order_service.model.OrderStatus;
import com.raya.order_service.repository.OrderRepository;
import com.raya.order_service.saga.event.InventoryReleasedEvent;
import com.raya.order_service.saga.event.PaymentCompletedEvent;
import com.raya.order_service.saga.event.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OrderSagaEventHandler — Session 7.
 * Listens for the final Saga outcomes and updates the order status.
 * Follows the slide's simplified parsing: discriminate by event type in the
 * JSON payload (the "type" field added via @JsonTypeInfo), then extract orderId.
 */
@Service
public class OrderSagaEventHandler {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventPublisher eventPublisher;

    private static final Logger log = LoggerFactory.getLogger(OrderSagaEventHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Listen for Saga completion events
    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void handlePaymentEvent(
            @Payload String rawEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        // Simplified: parse event type and orderId from JSON
        // In production: use proper JSON deserialization with type discriminator
        if (rawEvent.contains("PaymentCompleted")) {
            PaymentCompletedEvent event = parsePaymentCompleted(rawEvent);
            Order order = orderRepository.findById(event.orderId()).orElseThrow();
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("[SAGA] Order {} CONFIRMED ✅", event.orderId());

            eventPublisher.publishOrderConfirmed(new OrderConfirmedEvent(
                    order.orderId(),
                    order.amount(),
                    order.customerId(),
                    event.transactionId()
            ));
        } else if (rawEvent.contains("PaymentFailed")) {
            PaymentFailedEvent event = parsePaymentFailed(rawEvent);
            Order order = orderRepository.findById(event.orderId()).orElseThrow();
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            log.warn("[SAGA] Order {} payment failed, waiting for inventory release...", event.orderId());
        }
    }

    // Cancellation: inventory was released after payment failure
    @KafkaListener(topics = "inventory-events", groupId = "order-service-cancel")
    public void handleInventoryReleased(String rawEvent) {
        if (rawEvent.contains("InventoryReleased")) {
            InventoryReleasedEvent event = parseInventoryReleased(rawEvent);
            Order order = orderRepository.findById(event.orderId()).orElseThrow();
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("[SAGA] Order {} CANCELLED — inventory released ✅", event.orderId());
        }
    }

    private PaymentCompletedEvent parsePaymentCompleted(String rawEvent) {
        JsonNode node = readTree(rawEvent);
        return new PaymentCompletedEvent(node.get("orderId").asText(), node.get("transactionId").asText());
    }

    private PaymentFailedEvent parsePaymentFailed(String rawEvent) {
        JsonNode node = readTree(rawEvent);
        return new PaymentFailedEvent(node.get("orderId").asText(), node.get("reason").asText());
    }

    private InventoryReleasedEvent parseInventoryReleased(String rawEvent) {
        JsonNode node = readTree(rawEvent);
        return new InventoryReleasedEvent(node.get("orderId").asText());
    }

    private JsonNode readTree(String rawEvent) {
        try {
            return objectMapper.readTree(rawEvent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse saga event: " + rawEvent, e);
        }
    }
}
