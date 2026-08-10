package com.raya.product_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raya.product_service.model.Product;
import com.raya.product_service.service.ProductService;
import com.raya.product_service.support.TestCacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lab 9A · Part 2 — @WebMvcTest slice for ProductController.
 *
 * Loads only DispatcherServlet + ProductController + Jackson + the validation
 * filters. No ProductService (it is a Mockito bean override), no repository,
 * no DataSource, no Redis. MockMvc dispatches in memory — no socket is opened.
 *
 * On Spring Boot 4 the slice annotation lives in
 * org.springframework.boot.webmvc.test.autoconfigure and @MockBean is gone —
 * @MockitoBean from spring-test replaces it.
 */
@WebMvcTest(ProductController.class)
@Import(TestCacheConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @Test
    @DisplayName("GET /api/v1/products/{id} returns 200 with the product JSON")
    void getProduct_found_returns200() throws Exception {
        Product product = new Product(1L, "Laptop", "15-inch laptop", new BigDecimal("999.99"), "Electronics");
        when(productService.findById(1L)).thenReturn(Optional.of(product));

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(999.99))
                .andExpect(jsonPath("$.category").value("Electronics"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} returns 404 when the product does not exist")
    void getProduct_notFound_returns404() throws Exception {
        when(productService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/products returns 200 with the full list")
    void getAllProducts_returns200WithList() throws Exception {
        when(productService.findAll()).thenReturn(List.of(
                new Product(1L, "Laptop", "15-inch laptop", new BigDecimal("999.99"), "Electronics"),
                new Product(2L, "Mouse", "Wireless mouse", new BigDecimal("19.99"), "Accessories")));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[1].name").value("Mouse"));
    }

    @Test
    @DisplayName("POST /api/v1/products returns 201 with the created product")
    void createProduct_valid_returns201() throws Exception {
        Product request = new Product("Keyboard", "Mechanical keyboard", new BigDecimal("89.50"), "Accessories");
        Product saved = new Product(10L, "Keyboard", "Mechanical keyboard", new BigDecimal("89.50"), "Accessories");
        when(productService.save(any(Product.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.price").value(89.50));
    }

    @Test
    @DisplayName("POST /api/v1/products with a missing name returns 400")
    void createProduct_missingName_returns400() throws Exception {
        // name is @NotBlank on the entity and the controller argument is @Valid,
        // so the request must never reach the service.
        String bodyWithoutName = """
                {"description":"Mechanical keyboard","price":89.50,"category":"Accessories"}
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithoutName))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/products with a negative price returns 400")
    void createProduct_negativePrice_returns400() throws Exception {
        String bodyWithNegativePrice = """
                {"name":"Keyboard","description":"Mechanical keyboard","price":-1,"category":"Accessories"}
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithNegativePrice))
                .andExpect(status().isBadRequest());
    }
}
