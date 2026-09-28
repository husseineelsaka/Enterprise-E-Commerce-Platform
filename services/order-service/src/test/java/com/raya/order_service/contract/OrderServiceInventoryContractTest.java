package com.raya.order_service.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.raya.order_service.dto.StockCheckResponse;
import com.raya.order_service.service.InventoryClient;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lab 9B · Part 1 — Pact CONSUMER test.
 *
 * order-service is the consumer: it declares exactly what it needs back from
 * inventory-service. Running this writes the contract to
 * target/pacts/order-service-inventory-service.json, which the provider test in
 * inventory-service then replays against its real API.
 *
 * This is not an end-to-end test and it asserts no business logic. It answers
 * one question: does the provider's response SHAPE match what our Feign client
 * deserialises? If inventory-service renames "available" to "inStock", its own
 * unit tests stay green and the provider verification is what fails.
 *
 * Two deliberate departures from the slide deck:
 *  - No fixed port. The deck pins the mock server to 8888, which is the
 *    config-server port on this platform. Pact picks a free port instead and
 *    the test reads it from MockServer#getUrl.
 *  - pactVersion = V3. Pact 4.6 defaults to the V4 spec, whose builder returns
 *    a V4Pact from a PactBuilder; the RequestResponsePact/PactDslWithProvider
 *    pair the deck uses is the V3 signature, so the spec version is pinned to
 *    match.
 *  - The Feign client is built with SpringMvcContract. Plain Feign only
 *    understands its own @RequestLine annotations, so without this contract it
 *    cannot read the @GetMapping/@RequestParam on InventoryClient. The
 *    "/api/v1/inventory" prefix comes from @FeignClient(path=...), which is
 *    applied by Spring Cloud at runtime and so has to be added to the target
 *    URL by hand here.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "inventory-service", pactVersion = PactSpecVersion.V3)
class OrderServiceInventoryContractTest {

    private static final String INVENTORY_BASE_PATH = "/api/v1/inventory";

    /** The contract: what order-service expects from inventory-service. */
    @Pact(consumer = "order-service", provider = "inventory-service")
    public RequestResponsePact checkStockAvailable(PactDslWithProvider builder) {
        return builder
                .given("PROD-001 has 100 units in stock")
                .uponReceiving("a stock check for PROD-001 quantity 5")
                .path(INVENTORY_BASE_PATH + "/check")
                .method("GET")
                .query("productId=PROD-001&quantity=5")
                .willRespondWith()
                .status(200)
                .body(LambdaDsl.newJsonBody(body -> body
                        // Field NAMES are the contract. A rename breaks the consumer.
                        .stringValue("productId", "PROD-001")
                        .integerType("requestedQuantity", 5)
                        .booleanValue("available", true)
                        // integerType matches the TYPE — the exact number is only an example.
                        .integerType("remainingStock", 100)
                ).build())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "checkStockAvailable")
    @DisplayName("InventoryClient deserialises the contracted stock-check response")
    void checkStock_deserialisesContractedFields(MockServer mockServer) {
        InventoryClient client = buildFeignClient(mockServer.getUrl());

        StockCheckResponse response = client.checkStock("PROD-001", 5);

        assertThat(response.productId()).isEqualTo("PROD-001");
        assertThat(response.requestedQuantity()).isEqualTo(5);
        assertThat(response.available()).isTrue();
        assertThat(response.remainingStock()).isGreaterThan(0);
    }

    private InventoryClient buildFeignClient(String baseUrl) {
        return Feign.builder()
                .contract(new SpringMvcContract())
                .decoder(new JacksonDecoder())
                .target(InventoryClient.class, baseUrl + INVENTORY_BASE_PATH);
    }
}
