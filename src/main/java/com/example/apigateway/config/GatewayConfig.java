package com.example.apigateway.config;

import com.example.apigateway.filter.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, JwtAuthFilter jwtAuthFilter) {
        return builder.routes()
                // Static resources (Frontend) - Public access
                .route("static-resources", r -> r
                        .path("/", "/index.html", "/static/**", "/css/**", "/js/**", "/images/**", "/favicon.ico")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("forward:/"))

                // Health check endpoints - Direct to local controller
                .route("health-check-actuator", r -> r
                        .path("/actuator/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("forward:/"))

                // Alternative health endpoint for API style access
                .route("health-check-api", r -> r
                        .path("/api/health")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("forward:/actuator/health"))

                // User Service - Auth endpoints (public)
                .route("user-auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("http://localhost:8081"))

                // User Service - Protected endpoints
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("http://localhost:8081"))

                // User Service - Admin endpoints
                .route("user-admin-service", r -> r
                        .path("/api/admin/users/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("http://localhost:8081"))

                // Subscription Service - Public endpoints
                .route("subscription-public-service", r -> r
                        .path("/api/subscriptions/available/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("http://localhost:8082"))

                // Subscription Service - Admin endpoints
                .route("subscription-admin-service", r -> r
                        .path("/api/subscriptions/admin/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("http://localhost:8082"))

                // User Subscription Service - All endpoints
                .route("user-subscription-service", r -> r
                        .path("/api/user-subscriptions/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("http://localhost:8083"))

                // Email Service
                .route("email-service", r -> r
                        .path("/api/email/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("http://localhost:8084"))

                .build();
    }
}
