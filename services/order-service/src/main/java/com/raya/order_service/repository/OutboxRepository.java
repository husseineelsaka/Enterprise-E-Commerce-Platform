package com.raya.order_service.repository;

import com.raya.order_service.model.OutboxEvent;
import org.springframework.stereotype.Repository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class OutboxRepository {

    private final Map<String, OutboxEvent> store = new ConcurrentHashMap<>();

    public OutboxEvent save(OutboxEvent event) {
        store.put(event.getId(), event);
        return event;
    }

    public List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc() {
        return store.values().stream()
                .filter(e -> !e.isPublished())
                .sorted(Comparator.comparing(OutboxEvent::getCreatedAt))
                .collect(Collectors.toList());
    }
}
