package com.subscribly.controller;

import com.subscribly.dto.UserResponse;
import com.subscribly.entity.User;
import com.subscribly.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/v1/user/me: returns logged in user profile")
    void testGetLoggedUserSuccess() throws Exception {
        User user = User.builder().id(1L).email("user@example.com").fullName("Test User").active(true).build();
        UserResponse response = UserResponse.builder().id(1L).email("user@example.com").fullName("Test User").roles(List.of("ROLE_USER")).active(true).build();

        Principal principal = () -> "user@example.com";
        when(userService.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userService.toResponse(user)).thenReturn(response);

        mockMvc.perform(get("/api/v1/user/me")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"));
    }

    @Test
    @DisplayName("GET /api/user/me: supports legacy route alias")
    void testGetLoggedUserLegacyRoute() throws Exception {
        User user = User.builder().id(1L).email("user@example.com").fullName("Test User").active(true).build();
        UserResponse response = UserResponse.builder().id(1L).email("user@example.com").fullName("Test User").roles(List.of("ROLE_USER")).active(true).build();

        Principal principal = () -> "user@example.com";
        when(userService.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userService.toResponse(user)).thenReturn(response);

        mockMvc.perform(get("/api/user/me")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/user/me: returns 401 when unauthenticated")
    void testGetLoggedUserUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/user/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/user/me: returns 404 when user not found in database")
    void testGetLoggedUserNotFound() throws Exception {
        Principal principal = () -> "ghost@example.com";
        when(userService.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/user/me")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
