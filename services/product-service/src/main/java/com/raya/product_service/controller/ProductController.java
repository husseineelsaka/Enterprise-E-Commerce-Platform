package com.raya.product_service.controller;

import com.raya.product_service.model.Product;
import com.raya.product_service.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    // TODO: Inject ProductService
    private final ProductService productService;


    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // TODO: GET /api/v1/products         → return all products
    @GetMapping
    public List<Product> findAll(){
        return  productService.findAll();
    }

    // TODO: GET /api/v1/products/{id}    → return product by id (404 if not found)

    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    // TODO: POST /api/v1/products        → create a new product
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody Product product) {
        return productService.save(product);
    }

    // TODO: PUT /api/v1/products/{id}     → update an existing product (404 if not found)
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @Valid @RequestBody Product product) {
        Product toUpdate = new Product(
                id,
                product.name(),
                product.description(),
                product.price(),
                product.category()
        );
        return ResponseEntity.ok(productService.update(toUpdate));
    }

    // TODO: DELETE /api/v1/products/{id} → delete a product

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        boolean deleted = productService.deleteById(id);
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}