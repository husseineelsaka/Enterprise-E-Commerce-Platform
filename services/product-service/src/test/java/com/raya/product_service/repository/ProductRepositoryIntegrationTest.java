package com.raya.product_service.repository;

import com.raya.product_service.model.Product;
import com.raya.product_service.support.TestCacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Lab 9A · Part 3 — repository integration test against a REAL PostgreSQL.
 *
 * replace = NONE stops Boot swapping in H2. H2 silently tolerates types and
 * SQL that PostgreSQL rejects, which is exactly how a suite goes green while
 * production fails; running the same postgres:16-alpine image as
 * docker-compose.yml removes that gap.
 *
 * The container is static, so it starts once for the whole class — not per
 * @Test — and is torn down when the class finishes.
 *
 * TestContainers 2.x note: PostgreSQLContainer moved to
 * org.testcontainers.postgresql and is no longer a generic type, so the 1.x
 * `PostgreSQLContainer<?> c = new PostgreSQLContainer<>(...)` form no longer
 * compiles.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import(TestCacheConfig.class)
class ProductRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("productdb_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("save then findById round-trips through real PostgreSQL")
    void save_andFindById_roundTrip() {
        Product product = new Product("Laptop", "15-inch laptop", new BigDecimal("999.99"), "Electronics");

        Product saved = productRepository.save(product);
        Optional<Product> found = productRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Laptop", found.get().getName());
        assertEquals("15-inch laptop", found.get().getDescription());
        assertEquals("Electronics", found.get().getCategory());
        // numeric(38,2) comes back scaled — compare numerically, not by toString.
        assertEquals(0, new BigDecimal("999.99").compareTo(found.get().getPrice()));
    }

    @Test
    @DisplayName("findByPriceLessThan returns only the products under the threshold")
    void findByPriceLessThan_returnsMatchingProducts() {
        productRepository.saveAll(List.of(
                new Product("Mouse", "Wireless mouse", new BigDecimal("29.99"), "Accessories"),
                new Product("Monitor", "27-inch monitor", new BigDecimal("399.99"), "Electronics")));

        List<Product> cheap = productRepository.findByPriceLessThan(new BigDecimal("50.00"));

        assertEquals(1, cheap.size());
        assertEquals("Mouse", cheap.get(0).getName());
    }

    @Test
    @DisplayName("findByPriceLessThan returns an empty list when nothing is under the threshold")
    void findByPriceLessThan_returnsEmpty_whenNoMatch() {
        productRepository.save(new Product("Monitor", "27-inch monitor", new BigDecimal("399.99"), "Electronics"));

        assertTrue(productRepository.findByPriceLessThan(new BigDecimal("10.00")).isEmpty());
    }

    @Test
    @DisplayName("deleteById removes the row from the real database")
    void deleteById_removesRow() {
        Product saved = productRepository.save(
                new Product("Webcam", "1080p webcam", new BigDecimal("59.00"), "Accessories"));

        productRepository.deleteById(saved.getId());

        assertTrue(productRepository.findById(saved.getId()).isEmpty());
    }
}
