package com.subscribly.controller;

import com.subscribly.dto.SubscriptionResponse;
import com.subscribly.dto.UserResponse;
import com.subscribly.entity.SubscriptionStatus;
import com.subscribly.service.SubscriptionService;
import com.subscribly.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/v1/admin/subscriptions: returns all subscriptions")
    void testGetAllSubscriptions() throws Exception {
        SubscriptionResponse sub = SubscriptionResponse.builder()
                .id(1L)
                .plan("PRO")
                .status(SubscriptionStatus.ACTIVE)
                .price(BigDecimal.valueOf(29.99))
                .build();
        when(subscriptionService.getAllSubscriptions()).thenReturn(List.of(sub));

        mockMvc.perform(get("/api/v1/admin/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].plan").value("PRO"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users: returns all users without password hash")
    void testGetAllUsers() throws Exception {
        UserResponse user = UserResponse.builder()
                .id(1L)
                .email("admin@example.com")
                .fullName("Admin User")
                .roles(List.of("ROLE_ADMIN"))
                .active(true)
                .build();
        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].email").value("admin@example.com"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/users/{id}/role: updates user role")
    void testUpdateUserRole() throws Exception {
        UserResponse user = UserResponse.builder()
                .id(1L)
                .email("user@example.com")
                .roles(List.of("ROLE_ADMIN"))
                .active(true)
                .build();
        when(userService.updateUserRole(eq(1L), eq("ROLE_ADMIN"), any())).thenReturn(user);

        mockMvc.perform(put("/api/v1/admin/users/1/role")
                        .param("role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/users/{id}/role: returns 404 when user not found")
    void testUpdateUserRoleNotFound() throws Exception {
        when(userService.updateUserRole(eq(999L), eq("ROLE_ADMIN"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: 999"));

        mockMvc.perform(put("/api/v1/admin/users/999/role")
                        .param("role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/users/{id}/role: returns 400 when role is invalid")
    void testUpdateUserRoleInvalidRole() throws Exception {
        when(userService.updateUserRole(eq(1L), eq("INVALID_ROLE"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: INVALID_ROLE"));

        mockMvc.perform(put("/api/v1/admin/users/1/role")
                        .param("role", "INVALID_ROLE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/users/{id}/role: returns 409 when demoting last administrator")
    void testUpdateUserRoleLastAdminDemotionBlocked() throws Exception {
        when(userService.updateUserRole(eq(1L), eq("ROLE_USER"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cannot demote the last remaining active administrator"));

        mockMvc.perform(put("/api/v1/admin/users/1/role")
                        .param("role", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/users/{id}/status: updates user status")
    void testUpdateUserStatus() throws Exception {
        UserResponse user = UserResponse.builder()
                .id(1L)
                .email("user@example.com")
                .active(false)
                .build();
        when(userService.updateUserStatus(eq(1L), eq("INACTIVE"), any())).thenReturn(user);

        mockMvc.perform(put("/api/v1/admin/users/1/status")
                        .param("status", "INACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/users/{id}/status: returns 400 when status is invalid")
    void testUpdateUserStatusInvalid() throws Exception {
        when(userService.updateUserStatus(eq(1L), eq("BANANA"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: BANANA"));

        mockMvc.perform(put("/api/v1/admin/users/1/status")
                        .param("status", "BANANA")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/users/{id}/status: returns 409 when deactivating last administrator")
    void testUpdateUserStatusLastAdminDeactivationBlocked() throws Exception {
        when(userService.updateUserStatus(eq(1L), eq("INACTIVE"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cannot deactivate the last remaining active administrator"));

        mockMvc.perform(put("/api/v1/admin/users/1/status")
                        .param("status", "INACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }
}
