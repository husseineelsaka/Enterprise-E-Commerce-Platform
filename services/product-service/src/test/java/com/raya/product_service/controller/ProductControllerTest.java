package com.raya.product_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raya.product_service.model.Product;
import com.raya.product_service.projection.ProductSummaryProjection;
import com.raya.product_service.service.ProductCommandService;
import com.raya.product_service.service.ProductQueryService;
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

@WebMvcTest(ProductController.class)
@Import(TestCacheConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductCommandService commandService;

    @MockitoBean
    private ProductQueryService queryService;

    record TestProductSummary(Long id, String name, BigDecimal price, String categoryName) implements ProductSummaryProjection {
        @Override public Long getId() { return id; }
        @Override public String getName() { return name; }
        @Override public BigDecimal getPrice() { return price; }
        @Override public String getCategoryName() { return categoryName; }
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} returns 200 with the product JSON")
    void getProduct_found_returns200() throws Exception {
        ProductSummaryProjection proj = new TestProductSummary(1L, "Laptop", new BigDecimal("999.99"), "Electronics");

        when(queryService.findById(1L)).thenReturn(Optional.of(proj));

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(999.99))
                .andExpect(jsonPath("$.categoryName").value("Electronics"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} returns 404 when the product does not exist")
    void getProduct_notFound_returns404() throws Exception {
        when(queryService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/products returns 200 with the full list")
    void getAllProducts_returns200WithList() throws Exception {
        ProductSummaryProjection proj1 = new TestProductSummary(1L, "Laptop", new BigDecimal("999.99"), "Electronics");
        ProductSummaryProjection proj2 = new TestProductSummary(2L, "Mouse", new BigDecimal("19.99"), "Accessories");

        when(queryService.findAll()).thenReturn(List.of(proj1, proj2));

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
        when(commandService.create(any(Product.class))).thenReturn(saved);

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
