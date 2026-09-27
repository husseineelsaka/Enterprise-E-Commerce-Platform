package com.raya.order_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@org.springframework.scheduling.annotation.EnableScheduling
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(io.micrometer.core.instrument.MeterRegistry.class)
	public io.micrometer.core.aop.TimedAspect timedAspect(io.micrometer.core.instrument.MeterRegistry registry) {
		return new io.micrometer.core.aop.TimedAspect(registry);
	}
}
