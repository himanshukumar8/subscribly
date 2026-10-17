package com.subscribly.userservice.controller;

import com.subscribly.common.dto.UserDto;
import com.subscribly.userservice.entity.User;
import com.subscribly.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getInternalUserById(@PathVariable Long id) {
        log.info("[InternalAPI] Service-to-service lookup for User ID: {}", id);
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok().body(toDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getInternalUserByEmail(@PathVariable String email) {
        log.info("[InternalAPI] Service-to-service lookup for User Email: {}", email);
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok().body(toDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    private UserDto toDto(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName())
                .toList();
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roleNames)
                .build();
    }
}
