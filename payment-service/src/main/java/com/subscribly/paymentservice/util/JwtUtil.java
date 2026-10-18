package com.subscribly.paymentservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtUtil {

    private static final int MIN_KEY_BYTES = 32;

    @Value("${jwt.secret:}")
    private String secretKey;

    @Autowired(required = false)
    private Environment environment;

    private Key cachedSigningKey;

    public JwtUtil() {}

    public JwtUtil(String secretKey) {
        this.secretKey = secretKey;
        initSigningKey();
    }

    @PostConstruct
    public void init() {
        validateConfiguration();
        initSigningKey();
    }

    private void validateConfiguration() {
        boolean isProd = environment != null && environment.acceptsProfiles(Profiles.of("prod"));

        if (secretKey == null || secretKey.trim().isBlank()) {
            if (isProd) {
                throw new IllegalStateException("FATAL: JWT signing secret (jwt.secret / JWT_SECRET) is required in production environment.");
            } else {
                throw new IllegalStateException("JWT signing secret (jwt.secret) must not be blank.");
            }
        }

        byte[] keyBytes = resolveKeyBytes(secretKey.trim());
        if (keyBytes.length < MIN_KEY_BYTES) {
            String msg = "JWT signing key must be at least 256 bits (32 bytes) for HS256. Found " + (keyBytes.length * 8) + " bits.";
            if (isProd) {
                throw new IllegalStateException("FATAL: " + msg);
            } else {
                throw new IllegalStateException(msg);
            }
        }
    }

    private void initSigningKey() {
        if (secretKey != null && !secretKey.trim().isBlank()) {
            byte[] keyBytes = resolveKeyBytes(secretKey.trim());
            this.cachedSigningKey = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    private byte[] resolveKeyBytes(String secret) {
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded != null && decoded.length >= MIN_KEY_BYTES) {
                return decoded;
            }
        } catch (Exception ignored) {
            // Not Base64 encoded; fallback to raw UTF-8 bytes
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private Key getSigningKey() {
        if (cachedSigningKey == null) {
            initSigningKey();
        }
        return cachedSigningKey;
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List<?>) {
                return (List<String>) rolesObj;
            }
        } catch (Exception e) {
            log.warn("[Security] Could not extract roles from JWT: {}", e.getMessage());
        }
        return Collections.singletonList("ROLE_USER");
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token) {
        try {
            return !isExpired(token);
        } catch (Exception e) {
            log.error("[Security] JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    private boolean isExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
