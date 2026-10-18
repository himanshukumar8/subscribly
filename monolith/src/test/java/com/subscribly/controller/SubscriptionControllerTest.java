package com.subscribly.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscribly.dto.SubscriptionRequest;
import com.subscribly.dto.SubscriptionResponse;
import com.subscribly.dto.SubscriptionValidationDto;
import com.subscribly.entity.BillingCycle;
import com.subscribly.entity.SubscriptionStatus;
import com.subscribly.service.SubscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @Test
    @DisplayName("GET /api/v1/subscription/{id}/validate returns 200 for authenticated subscription owner")
    void testValidateSubscriptionEndpoint() throws Exception {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(3600);

        SubscriptionValidationDto dto = SubscriptionValidationDto.builder()
                .subscriptionId(1L)
                .userEmail("alice@example.com")
                .plan("PRO")
                .price(BigDecimal.valueOf(29.99))
                .billingCycle(BillingCycle.MONTHLY)
                .persistedStatus(SubscriptionStatus.ACTIVE)
                .effectiveStatus(SubscriptionStatus.ACTIVE)
                .active(true)
                .expiresAt(expiry)
                .remainingSeconds(3600)
                .autoRenew(true)
                .build();

        when(subscriptionService.validateSubscription(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/subscription/1/validate")
                        .principal(() -> "alice@example.com")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptionId").value(1))
                .andExpect(jsonPath("$.userEmail").value("alice@example.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.effectiveStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.plan").value("PRO"))
                .andExpect(jsonPath("$.autoRenew").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/subscription/{id}/validate returns 401 when unauthenticated")
    void testValidateSubscriptionUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/subscription/1/validate")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/subscription/{id}/validate returns 403 when accessing another user's subscription")
    void testValidateSubscriptionCrossUserReturns403() throws Exception {
        SubscriptionValidationDto dto = SubscriptionValidationDto.builder()
                .subscriptionId(1L)
                .userEmail("alice@example.com")
                .active(true)
                .build();

        when(subscriptionService.validateSubscription(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/subscription/1/validate")
                        .principal(() -> "mallory@example.com")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/subscription/{id}/validate returns 200 for admin accessing any subscription")
    void testValidateSubscriptionAdminAuthorizedReturns200() throws Exception {
        SubscriptionValidationDto dto = SubscriptionValidationDto.builder()
                .subscriptionId(1L)
                .userEmail("alice@example.com")
                .plan("PRO")
                .active(true)
                .build();

        when(subscriptionService.validateSubscription(1L)).thenReturn(dto);

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken adminAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "admin@example.com", null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
                );

        mockMvc.perform(get("/api/v1/subscription/1/validate")
                        .principal(adminAuth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value("alice@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/subscription/create returns 200 and created SubscriptionResponse")
    void testCreateSubscriptionEndpoint() throws Exception {
        SubscriptionRequest request = SubscriptionRequest.builder()
                .plan("ENTERPRISE")
                .price(BigDecimal.valueOf(99.0))
                .billingCycle(BillingCycle.YEARLY)
                .autoRenew(true)
                .email("bob@example.com")
                .build();

        SubscriptionResponse response = SubscriptionResponse.builder()
                .id(5L)
                .plan("ENTERPRISE")
                .price(BigDecimal.valueOf(99.0))
                .billingCycle(BillingCycle.YEARLY)
                .status(SubscriptionStatus.ACTIVE)
                .effectiveStatus(SubscriptionStatus.ACTIVE)
                .active(true)
                .autoRenew(true)
                .build();

        when(subscriptionService.createSubscription(eq("bob@example.com"), any(SubscriptionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/subscription/create")
                        .principal(() -> "bob@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.plan").value("ENTERPRISE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/subscription/create returns 401 Unauthorized when unauthenticated")
    void testCreateSubscriptionUnauthenticatedReturns401() throws Exception {
        SubscriptionRequest request = SubscriptionRequest.builder()
                .plan("ENTERPRISE")
                .price(BigDecimal.valueOf(99.0))
                .email("bob@example.com")
                .build();

        mockMvc.perform(post("/api/v1/subscription/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/subscription/create returns 403 Forbidden when email in body does not match authenticated user")
    void testCreateSubscriptionMismatchedEmailReturns403() throws Exception {
        SubscriptionRequest request = SubscriptionRequest.builder()
                .plan("ENTERPRISE")
                .price(BigDecimal.valueOf(99.0))
                .email("victim@example.com")
                .build();

        mockMvc.perform(post("/api/v1/subscription/create")
                        .principal(() -> "attacker@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/subscription/my returns user subscriptions for authenticated principal")
    void testGetMySubscriptionsEndpoint() throws Exception {
        SubscriptionResponse res = SubscriptionResponse.builder()
                .id(1L)
                .plan("BASIC")
                .price(BigDecimal.valueOf(9.99))
                .billingCycle(BillingCycle.MONTHLY)
                .status(SubscriptionStatus.ACTIVE)
                .effectiveStatus(SubscriptionStatus.ACTIVE)
                .active(true)
                .build();

        when(subscriptionService.getSubscriptionsByUser("charlie@example.com"))
                .thenReturn(List.of(res));

        mockMvc.perform(get("/api/v1/subscription/my")
                        .principal(() -> "charlie@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].plan").value("BASIC"));
    }

    @Test
    @DisplayName("GET /api/v1/subscription/my returns 401 Unauthorized when unauthenticated")
    void testGetMySubscriptionsUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/subscription/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/subscription/my returns 403 Forbidden when email param does not match authenticated principal")
    void testGetMySubscriptionsMismatchedEmailReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/subscription/my")
                        .principal(() -> "charlie@example.com")
                        .param("email", "victim@example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/subscription/{id}/cancel cancels subscription for authenticated owner")
    void testCancelSubscriptionEndpoint() throws Exception {
        SubscriptionResponse res = SubscriptionResponse.builder()
                .id(1L)
                .status(SubscriptionStatus.CANCELLED)
                .effectiveStatus(SubscriptionStatus.CANCELLED)
                .active(false)
                .build();

        when(subscriptionService.cancelSubscription(1L, "charlie@example.com")).thenReturn(res);

        mockMvc.perform(put("/api/v1/subscription/1/cancel")
                        .principal(() -> "charlie@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.active").value(false));
    }
}
