package com.raya.api_gateway.configs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    // TODO 1: Inject a Logger (SLF4J)
    private static final Logger log =
            LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // TODO 2: Implement filter() method
        //         - Log: method + path + remote address (pre-filter)
        //         - Log: response status code (post-filter)
        //         - Chain the filter correctly
        log.info("[REQUEST] {} {} from {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                exchange.getRequest().getRemoteAddress());
        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    log.info("[RESPONSE] Status: {}",
                            exchange.getResponse().getStatusCode());
                }));
    }

    @Override
    public int getOrder() {
        // TODO 3: Return Ordered.HIGHEST_PRECEDENCE
        return Ordered.HIGHEST_PRECEDENCE;
    }
}