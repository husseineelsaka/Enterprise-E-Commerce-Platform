package com.raya.product_service.controller;

import com.raya.product_service.model.Product;
import com.raya.product_service.projection.ProductSummaryProjection;
import com.raya.product_service.service.ProductCommandService;
import com.raya.product_service.service.ProductQueryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductCommandService commandService;
    private final ProductQueryService queryService;

    public ProductController(ProductCommandService commandService, ProductQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    public List<ProductSummaryProjection> findAll(){
        return queryService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductSummaryProjection> findById(@PathVariable Long id) {
        return queryService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @io.micrometer.core.annotation.Timed(value = "product.create.duration", description = "Time to create a product")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody Product product) {
        return commandService.create(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @Valid @RequestBody Product product) {
        Product toUpdate = new Product(
                id,
                product.name(),
                product.description(),
                product.price(),
                product.category()
        );
        return ResponseEntity.ok(commandService.update(toUpdate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        try {
            commandService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        }
    }
}