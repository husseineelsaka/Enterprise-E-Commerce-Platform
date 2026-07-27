package com.raya.order_service.messaging;

import com.raya.order_service.event.OrderConfirmedEvent;
import com.raya.order_service.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    public static final String ORDER_EVENTS_TOPIC = "order-events";
    public static final String ORDER_CONFIRMED_TOPIC = "order-notification";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderPlaced(OrderCreatedEvent event) {

        kafkaTemplate.send(
                ORDER_EVENTS_TOPIC,
                event.orderId(),
                event
        ).join();

        log.info("[SAGA] Published OrderCreatedEvent for order: {}", event.orderId());
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {

        kafkaTemplate.send(
                ORDER_CONFIRMED_TOPIC,
                event.orderId(),
                event
        ).join();

        log.info("[SAGA] Published OrderConfirmedEvent for order: {}", event.orderId());
    }
}
