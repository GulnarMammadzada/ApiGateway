package com.example.apigateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class HealthController {

    // Logger-i bu class üçün təyin edin
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    // Standard actuator endpoint
    @GetMapping("/actuator/health")
    public ResponseEntity<?> actuatorHealth() {
        logger.info("Actuator health endpoint accessed");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "api-gateway");
        response.put("port", 8080);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

// Düzəldilmiş JwtAuthFilter.java - isPublicPath metodu
// Bu hissəni JwtAuthFilter.java faylında istifadə edin

    private boolean isPublicPath(String path) {
        List<String> publicPaths = List.of(
                "/",
                "/index.html",
                "/static/",
                "/css/",
                "/js/",
                "/images/",
                "/favicon.ico",
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/refresh",
                "/api/subscriptions/available",
                "/actuator/health",
                "/api/health",
                "/health"
        );

        // Check if path starts with any public path
        boolean isPublic = publicPaths.stream().anyMatch(path::startsWith) || "/".equals(path);

        // Bu logger artıq JwtAuthFilter class-ında təyin edilmişdir
        logger.debug("Path '{}' is public: {}", path, isPublic);
        return isPublic;
    }

    // Alternative API style endpoint
    @GetMapping("/api/health")
    public ResponseEntity<?> apiHealth() {
        logger.info("API health endpoint accessed");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "api-gateway");
        response.put("port", 8080);
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "API Gateway is running");

        return ResponseEntity.ok(response);
    }

    // Root health check
    @GetMapping("/health")
    public ResponseEntity<?> rootHealth() {
        logger.info("Root health endpoint accessed");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "api-gateway");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}