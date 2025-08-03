package com.example.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    // Optional: Simple fallback route for testing
    @Bean
    public RouteLocator fallbackRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("fallback", r -> r
                        .path("/fallback")
                        .uri("forward:/actuator/health"))
                .build();
    }
}