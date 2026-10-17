package com.subscribly.paymentservice.util;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String VALID_TEST_KEY_RAW = "my-test-secret-key-that-is-at-least-32-bytes-long!";
    private static final String WEAK_KEY = "too-short";

    @Test
    @DisplayName("Valid key initializes and parses tokens correctly")
    void testValidKeyInitializes() {
        JwtUtil jwtUtil = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", VALID_TEST_KEY_RAW);
        ReflectionTestUtils.setField(jwtUtil, "environment", env);
        jwtUtil.init();

        Key key = Keys.hmacShaKeyFor(VALID_TEST_KEY_RAW.getBytes());
        String token = io.jsonwebtoken.Jwts.builder()
                .setSubject("payer@example.com")
                .claim("roles", List.of("ROLE_USER"))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("payer@example.com", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.extractRoles(token).contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Weak key fails fast on initialization")
    void testWeakKeyThrows() {
        JwtUtil jwtUtil = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", WEAK_KEY);
        ReflectionTestUtils.setField(jwtUtil, "environment", env);

        assertThrows(IllegalStateException.class, jwtUtil::init);
    }

    @Test
    @DisplayName("Expired JWT is rejected")
    void testExpiredJwtRejected() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", VALID_TEST_KEY_RAW);
        jwtUtil.init();

        Key key = Keys.hmacShaKeyFor(VALID_TEST_KEY_RAW.getBytes());
        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .setSubject("user@example.com")
                .setIssuedAt(new Date(System.currentTimeMillis() - 100000))
                .setExpiration(new Date(System.currentTimeMillis() - 50000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.isTokenValid(expiredToken));
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.extractUsername(expiredToken));
    }

    @Test
    @DisplayName("Malformed JWT is rejected")
    void testMalformedJwtRejected() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", VALID_TEST_KEY_RAW);
        jwtUtil.init();

        assertFalse(jwtUtil.isTokenValid("invalid.jwt.token"));
    }

    @Test
    @DisplayName("Incorrectly signed JWT is rejected")
    void testIncorrectlySignedJwtRejected() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", VALID_TEST_KEY_RAW);
        jwtUtil.init();

        Key wrongKey = Keys.hmacShaKeyFor("another-completely-different-key-32b!".getBytes());
        String wrongToken = io.jsonwebtoken.Jwts.builder()
                .setSubject("user@example.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(wrongKey, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.isTokenValid(wrongToken));
        assertThrows(SignatureException.class, () -> jwtUtil.extractUsername(wrongToken));
    }

    @Test
    @DisplayName("Tampered JWT payload is rejected")
    void testTamperedJwtRejected() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", VALID_TEST_KEY_RAW);
        jwtUtil.init();

        Key key = Keys.hmacShaKeyFor(VALID_TEST_KEY_RAW.getBytes());
        String validToken = io.jsonwebtoken.Jwts.builder()
                .setSubject("user@example.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String[] parts = validToken.split("\\.");
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"attacker@example.com\"}".getBytes());
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertFalse(jwtUtil.isTokenValid(tamperedToken));
    }
}
