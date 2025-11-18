package com.subscribly.controller;

import com.subscribly.dto.LoginRequest;
import com.subscribly.entity.User;
import com.subscribly.service.UserService;
import com.subscribly.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {

        // load user by email
        User user = userService.findByEmail(req.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        // generate JWT using EMAIL only
        String token = jwtUtil.generateToken(user.getEmail());

        return ResponseEntity.ok("Bearer " + token);
    }
}
