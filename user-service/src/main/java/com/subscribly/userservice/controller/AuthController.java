package com.subscribly.userservice.controller;

import com.subscribly.userservice.dto.AuthResponse;
import com.subscribly.userservice.dto.LoginRequest;
import com.subscribly.userservice.dto.RegisterRequest;
import com.subscribly.userservice.entity.User;
import com.subscribly.userservice.service.UserService;
import com.subscribly.userservice.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {

        User user = userService.findByEmail(req.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user);

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .user(userService.toDto(user))
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        try {
            User user = userService.registerUser(req);
            return ResponseEntity.ok(userService.toDto(user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
