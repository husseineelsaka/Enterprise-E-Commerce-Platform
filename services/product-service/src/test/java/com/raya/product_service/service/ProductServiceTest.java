package com.raya.product_service.service;

import com.raya.product_service.model.Product;
import com.raya.product_service.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
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

    // ── Lab 9A · Part 1: @ParameterizedTest ──────────────────────────────
    // One method, many inputs. A new case is one more CSV row — no copy-paste,
    // and a failure names the exact input that broke.

    @ParameterizedTest(name = "id {0} -> \"{1}\"")
    @CsvSource({
            "1, Laptop,  Electronics",
            "2, Mouse,   Accessories",
            "3, Monitor, Electronics"
    })
    @DisplayName("findById returns the product the repository holds for each id")
    void findById_returnsMatchingProduct(Long id, String name, String category) {
        Product stored = new Product(id, name, name + " description", BigDecimal.valueOf(99.99), category);
        when(productRepository.findById(id)).thenReturn(Optional.of(stored));

        Optional<Product> result = productService.findById(id);

        assertTrue(result.isPresent());
        assertEquals(name, result.get().name());
        assertEquals(category, result.get().category());
    }

    @ParameterizedTest(name = "missing id {0} -> empty Optional")
    @CsvSource({"404", "999", "123456"})
    @DisplayName("findById returns an empty Optional for any id the repository does not hold")
    void findById_returnsEmpty_forUnknownIds(Long missingId) {
        when(productRepository.findById(missingId)).thenReturn(Optional.empty());

        assertTrue(productService.findById(missingId).isEmpty());
    }

    // ── Lab 9A · Part 1: ArgumentCaptor ──────────────────────────────────
    // verify(repo).save(product) only proves save() was called. ArgumentCaptor
    // proves WHAT was passed — the field values as they reached the repository.

    @Test
    @DisplayName("save passes the product through to the repository with its fields intact")
    void save_passesProductToRepository_withFieldsIntact() {
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        Product newProduct = new Product("Laptop", "15-inch laptop", new BigDecimal("999.99"), "Electronics");
        when(productRepository.save(any(Product.class)))
                .thenReturn(new Product(1L, "Laptop", "15-inch laptop", new BigDecimal("999.99"), "Electronics"));

        productService.save(newProduct);

        // capture() must sit INSIDE verify(); getValue() is read after.
        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();

        assertNull(saved.id(), "a newly created product must reach the repository without an id");
        assertEquals("Laptop", saved.name());
        assertEquals("15-inch laptop", saved.description());
        assertEquals(new BigDecimal("999.99"), saved.price());
        assertEquals("Electronics", saved.category());
    }

    @Test
    @DisplayName("update forces the caller-supplied id onto the entity that is saved")
    void update_savesEntityCarryingTheGivenId() {
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        Product updated = new Product(7L, "Laptop Pro", "16-inch laptop", new BigDecimal("1299.00"), "Electronics");
        when(productRepository.save(any(Product.class))).thenReturn(updated);

        productService.update(updated);

        verify(productRepository).save(productCaptor.capture());
        assertEquals(7L, productCaptor.getValue().id());
        assertEquals("Laptop Pro", productCaptor.getValue().name());
    }
}