package com.raya.order_service.saga;

import com.raya.order_service.dto.OrderRequest;
import com.raya.order_service.dto.OrderResponse;
import com.raya.order_service.event.OrderConfirmedEvent;
import com.raya.order_service.messaging.OrderEventPublisher;
import com.raya.order_service.model.Order;
import com.raya.order_service.model.OrderStatus;
import com.raya.order_service.repository.OrderRepository;
import com.raya.order_service.saga.command.ProcessPaymentCommand;
import com.raya.order_service.saga.command.ReleaseInventoryCommand;
import com.raya.order_service.saga.command.ReserveInventoryCommand;
import com.raya.order_service.saga.event.InventoryReleasedEvent;
import com.raya.order_service.saga.event.InventoryResultEvent;
import com.raya.order_service.saga.event.PaymentResultEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OrderSagaOrchestrator — Session 12 (Orchestration Saga).
 *
 * Unlike the Session 7 choreography (see OrderSagaEventHandler), the flow is
 * owned here: the orchestrator SENDS commands on "saga-commands" and CONSUMES
 * replies on "saga-results". Participants never decide the next step, and never
 * decide to compensate — the orchestrator does.
 *
 * Replies arrive as raw JSON because the consumer is configured with
 * StringDeserializer (see order-service.yml in the config repo). A single
 * listener reads the "type" discriminator (added by @JsonTypeInfo on the reply
 * records) and dispatches to the typed handler below — one listener, because
 * every consumer group on "saga-results" receives EVERY reply, not just the
 * ones it cares about.
 */
@Service
public class OrderSagaOrchestrator {

    static final String COMMANDS_TOPIC = "saga-commands";
    static final String RESULTS_TOPIC = "saga-results";

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    /** Saga state per orderId. In-memory for the lab — see the note at the bottom of this class. */
    private final Map<String, SagaState> sagaStates = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    /** Same publisher the choreography path uses, so both sagas notify identically. */
    @Autowired
    private OrderEventPublisher eventPublisher;

    // ---------------------------------------------------------------- start

    public OrderResponse startSaga(OrderRequest request) {
        String orderId = UUID.randomUUID().toString();

        Order order = new Order(orderId, request.productId(), request.quantity(),
                request.amount(), OrderStatus.PENDING, request.customerId());
        orderRepository.save(order);

        sagaStates.put(orderId, SagaState.STARTED);

        // STEP 1: tell inventory what to do — no event, a command.
        // State is set BEFORE the send: this method runs on the HTTP thread while
        // replies arrive on the Kafka listener thread, so a fast reply would
        // otherwise be rejected by the state guard and strand the saga.
        transition(orderId, SagaState.INVENTORY_RESERVING);
        kafkaTemplate.send(COMMANDS_TOPIC, orderId,
                new ReserveInventoryCommand(orderId, request.productId(), request.quantity()));

        return new OrderResponse(orderId, OrderStatus.PENDING.name(), "Order received — processing...");
    }

    // -------------------------------------------------------------- replies

    /**
     * Single entry point for every saga reply. Dispatch is by the "type"
     * property that @JsonTypeInfo writes into the JSON on the sending side.
     */
    @KafkaListener(topics = RESULTS_TOPIC, groupId = "order-saga-orchestrator")
    public void handleSagaResult(String rawReply) {
        JsonNode node = readTree(rawReply);
        String type = node.path("type").asText();
        String orderId = node.path("orderId").asText();

        switch (type) {
            case "InventoryResultEvent" -> handleInventoryResult(new InventoryResultEvent(
                    orderId, node.path("success").asBoolean(), text(node, "reason")));
            case "PaymentResultEvent" -> handlePaymentResult(new PaymentResultEvent(
                    orderId, node.path("success").asBoolean(), text(node, "reason"),
                    text(node, "transactionId")));
            case "InventoryReleasedEvent" -> handleInventoryReleased(new InventoryReleasedEvent(orderId));
            default -> log.debug("[SAGA] Ignoring unknown reply type '{}' on {}", type, RESULTS_TOPIC);
        }
    }

    void handleInventoryResult(InventoryResultEvent event) {
        if (notInState(event.orderId(), SagaState.INVENTORY_RESERVING)) return;

        if (event.success()) {
            transition(event.orderId(), SagaState.INVENTORY_RESERVED);

            // STEP 2: inventory is held — now charge the customer
            transition(event.orderId(), SagaState.PAYMENT_PROCESSING);
            kafkaTemplate.send(COMMANDS_TOPIC, event.orderId(),
                    new ProcessPaymentCommand(event.orderId(), getOrderAmount(event.orderId())));
        } else {
            // Nothing to compensate yet — no stock was reserved
            transition(event.orderId(), SagaState.INVENTORY_RESERVE_FAILED);
            updateOrderStatus(event.orderId(), OrderStatus.CANCELLED);
            sagaStates.remove(event.orderId());
            log.warn("[SAGA] Order {} CANCELLED — inventory reservation failed: {}",
                    event.orderId(), event.reason());
        }
    }

    void handlePaymentResult(PaymentResultEvent event) {
        if (notInState(event.orderId(), SagaState.PAYMENT_PROCESSING)) return;

        if (event.success()) {
            transition(event.orderId(), SagaState.COMPLETED);
            updateOrderStatus(event.orderId(), OrderStatus.CONFIRMED);
            notifyOrderConfirmed(event.orderId(), event.transactionId());
            sagaStates.remove(event.orderId());
            log.info("[SAGA] ✅ Order {} CONFIRMED", event.orderId());
        } else {
            // COMPENSATION: the orchestrator decides to undo step 1
            transition(event.orderId(), SagaState.PAYMENT_FAILED);
            updateOrderStatus(event.orderId(), OrderStatus.PAYMENT_FAILED);

            transition(event.orderId(), SagaState.INVENTORY_RELEASING);
            kafkaTemplate.send(COMMANDS_TOPIC, event.orderId(),
                    new ReleaseInventoryCommand(event.orderId()));
            log.warn("[SAGA] Order {} payment failed ({}) — releasing inventory",
                    event.orderId(), event.reason());
        }
    }

    void handleInventoryReleased(InventoryReleasedEvent event) {
        if (notInState(event.orderId(), SagaState.INVENTORY_RELEASING)) return;

        transition(event.orderId(), SagaState.CANCELLED);
        updateOrderStatus(event.orderId(), OrderStatus.CANCELLED);
        sagaStates.remove(event.orderId());
        log.info("[SAGA] ✅ Order {} CANCELLED — compensation complete", event.orderId());
    }

    // --------------------------------------------------------------- state

    private void transition(String orderId, SagaState newState) {
        SagaState old = sagaStates.put(orderId, newState);
        log.info("[SAGA] {} {} → {}", orderId, old, newState);
    }

    /**
     * Guards against replays and out-of-order replies: a reply is only acted on
     * when the saga is in the state that command was sent from. A null state
     * means the saga already finished (duplicate delivery) or belongs to
     * another instance.
     */
    private boolean notInState(String orderId, SagaState expected) {
        SagaState current = sagaStates.get(orderId);
        if (current != expected) {
            log.warn("[SAGA] Ignoring reply for order {} — state is {}, expected {}",
                    orderId, current, expected);
            return true;
        }
        return false;
    }

    private BigDecimal getOrderAmount(String orderId) {
        return orderRepository.findById(orderId)
                .map(Order::amount)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));
    }

    /**
     * The choreography path ends by publishing OrderConfirmedEvent, which is what
     * notification-service listens for. The orchestrated path has to do the same,
     * or an orchestrated order completes without ever notifying the customer.
     */
    private void notifyOrderConfirmed(String orderId, String transactionId) {
        orderRepository.findById(orderId).ifPresent(order ->
                eventPublisher.publishOrderConfirmed(new OrderConfirmedEvent(
                        order.orderId(), order.amount(), order.customerId(), transactionId)));
    }

    private void updateOrderStatus(String orderId, OrderStatus status) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus(status);
            orderRepository.save(order);
        });
    }

    /**
     * Reads an optional string field as a real null. Replies serialize their unused
     * field explicitly (a successful payment sends "reason": null), and asText()
     * turns that into the literal string "null".
     */
    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNull() || value.isMissingNode() ? null : value.asText();
    }

    private JsonNode readTree(String rawReply) {
        try {
            return objectMapper.readTree(rawReply);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse saga reply: " + rawReply, e);
        }
    }
}
