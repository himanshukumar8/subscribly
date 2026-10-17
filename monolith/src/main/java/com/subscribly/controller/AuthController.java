package com.subscribly.controller;

import com.subscribly.config.JwtUtil;
import com.subscribly.dto.AuthResponse;
import com.subscribly.dto.LoginRequest;
import com.subscribly.dto.RegisterRequest;
import com.subscribly.entity.Role;
import com.subscribly.entity.User;
import com.subscribly.repository.RoleRepository;
import com.subscribly.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "ERROR",
                    "message", "Email already in use: " + email
            ));
        }

        List<Role> userRoles = new ArrayList<>();
        roleRepository.findByName("ROLE_USER").ifPresent(userRoles::add);

        User user = User.builder()
                .fullName(req.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .roles(userRoles)
                .build();

        User saved = userRepository.save(user);
        String token = jwtUtil.generateToken(saved.getEmail());

        log.info("[AuthController] Registered user id={}, email={}", saved.getId(), saved.getEmail());
        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            log.warn("[AuthController] Login failed for email={}", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "ERROR",
                    "message", "Invalid credentials"
            ));
        }

        if (!user.isActive()) {
            log.warn("[AuthController] Login rejected for deactivated account: {}", email);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "status", "ERROR",
                    "message", "Account has been deactivated"
            ));
        }

        List<String> roleNames = (user.getRoles() != null && !user.getRoles().isEmpty())
                ? user.getRoles().stream().map(Role::getName).toList()
                : List.of("ROLE_USER");
        String token = jwtUtil.generateToken(user.getEmail(), roleNames);
        log.info("[AuthController] User logged in: {}", user.getEmail());
        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build());
    }
}
