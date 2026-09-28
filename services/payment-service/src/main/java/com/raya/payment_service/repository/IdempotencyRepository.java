package com.raya.payment_service.repository;

import com.raya.payment_service.model.IdempotencyRecord;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IdempotencyRepository — Session 22.
 *
 * Thread-safe repository storing idempotency keys for payment deduplication.
 */
@Repository
public class IdempotencyRepository {

    private final Map<String, IdempotencyRecord> store = new ConcurrentHashMap<>();

    public Optional<IdempotencyRecord> findById(String idempotencyKey) {
        return Optional.ofNullable(store.get(idempotencyKey));
    }

    public IdempotencyRecord save(IdempotencyRecord record) {
        store.put(record.getIdempotencyKey(), record);
        return record;
    }

    public void clear() {
        store.clear();
    }
}
