package com.raya.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("gateway")
public record GatewayProperties(List<Route> publicRoutes) {
    public record Route(String method, String path) {}
}
