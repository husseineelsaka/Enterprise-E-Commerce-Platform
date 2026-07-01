package com.raya.api_gateway.filter;

import com.raya.api_gateway.config.GatewayProperties;
import com.raya.api_gateway.security.JwtUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain filterChain;

    private JwtAuthFilter createFilter(List<GatewayProperties.Route> routes) {
        return new JwtAuthFilter(jwtUtil, new GatewayProperties(routes));
    }

    @Test
    void getOrder_returnsHighestPrecedencePlusOne() {
        JwtAuthFilter filter = createFilter(List.of());
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
    }

    @Test
    void filter_callsChainFilter_forPublicRoutes_withoutValidatingAnyToken() {
        JwtAuthFilter filter = createFilter(
                List.of(new GatewayProperties.Route("GET", "/api/v1/products"))
        );

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/products").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain, times(1)).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }
}
