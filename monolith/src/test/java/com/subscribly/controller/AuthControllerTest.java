package com.subscribly.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscribly.config.JwtUtil;
import com.subscribly.dto.LoginRequest;
import com.subscribly.dto.RegisterRequest;
import com.subscribly.entity.Role;
import com.subscribly.entity.User;
import com.subscribly.repository.RoleRepository;
import com.subscribly.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("POST /api/v1/auth/register succeeds with valid payload")
    void testRegisterSuccess() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Alice Wonder")
                .email("alice@example.com")
                .password("secret123")
                .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("encodedSecret");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(new Role(1L, "ROLE_USER")));

        User savedUser = User.builder()
                .id(1L)
                .fullName("Alice Wonder")
                .email("alice@example.com")
                .password("encodedSecret")
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken("alice@example.com")).thenReturn("mock.jwt.token");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.token").value("mock.jwt.token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register rejects duplicate email with 400 Bad Request")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("Alice Wonder")
                .email("existing@example.com")
                .password("secret123")
                .build();

        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new User()));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Email already in use: existing@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login succeeds with correct credentials")
    void testLoginSuccess() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("alice@example.com")
                .password("secret123")
                .build();

        User existing = User.builder()
                .id(1L)
                .fullName("Alice Wonder")
                .email("alice@example.com")
                .password("encodedSecret")
                .active(true)
                .roles(List.of(new Role(1L, "ROLE_USER")))
                .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("secret123", "encodedSecret")).thenReturn(true);
        when(jwtUtil.generateToken(eq("alice@example.com"), any())).thenReturn("mock.jwt.token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.token").value("mock.jwt.token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login rejects deactivated user with 403 Forbidden")
    void testLoginDeactivatedUserRejected() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("deactivated@example.com")
                .password("secret123")
                .build();

        User deactivated = User.builder()
                .id(2L)
                .fullName("Deactivated User")
                .email("deactivated@example.com")
                .password("encodedSecret")
                .active(false)
                .build();

        when(userRepository.findByEmail("deactivated@example.com")).thenReturn(Optional.of(deactivated));
        when(passwordEncoder.matches("secret123", "encodedSecret")).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Account has been deactivated"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login rejects invalid credentials with 401 Unauthorized")
    void testLoginInvalidCredentials() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("alice@example.com")
                .password("wrongpassword")
                .build();

        User existing = User.builder()
                .id(1L)
                .fullName("Alice Wonder")
                .email("alice@example.com")
                .password("encodedSecret")
                .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrongpassword", "encodedSecret")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }
}
