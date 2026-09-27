package com.raya.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.List;

@Component
public class JwtHeaderEnrichmentFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(jwt -> {
                    String userId = jwt.getSubject();
                    String role = extractRole(jwt);
                    return exchange.getRequest().mutate()
                            .headers(headers -> {
                                headers.remove("X-User-Id");
                                headers.remove("X-User-Role");
                                if (userId != null) headers.add("X-User-Id", userId);
                                if (role != null) headers.add("X-User-Role", role);
                            }).build();
                })
                .defaultIfEmpty(exchange.getRequest())
                .flatMap(req -> chain.filter(exchange.mutate().request(req).build()));
    }

    @SuppressWarnings("unchecked")
    private String extractRole(Jwt jwt) {
        if (jwt.hasClaim("realm_access")) {
            java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                List<String> roles = (List<String>) realmAccess.get("roles");
                if (roles != null && !roles.isEmpty()) {
                    return roles.get(0);
                }
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
