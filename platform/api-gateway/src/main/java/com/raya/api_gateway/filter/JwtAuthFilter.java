package com.raya.api_gateway.filter;

import com.raya.api_gateway.config.GatewayProperties;
import com.raya.api_gateway.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<PublicRoute> publicRoutes;

    private record PublicRoute(HttpMethod method, String pathPattern) {}

    public JwtAuthFilter(JwtUtil jwtUtil, GatewayProperties gatewayProperties) {
        this.jwtUtil = jwtUtil;
        this.publicRoutes = gatewayProperties.publicRoutes().stream()
                .map(r -> new PublicRoute(HttpMethod.valueOf(r.method()), r.path()))
                .toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicRoute(method, path))
            return chain.filter(exchange);

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange, "Missing token");
        }

        try {
            Claims claims = jwtUtil.validateToken(authHeader.substring(7));
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            if (role == null || role.isBlank()) {
                return unauthorizedResponse(exchange, "Token does not contain role claim");
            }
            ServerHttpRequest enriched = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Role");
                        headers.add("X-User-Id", userId);
                        headers.add("X-User-Role", role);
                    }).build();
            return chain.filter(exchange.mutate().request(enriched).build());
        } catch (ExpiredJwtException e) {
            return unauthorizedResponse(exchange, "Token expired: " + e.getMessage());
        } catch (MalformedJwtException e) {
            return unauthorizedResponse(exchange, "Malformed token: " + e.getMessage());
        } catch (SignatureException e) {
            return unauthorizedResponse(exchange, "Invalid signature: " + e.getMessage());
        } catch (JwtException e) {
            return unauthorizedResponse(exchange, "Invalid token: " + e.getMessage());
        }
    }

    private boolean isPublicRoute(HttpMethod method, String path) {
        return publicRoutes.stream().anyMatch(route ->
                route.method().equals(method)
                        && pathMatcher.match(route.pathPattern(), path)
        );
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        String body = """
                {
                  "status": 401,
                  "error": "Unauthorized",
                  "message": "%s"
                }
                """.formatted(message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
