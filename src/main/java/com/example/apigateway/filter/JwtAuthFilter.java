package com.example.apigateway.filter;

import com.example.apigateway.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            String method = request.getMethod().name();

            logger.info("=== REQUEST START ===");
            logger.info("Processing request: {} {}", method, path);
            logger.info("Request Headers: {}", request.getHeaders());
            logger.info("Query Params: {}", request.getQueryParams());

            // Public paths - no authentication needed (including static resources)
            if (isPublicPath(path)) {
                logger.info("Public path accessed: {}", path);

                // Continue with chain and log response
                return chain.filter(exchange)
                        .doOnSuccess(aVoid -> {
                            ServerHttpResponse response = exchange.getResponse();
                            logger.info("=== RESPONSE SUCCESS ===");
                            logger.info("Response Status: {}", response.getStatusCode());
                            logger.info("Response Headers: {}", response.getHeaders());
                            logger.info("=== REQUEST END ===");
                        })
                        .doOnError(throwable -> {
                            logger.error("=== RESPONSE ERROR ===");
                            logger.error("Error processing request: ", throwable);
                            logger.info("=== REQUEST END ===");
                        });
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            logger.info("Authorization header present: {}", authHeader != null);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Missing or invalid Authorization header for path: {}", path);
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                if (!jwtUtil.validateToken(token)) {
                    logger.warn("Invalid token for path: {}", path);
                    return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                }

                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                logger.info("Authenticated user: {} with role: {} accessing: {}", username, role, path);

                // Check admin access
                if (isAdminPath(path) && !"ADMIN".equals(role)) {
                    logger.warn("Non-admin user {} attempted to access admin path: {}", username, path);
                    return onError(exchange, "Admin access required", HttpStatus.FORBIDDEN);
                }

                // Add user info to headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", username)
                        .header("X-User-Role", role)
                        .build();

                logger.info("Request forwarded for user: {}", username);

                return chain.filter(exchange.mutate().request(modifiedRequest).build())
                        .doOnSuccess(aVoid -> {
                            ServerHttpResponse response = exchange.getResponse();
                            logger.info("=== RESPONSE SUCCESS ===");
                            logger.info("Response Status: {}", response.getStatusCode());
                            logger.info("Response Headers: {}", response.getHeaders());
                            logger.info("=== REQUEST END ===");
                        })
                        .doOnError(throwable -> {
                            logger.error("=== RESPONSE ERROR ===");
                            logger.error("Error processing authenticated request: ", throwable);
                            logger.info("=== REQUEST END ===");
                        });

            } catch (Exception e) {
                logger.error("Error processing JWT token for path: {}", path, e);
                return onError(exchange, "Token validation failed", HttpStatus.UNAUTHORIZED);
            }
        };
    }

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
                "/actuator/health"
        );

        // Check if path starts with any public path
        boolean isPublic = publicPaths.stream().anyMatch(path::startsWith) || "/".equals(path);
        logger.debug("Path '{}' is public: {}", path, isPublic);
        return isPublic;
    }

    private boolean isAdminPath(String path) {
        return path.contains("/admin/");
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();

        logger.error("=== ERROR RESPONSE ===");
        logger.error("Status: {}, Message: {}", status, message);

        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().add("Access-Control-Allow-Origin", "http://localhost:8080");
        response.getHeaders().add("Access-Control-Allow-Credentials", "true");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("status", status.value());

        try {
            String json = objectMapper.writeValueAsString(errorResponse);
            logger.error("Error response JSON: {}", json);
            DataBuffer buffer = response.bufferFactory().wrap(json.getBytes());

            return response.writeWith(Mono.just(buffer))
                    .doFinally(signalType -> {
                        logger.error("=== REQUEST END (ERROR) ===");
                    });
        } catch (Exception e) {
            logger.error("Error creating error response", e);
            return response.setComplete();
        }
    }

    public static class Config {
        // Empty config class for configuration if needed
    }
}