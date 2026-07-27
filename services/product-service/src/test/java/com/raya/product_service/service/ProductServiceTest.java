package com.raya.product_service.service;

import com.raya.product_service.model.Product;
import com.raya.product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void findAll_returnsEmptyList_whenNoProductsExist() {
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        List<Product> result = productService.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productRepository).findAll();
    }

    @Test
    void save_storesProduct_andFindByIdRetrievesIt() {
        Product product = new Product(null, "Laptop", "15-inch laptop", BigDecimal.valueOf(999.99), "Electronics");
        Product savedProduct = new Product(1L, "Laptop", "15-inch laptop", BigDecimal.valueOf(999.99), "Electronics");

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productRepository.findById(1L)).thenReturn(Optional.of(savedProduct));

        Product saved = productService.save(product);

        assertNotNull(saved.id());
        assertEquals("Laptop", saved.name());

        Optional<Product> found = productService.findById(saved.id());

        assertTrue(found.isPresent());
        assertEquals(saved, found.get());
    }

    @Test
    void findById_returnsEmptyOptional_forNonExistentId() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Product> result = productService.findById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteById_removesProduct() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        boolean deleted = productService.deleteById(1L);

        assertTrue(deleted);
        verify(productRepository).deleteById(1L);
    }

    @Test
    void findAll_returnsAllSavedProduct() {
        Product p1 = new Product(1L, "Laptop", "15-inch laptop", BigDecimal.valueOf(999.99), "Electronics");
        Product p2 = new Product(2L, "Mouse", "Wireless mouse", BigDecimal.valueOf(19.99), "Accessories");

        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        List<Product> result = productService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
    }
}