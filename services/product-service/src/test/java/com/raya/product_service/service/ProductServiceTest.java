package com.raya.product_service.service;

import com.raya.product_service.model.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    // TODO Test 1: findAll() returns empty list when no products exist
    @Test
    void findAll_returnsEmptyList_whenNoProductsExist() {
        List<Product> result = productService.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    // TODO Test 2: save() stores a product and findById() retrieves it
    @Test
    void save_storesProduct_andFindByIdRetrievesIt() {
        Product product = new Product(null, "Laptop", "15-inch laptop", BigDecimal.valueOf(999.99), "Electronics");

        Product saved = productService.save(product);

        assertNotNull(saved.id());
        assertEquals("Laptop", saved.name());

        Optional<Product> found = productService.findById(saved.id());

        assertTrue(found.isPresent());
        assertEquals(saved, found.get());
    }
    // TODO Test 3: findById() returns empty Optional for non-existent id
    @Test
    void findById_returnsEmptyOptional_forNonExistentId() {
        Optional<Product> result = productService.findById(999L);

        assertTrue(result.isEmpty());
    }

    // BONUS Test 4: deleteById() removes the product
    @Test
    void deleteById_removesProduct() {
        Product product = new Product(null, "Mouse", "Wireless mouse", BigDecimal.valueOf(19.99), "Accessories");
        Product saved = productService.save(product);

        boolean deleted = productService.deleteById(saved.id());

        assertTrue(deleted);
        assertTrue(productService.findById(saved.id()).isEmpty());
    }
    // BONUS Test 5: findAll() returns all saved products
    @Test
    void findAll_returnsAllSavedProduct(){

        Product product = new Product(null, "Laptop", "15-inch laptop", BigDecimal.valueOf(999.99), "Electronics");

        for (int i = 0; i < 5; i++) {
            Product saved = productService.save(product);
        }

        List<Product> result = productService.findAll();


        assertNotNull(result);
        assertTrue((result.size() == 5));
    }
}