package com.raya.product_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching  // REQUIRED — activates the Spring Cache abstraction
public class ProductServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductServiceApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(io.micrometer.core.instrument.MeterRegistry.class)
	public io.micrometer.core.aop.TimedAspect timedAspect(io.micrometer.core.instrument.MeterRegistry registry) {
		return new io.micrometer.core.aop.TimedAspect(registry);
	}
}
