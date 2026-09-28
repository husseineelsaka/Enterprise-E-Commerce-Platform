package com.raya.order_service.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.raya.order_service.dto.PaymentRequest;
import com.raya.order_service.dto.PaymentResponse;
import com.raya.order_service.dto.StockCheckResponse;
import com.raya.order_service.service.InventoryClient;
import com.raya.order_service.service.PaymentServiceClient;
import feign.Feign;
import feign.FeignException;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lab 9B · Part 3 — WireMock: a REAL HTTP server, not a Java mock.
 *
 * A Mockito @Mock of InventoryClient replaces the object outright: no HTTP is
 * sent, no JSON is serialised, and a wrong URL or query-parameter name is
 * invisible. WireMock answers a real socket, so Feign has to build the request
 * and parse the response for these tests to pass — which is exactly what these
 * tests are for.
 *
 * Scope note: the deck drives these assertions through OrderService.createOrder
 * and expects CONFIRMED / PENDING. On this platform createOrder is the
 * choreography saga — it publishes OrderPlacedEvent to Kafka and always returns
 * PENDING, with no synchronous payment call and no circuit breaker. Rather than
 * reshape the saga to fit the slide, these tests target the Feign clients
 * directly, which is the boundary WireMock is actually there to prove.
 */
class OrderServiceWireMockTest {

    private static final String INVENTORY_BASE_PATH = "/api/v1/inventory";
    private static final String PAYMENT_BASE_PATH = "/api/v1/payments";

    private static WireMockServer wireMock;

    private InventoryClient inventoryClient;
    private PaymentServiceClient paymentClient;

    @BeforeAll
    static void startServer() {
        wireMock = new WireMockServer(options().dynamicPort());   // port 0 — no clashes in CI
        wireMock.start();
    }

    @AfterAll
    static void stopServer() {
        wireMock.stop();
    }

    @BeforeEach
    void resetAndBuildClients() {
        wireMock.resetAll();   // each test defines its own stubs
        String baseUrl = "http://localhost:" + wireMock.port();
        inventoryClient = buildClient(InventoryClient.class, baseUrl + INVENTORY_BASE_PATH);
        paymentClient = buildClient(PaymentServiceClient.class, baseUrl + PAYMENT_BASE_PATH);
    }

    // ── Inventory: happy path ────────────────────────────────────────────

    @Test
    @DisplayName("InventoryClient deserialises a 200 stock-check response into StockCheckResponse")
    void checkStock_deserialisesResponse_whenStockAvailable() {
        wireMock.stubFor(get(urlPathEqualTo(INVENTORY_BASE_PATH + "/check"))
                .withQueryParam("productId", equalTo("PROD-001"))
                .withQueryParam("quantity", equalTo("5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"productId":"PROD-001","requestedQuantity":5,"available":true,"remainingStock":100}
                                """)));

        StockCheckResponse response = inventoryClient.checkStock("PROD-001", 5);

        assertThat(response.productId()).isEqualTo("PROD-001");
        assertThat(response.requestedQuantity()).isEqualTo(5);
        assertThat(response.available()).isTrue();
        assertThat(response.remainingStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("InventoryClient sends GET /api/v1/inventory/check with both query parameters")
    void checkStock_sendsCorrectUrlAndQueryParameters() {
        wireMock.stubFor(get(urlPathEqualTo(INVENTORY_BASE_PATH + "/check"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"productId":"PROD-002","requestedQuantity":3,"available":true,"remainingStock":5}
                                """)));

        inventoryClient.checkStock("PROD-002", 3);

        // This is the assertion a Mockito mock can never make: it inspects the
        // request that actually went over the wire.
        wireMock.verify(getRequestedFor(urlPathEqualTo(INVENTORY_BASE_PATH + "/check"))
                .withQueryParam("productId", equalTo("PROD-002"))
                .withQueryParam("quantity", equalTo("3")));
    }

    // ── Inventory: failure paths ─────────────────────────────────────────

    @Test
    @DisplayName("A 503 from inventory-service surfaces as FeignException.ServiceUnavailable")
    void checkStock_throwsServiceUnavailable_whenInventoryIsDown() {
        wireMock.stubFor(get(urlPathEqualTo(INVENTORY_BASE_PATH + "/check"))
                .willReturn(aResponse().withStatus(503)));

        // A raw Feign client has no fallback of its own — the 503 is raised as a
        // typed exception. This is the failure a Circuit Breaker counts.
        assertThatThrownBy(() -> inventoryClient.checkStock("PROD-001", 1))
                .isInstanceOf(FeignException.ServiceUnavailable.class);
    }

    @Test
    @DisplayName("A 409 out-of-stock response surfaces as FeignException.Conflict")
    void checkStock_throwsConflict_whenStockIsInsufficient() {
        // inventory-service answers 409 (not 200) when stock is short.
        wireMock.stubFor(get(urlPathEqualTo(INVENTORY_BASE_PATH + "/check"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"productId":"PROD-003","requestedQuantity":1,"available":false,"remainingStock":0}
                                """)));

        assertThatThrownBy(() -> inventoryClient.checkStock("PROD-003", 1))
                .isInstanceOf(FeignException.Conflict.class);
    }

    // ── Payment: request body is serialised correctly ────────────────────

    @Test
    @DisplayName("PaymentServiceClient POSTs the amount and deserialises the approval")
    void processPayment_sendsAmount_andReadsApproval() {
        wireMock.stubFor(post(urlEqualTo(PAYMENT_BASE_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"transactionId":"TXN-001","status":"APPROVED","amount":250.00}
                                """)));

        PaymentResponse response = paymentClient.processPayment(new PaymentRequest(new BigDecimal("250.00")));

        assertThat(response.transactionId()).isEqualTo("TXN-001");
        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.amount()).isEqualByComparingTo("250.00");

        // Verify what was actually serialised into the request body.
        // equalToJson compares the JSON semantically, so 250.00 and 250.0 match.
        // matchingJsonPath("$.amount", equalTo(...)) would compare the rendered
        // string instead, where the BigDecimal scale is lost and only "250.0"
        // matches — a brittle assertion to write against a money field.
        wireMock.verify(postRequestedFor(urlEqualTo(PAYMENT_BASE_PATH))
                .withRequestBody(equalToJson("{\"amount\":250.00}"))
                .withRequestBody(matchingJsonPath("$.amount")));
    }

    @Test
    @DisplayName("A 503 from payment-service surfaces as FeignException.ServiceUnavailable")
    void processPayment_throwsServiceUnavailable_whenPaymentIsDown() {
        wireMock.stubFor(post(urlEqualTo(PAYMENT_BASE_PATH))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> paymentClient.processPayment(new PaymentRequest(new BigDecimal("100.00"))))
                .isInstanceOf(FeignException.ServiceUnavailable.class);
    }

    /**
     * SpringMvcContract teaches plain Feign to read the @GetMapping /
     * @RequestParam / @RequestBody annotations on the client interfaces. The
     * base path from @FeignClient(path=...) is applied by Spring Cloud at
     * runtime, so it is appended to the target URL here.
     */
    private <T> T buildClient(Class<T> clientType, String targetUrl) {
        return Feign.builder()
                .contract(new SpringMvcContract())
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .target(clientType, targetUrl);
    }
}
