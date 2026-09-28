package com.raya.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {
    @Bean
    @Primary  // Option A: rate limit per IP address
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.justOrEmpty(
                exchange.getRequest().getRemoteAddress()
        ).map(addr -> addr.getAddress().getHostAddress());
    }
    @Bean  // Option B: rate limit per authenticated user
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.justOrEmpty(
                exchange.getRequest().getHeaders().getFirst("X-User-Id")
        ).defaultIfEmpty("anonymous");
    }
}