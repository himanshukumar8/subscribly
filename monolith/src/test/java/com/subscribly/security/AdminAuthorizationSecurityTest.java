package com.subscribly.security;

import com.subscribly.config.JwtUtil;
import com.subscribly.config.SecurityConfig;
import com.subscribly.controller.AdminController;
import com.subscribly.service.SubscriptionService;
import com.subscribly.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
class AdminAuthorizationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("Unauthenticated request to admin endpoint returns 401 Unauthorized")
    void testUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "ordinary@example.com", roles = "USER")
    @DisplayName("Ordinary user (ROLE_USER) gets 403 Forbidden on GET /api/v1/admin/users")
    void testOrdinaryUserForbiddenOnGetUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ordinary@example.com", roles = "USER")
    @DisplayName("Ordinary user (ROLE_USER) gets 403 Forbidden on GET /api/v1/admin/subscriptions")
    void testOrdinaryUserForbiddenOnGetSubscriptions() throws Exception {
        mockMvc.perform(get("/api/v1/admin/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ordinary@example.com", roles = "USER")
    @DisplayName("Ordinary user (ROLE_USER) gets 403 Forbidden on PUT /api/v1/admin/users/1/role")
    void testOrdinaryUserForbiddenOnPutRole() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/1/role")
                        .param("role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ordinary@example.com", roles = "USER")
    @DisplayName("Ordinary user (ROLE_USER) gets 403 Forbidden on PUT /api/v1/admin/users/1/status")
    void testOrdinaryUserForbiddenOnPutStatus() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/1/status")
                        .param("status", "INACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("Admin user (ROLE_ADMIN) succeeds on GET /api/v1/admin/users")
    void testAdminUserAuthorizedOnGetUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("Admin user (ROLE_ADMIN) succeeds on GET /api/v1/admin/subscriptions")
    void testAdminUserAuthorizedOnGetSubscriptions() throws Exception {
        mockMvc.perform(get("/api/v1/admin/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
