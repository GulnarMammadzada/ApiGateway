package com.example.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorsFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CorsFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String origin = request.getHeaders().getFirst(HttpHeaders.ORIGIN);
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        logger.debug("CORS Filter - Method: {}, Path: {}, Origin: {}", method, path, origin);

        // Add CORS headers
        HttpHeaders headers = response.getHeaders();

        // Allow multiple origins if needed
        if (origin != null && (origin.equals("http://localhost:3000") ||
                origin.equals("http://localhost:8080") ||
                origin.equals("http://localhost:4200"))) {
            headers.add("Access-Control-Allow-Origin", origin);
        } else {
            headers.add("Access-Control-Allow-Origin", "http://localhost:3000");
        }

        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        headers.add("Access-Control-Allow-Headers",
                "Origin, Content-Type, Accept, Authorization, X-Requested-With, X-User-Id, X-User-Role");
        headers.add("Access-Control-Allow-Credentials", "true");
        headers.add("Access-Control-Max-Age", "3600");

        // Handle preflight requests
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            logger.info("Handling OPTIONS preflight request for path: {}", path);
            response.setStatusCode(HttpStatus.OK);
            return response.setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100; // Higher priority than other filters
    }
}