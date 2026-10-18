package com.subscribly.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String VALID_TEST_KEY_RAW = "my-test-secret-key-that-is-at-least-32-bytes-long!";
    private static final String VALID_TEST_KEY_BASE64 = Base64.getEncoder().encodeToString(
            "my-test-secret-key-that-is-at-least-32-bytes-long!".getBytes()
    );
    private static final String WEAK_KEY = "too-short";

    @Test
    @DisplayName("Valid 256-bit raw UTF-8 key initializes and signs/validates tokens")
    void testValidRawKeyInitializes() {
        JwtUtil jwtUtil = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", VALID_TEST_KEY_RAW);
        ReflectionTestUtils.setField(jwtUtil, "environment", env);
        jwtUtil.init();

        String token = jwtUtil.generateToken("user@example.com", List.of("ROLE_USER"));
        assertNotNull(token);
        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("user@example.com", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.extractRoles(token).contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Valid 256-bit Base64 key initializes and signs/validates tokens")
    void testValidBase64KeyInitializes() {
        JwtUtil jwtUtil = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", VALID_TEST_KEY_BASE64);
        ReflectionTestUtils.setField(jwtUtil, "environment", env);
        jwtUtil.init();

        String token = jwtUtil.generateToken("admin@example.com", List.of("ROLE_ADMIN"));
        assertNotNull(token);
        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("admin@example.com", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.extractRoles(token).contains("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Weak key (< 256 bits) fails fast on initialization")
    void testWeakKeyThrowsException() {
        JwtUtil jwtUtil = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", WEAK_KEY);
        ReflectionTestUtils.setField(jwtUtil, "environment", env);

        IllegalStateException ex = assertThrows(IllegalStateException.class, jwtUtil::init);
        assertTrue(ex.getMessage().contains("at least 256 bits"));
    }

    @Test
    @DisplayName("Missing key in production profile fails startup immediately")
    void testMissingKeyInProductionFailsStartup() {
        JwtUtil jwtUtil = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        ReflectionTestUtils.setField(jwtUtil, "secretKey", "");
        ReflectionTestUtils.setField(jwtUtil, "environment", env);

        IllegalStateException ex = assertThrows(IllegalStateException.class, jwtUtil::init);
        assertTrue(ex.getMessage().contains("FATAL") && ex.getMessage().contains("required in production"));
    }

    @Test
    @DisplayName("Weak key in production profile fails startup immediately")
    void testWeakKeyInProductionFailsStartup() {
        JwtUtil jwtUtil = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        ReflectionTestUtils.setField(jwtUtil, "secretKey", "short-prod-key");
        ReflectionTestUtils.setField(jwtUtil, "environment", env);

        IllegalStateException ex = assertThrows(IllegalStateException.class, jwtUtil::init);
        assertTrue(ex.getMessage().contains("FATAL") && ex.getMessage().contains("at least 256 bits"));
    }

    @Test
    @DisplayName("Blank key in dev mode fails startup immediately without weak fallback")
    void testBlankKeyInDevThrows() {
        JwtUtil jwtUtil = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", "   ");
        ReflectionTestUtils.setField(jwtUtil, "environment", env);

        IllegalStateException ex = assertThrows(IllegalStateException.class, jwtUtil::init);
        assertTrue(ex.getMessage().contains("must not be blank"));
    }

    @Test
    @DisplayName("Expired JWT is rejected by isTokenValid")
    void testExpiredJwtIsRejected() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", VALID_TEST_KEY_RAW);
        jwtUtil.init();

        java.security.Key key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(VALID_TEST_KEY_RAW.getBytes());
        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .setSubject("user@example.com")
                .setIssuedAt(new java.util.Date(System.currentTimeMillis() - 100000))
                .setExpiration(new java.util.Date(System.currentTimeMillis() - 50000))
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.isTokenValid(expiredToken));
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> jwtUtil.extractUsername(expiredToken));
    }

    @Test
    @DisplayName("Malformed JWT is rejected by isTokenValid")
    void testMalformedJwtIsRejected() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", VALID_TEST_KEY_RAW);
        jwtUtil.init();

        assertFalse(jwtUtil.isTokenValid("not.a.valid.jwt"));
        assertThrows(io.jsonwebtoken.JwtException.class, () -> jwtUtil.extractUsername("malformed-token"));
    }

    @Test
    @DisplayName("Incorrectly signed JWT (signed with another key) is rejected")
    void testIncorrectlySignedJwtIsRejected() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", VALID_TEST_KEY_RAW);
        jwtUtil.init();

        java.security.Key anotherKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "another-completely-different-key-32b!".getBytes()
        );
        String invalidSignToken = io.jsonwebtoken.Jwts.builder()
                .setSubject("user@example.com")
                .setIssuedAt(new java.util.Date())
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60000))
                .signWith(anotherKey, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.isTokenValid(invalidSignToken));
        assertThrows(io.jsonwebtoken.security.SignatureException.class, () -> jwtUtil.extractUsername(invalidSignToken));
    }

    @Test
    @DisplayName("Tampered JWT payload is rejected by isTokenValid")
    void testTamperedJwtIsRejected() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", VALID_TEST_KEY_RAW);
        jwtUtil.init();

        String validToken = jwtUtil.generateToken("user@example.com", List.of("ROLE_USER"));
        String[] parts = validToken.split("\\.");
        assertEquals(3, parts.length);

        // Tamper with the payload segment
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"attacker@example.com\"}".getBytes());
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertFalse(jwtUtil.isTokenValid(tamperedToken));
        assertThrows(io.jsonwebtoken.JwtException.class, () -> jwtUtil.extractUsername(tamperedToken));
    }
}
