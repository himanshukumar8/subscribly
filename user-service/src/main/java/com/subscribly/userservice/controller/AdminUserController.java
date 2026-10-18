package com.subscribly.userservice.controller;

import com.subscribly.common.dto.UserDto;
import com.subscribly.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping({ "", "/all" })
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers().stream()
                .map(userService::toDto)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserDto> updateUserRole(
            @PathVariable Long id,
            @RequestParam String role
    ) {
        return ResponseEntity.ok(userService.toDto(userService.updateUserRole(id, role)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<UserDto> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return ResponseEntity.ok(userService.toDto(userService.updateUserStatus(id, status)));
    }
}
