package com.raya.order_service.saga;

import com.raya.order_service.dto.OrderRequest;
import com.raya.order_service.dto.OrderResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderSagaOrchestrator orchestrator;

    private OrderRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleRequest = new OrderRequest("PROD-100", 2, new BigDecimal("250.00"), "CUST-42");
    }

    @Test
    @DisplayName("Lab 10A: startSaga() saves order as PENDING and sends ReserveInventoryCommand")
    void startSaga_initiatesSagaAndSendsCommand() {
        OrderResponse response = orchestrator.startSaga(sampleRequest);

        assertThat(response.orderId()).isNotBlank();
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.message()).contains("processing");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().status()).isEqualTo(OrderStatus.PENDING);
        assertThat(orderCaptor.getValue().productId()).isEqualTo("PROD-100");

        ArgumentCaptor<Object> commandCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("saga-commands"), eq(response.orderId()), commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isInstanceOf(ReserveInventoryCommand.class);
        ReserveInventoryCommand cmd = (ReserveInventoryCommand) commandCaptor.getValue();
        assertThat(cmd.orderId()).isEqualTo(response.orderId());
        assertThat(cmd.productId()).isEqualTo("PROD-100");
        assertThat(cmd.quantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("Lab 10A: handleInventoryResult(success) sends ProcessPaymentCommand")
    void handleInventoryResult_success_sendsProcessPaymentCommand() {
        OrderResponse response = orchestrator.startSaga(sampleRequest);
        String orderId = response.orderId();

        Order order = new Order(orderId, "PROD-100", 2, new BigDecimal("250.00"), OrderStatus.PENDING, "CUST-42");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Mockito.reset(kafkaTemplate);

        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, true, null));

        ArgumentCaptor<Object> commandCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("saga-commands"), eq(orderId), commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isInstanceOf(ProcessPaymentCommand.class);
        ProcessPaymentCommand cmd = (ProcessPaymentCommand) commandCaptor.getValue();
        assertThat(cmd.orderId()).isEqualTo(orderId);
        assertThat(cmd.amount()).isEqualTo(new BigDecimal("250.00"));
    }

    @Test
    @DisplayName("Lab 10A: handleInventoryResult(failure) cancels order directly without compensation")
    void handleInventoryResult_failure_cancelsOrder() {
        OrderResponse response = orchestrator.startSaga(sampleRequest);
        String orderId = response.orderId();

        Order order = new Order(orderId, "PROD-100", 2, new BigDecimal("250.00"), OrderStatus.PENDING, "CUST-42");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Mockito.reset(kafkaTemplate);

        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, false, "Out of stock"));

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
        verify(kafkaTemplate, never()).send(eq("saga-commands"), eq(orderId), any());
    }

    @Test
    @DisplayName("Lab 10A: handlePaymentResult(success) marks order CONFIRMED and notifies")
    void handlePaymentResult_success_confirmsOrder() {
        OrderResponse response = orchestrator.startSaga(sampleRequest);
        String orderId = response.orderId();

        Order order = new Order(orderId, "PROD-100", 2, new BigDecimal("250.00"), OrderStatus.PENDING, "CUST-42");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Progress to payment processing
        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, true, null));

        // Payment succeeds
        orchestrator.handlePaymentResult(new PaymentResultEvent(orderId, true, null, "TX-999"));

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
        verify(eventPublisher).publishOrderConfirmed(any());
    }

    @Test
    @DisplayName("Lab 10A: handlePaymentResult(failure) triggers compensation (ReleaseInventoryCommand)")
    void handlePaymentResult_failure_triggersCompensation() {
        OrderResponse response = orchestrator.startSaga(sampleRequest);
        String orderId = response.orderId();

        Order order = new Order(orderId, "PROD-100", 2, new BigDecimal("250.00"), OrderStatus.PENDING, "CUST-42");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Progress to payment processing
        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, true, null));

        Mockito.reset(kafkaTemplate);

        // Payment fails
        orchestrator.handlePaymentResult(new PaymentResultEvent(orderId, false, "Insufficient funds", null));

        assertThat(order.status()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        ArgumentCaptor<Object> commandCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("saga-commands"), eq(orderId), commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isInstanceOf(ReleaseInventoryCommand.class);
        ReleaseInventoryCommand cmd = (ReleaseInventoryCommand) commandCaptor.getValue();
        assertThat(cmd.orderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Lab 10A: handleInventoryReleased() marks order CANCELLED after compensation")
    void handleInventoryReleased_completesCompensation() {
        OrderResponse response = orchestrator.startSaga(sampleRequest);
        String orderId = response.orderId();

        Order order = new Order(orderId, "PROD-100", 2, new BigDecimal("250.00"), OrderStatus.PENDING, "CUST-42");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Progress to payment processing -> fail payment -> inventory releasing
        orchestrator.handleInventoryResult(new InventoryResultEvent(orderId, true, null));
        orchestrator.handlePaymentResult(new PaymentResultEvent(orderId, false, "Card expired", null));

        Mockito.reset(orderRepository);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Inventory release confirmed
        orchestrator.handleInventoryReleased(new InventoryReleasedEvent(orderId));

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);
    }
}
