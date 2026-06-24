package com.raya.product_service.service;

import com.raya.product_service.ProductServiceApplication;
import com.raya.product_service.models.Product;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductService {

    // TODO 1: Inject a Map<Long, Product> as an in-memory store (no DB yet)
    private final Map<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);


    // TODO 2: Implement findAll() returning List<Product>
    public List<Product> findAll() {
        return List.copyOf(store.values());
    }
    // TODO 3: Implement findById(Long id) returning Optional<Product>
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }
    // TODO 4: Implement save(Product product) returning the saved Product
    public Product save(Product product){
        Long id = product.id() != null ? product.id() : idGenerator.incrementAndGet();

        Product toSave = new Product(
                id,
                product.name(),
                product.description(),
                product.price(),
                product.category()
        );

        store.put(id, toSave);
        return toSave;
    }
    // TODO 5: Implement deleteById(Long id)
    public boolean deleteById(Long id){
        return store.remove(id) != null;
    }
}