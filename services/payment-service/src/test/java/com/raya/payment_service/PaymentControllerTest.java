package com.raya.payment_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raya.payment_service.controller.PaymentController;
import com.raya.payment_service.dto.PaymentRequest;
import com.raya.payment_service.repository.IdempotencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        // Zero failure rate and delay for predictable tests
        ReflectionTestUtils.setField(paymentController, "failureRate", 0.0);
        ReflectionTestUtils.setField(paymentController, "delayMs", 0L);
        idempotencyRepository.clear();
    }

    @Test
    @DisplayName("Session 22 Lab 18 Task 2: First request creates payment and stores idempotency record")
    void firstRequest_createsPayment() throws Exception {
        PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-123")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.transactionId").isNotEmpty());

        assertThat(idempotencyRepository.findById("key-123")).isPresent();
        assertThat(idempotencyRepository.findById("key-123").get().getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Session 22 Lab 18 Task 2: Duplicate request with same Idempotency-Key returns cached response")
    void duplicateRequest_returnsCachedResponse() throws Exception {
        PaymentRequest request = new PaymentRequest(new BigDecimal("100.00"));

        String firstResponse = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-duplicate")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Second call with same idempotency key
        String secondResponse = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-duplicate")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(secondResponse).isEqualTo(firstResponse);
    }
}
