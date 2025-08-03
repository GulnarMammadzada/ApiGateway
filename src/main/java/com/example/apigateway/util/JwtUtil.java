package com.example.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String extractUsername(String token) {
        try {
            return extractClaims(token).getSubject();
        } catch (Exception e) {
            logger.error("Error extracting username from token", e);
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            Claims claims = extractClaims(token);
            String role = claims.get("role", String.class);
            return role != null ? role : "USER"; // Default to USER if role is not present
        } catch (Exception e) {
            logger.error("Error extracting role from token", e);
            return "USER";
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            logger.error("Error checking token expiration", e);
            return true;
        }
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                logger.warn("Token is null or empty");
                return false;
            }

            Claims claims = extractClaims(token);

            // Check if token is expired
            if (isTokenExpired(token)) {
                logger.warn("Token is expired");
                return false;
            }

            // Check if username exists
            String username = claims.getSubject();
            if (username == null || username.trim().isEmpty()) {
                logger.warn("Token does not contain valid username");
                return false;
            }

            logger.debug("Token validated successfully for user: {}", username);
            return true;

        } catch (ExpiredJwtException e) {
            logger.warn("Token expired: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            logger.warn("Malformed token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            logger.warn("Invalid token signature: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Token validation failed", e);
            return false;
        }
    }
}