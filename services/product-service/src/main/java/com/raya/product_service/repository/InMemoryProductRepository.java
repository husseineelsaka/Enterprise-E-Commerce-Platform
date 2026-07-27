package com.raya.product_service.repository;

import com.raya.product_service.model.Product;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public List<Product> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Product save(Product product) {
        Long id = product.id() != null ? product.id() : idGenerator.incrementAndGet();

        Product toPersist = new Product(
                id,
                product.name(),
                product.description(),
                product.price(),
                product.category()
        );

        store.put(id, toPersist);
        return toPersist;
    }

    @Override
    public boolean deleteById(Long id) {
        return store.remove(id) != null;
    }
}
