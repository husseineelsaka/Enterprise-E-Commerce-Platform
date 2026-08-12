package com.raya.inventory_service.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.raya.inventory_service.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

/**
 * Lab 9B · Part 2 — Pact PROVIDER verification.
 *
 * Reads the contract order-service generated, replays each interaction against
 * this service running for real on a random port, and compares the responses to
 * what the consumer said it needs.
 *
 * Run order matters: the consumer test has to run first, because this test
 * reads ../order-service/target/pacts. A `mvn clean` in order-service deletes
 * that folder, so the sequence is
 *   1. mvn test -pl … order-service   (writes the pact)
 *   2. mvn test -pl … inventory-service (verifies it)
 *
 * To watch it catch a real break: rename "available" to "inStock" in
 * StockCheckResponse. InventoryServiceTest and InventoryControllerTest stay
 * green — they never look at the JSON — and this test fails with the missing
 * field. That is the whole point of contract testing.
 */
@Provider("inventory-service")
@PactFolder("../order-service/target/pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        // Keep the verification hermetic: no Eureka registration, and no Kafka
        // listeners trying to reach a broker that is not running under `mvn test`.
        "eureka.client.enabled=false",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "spring.kafka.listener.auto-startup=false",
        "spring.cloud.config.enabled=false"
})
class InventoryServicePactVerificationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private InventoryService inventoryService;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    /** Matches given("PROD-001 has 100 units in stock") in the consumer test. */
    @State("PROD-001 has 100 units in stock")
    void prod001HasOneHundredUnits() {
        // Stock is an in-memory map, so put it back to the state the contract
        // assumes rather than depending on whatever earlier tests left behind.
        inventoryService.resetStock("PROD-001", 100, 0);
    }
}
