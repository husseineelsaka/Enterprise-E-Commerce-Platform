package com.raya.product_service.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

/**
 * ProductServiceApplication carries @EnableCaching, and that annotation is
 * processed by every test that boots a Spring context — including the slices.
 * A slice loads no Redis auto-configuration, so without this the context fails
 * with "No qualifying bean of type CacheManager".
 *
 * An in-memory ConcurrentMapCacheManager satisfies the requirement without
 * dragging Redis into a test that is not about caching.
 */
@TestConfiguration
public class TestCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("products");
    }
}
