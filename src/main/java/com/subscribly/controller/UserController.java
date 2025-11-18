package com.subscribly.controller;

import com.subscribly.entity.User;
import com.subscribly.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public User getLoggedUser(@AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return null;

        return userService.findByEmail(userDetails.getUsername())
                .orElse(null);
    }
}
