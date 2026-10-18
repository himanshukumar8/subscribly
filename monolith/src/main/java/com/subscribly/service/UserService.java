package com.subscribly.service;

import com.subscribly.dto.UserResponse;
import com.subscribly.entity.Role;
import com.subscribly.entity.User;
import com.subscribly.repository.RoleRepository;
import com.subscribly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse updateUserRole(Long id, String roleName) {
        return updateUserRole(id, roleName, null);
    }

    @Transactional
    public UserResponse updateUserRole(Long id, String roleName, String operatingAdminEmail) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID must not be null");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id));

        String normalizedRole = normalizeRole(roleName);
        Role role = roleRepository.findByName(normalizedRole)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found in system: " + normalizedRole));

        boolean isCurrentlyAdmin = user.isActive() && user.getRoles() != null &&
                user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName()) || "ADMIN".equalsIgnoreCase(r.getName()));
        boolean willBeAdmin = "ROLE_ADMIN".equalsIgnoreCase(normalizedRole);

        // Self-demotion protection
        if (operatingAdminEmail != null && user.getEmail().equalsIgnoreCase(operatingAdminEmail.trim()) && !willBeAdmin) {
            log.warn("[UserService] Blocked self-demotion attempt by admin '{}'", operatingAdminEmail);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Self-demotion is not permitted for the active administrator");
        }

        // Last active administrator demotion protection
        if (isCurrentlyAdmin && !willBeAdmin) {
            lockAdminRole();
            long activeAdminCount = userRepository.countActiveAdministrators();
            if (activeAdminCount <= 1) {
                log.warn("[UserService] Blocked demotion of last remaining active administrator id={}", id);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot demote the last remaining active administrator");
            }
        }

        if (user.getRoles() == null) {
            user.setRoles(new java.util.ArrayList<>());
        } else {
            user.getRoles().clear();
        }
        user.getRoles().add(role);
        User saved = userRepository.save(user);
        log.info("[UserService] Updated role for user id={} to {}", id, normalizedRole);
        return toResponse(saved);
    }

    public UserResponse updateUserStatus(Long id, String status) {
        return updateUserStatus(id, status, null);
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, String status, String operatingAdminEmail) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID must not be null");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id));

        boolean newActive = validateStatus(status);

        // Self-deactivation protection
        if (!newActive && operatingAdminEmail != null && user.getEmail().equalsIgnoreCase(operatingAdminEmail.trim())) {
            log.warn("[UserService] Blocked self-deactivation attempt by admin '{}'", operatingAdminEmail);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrators cannot deactivate their own active account");
        }

        // Last active administrator deactivation protection
        if (!newActive && user.isActive()) {
            boolean isCurrentlyAdmin = user.getRoles() != null &&
                    user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getName()) || "ADMIN".equalsIgnoreCase(r.getName()));
            if (isCurrentlyAdmin) {
                lockAdminRole();
                long activeAdminCount = userRepository.countActiveAdministrators();
                if (activeAdminCount <= 1) {
                    log.warn("[UserService] Blocked deactivation of last remaining active administrator id={}", id);
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot deactivate the last remaining active administrator");
                }
            }
        }

        user.setActive(newActive);
        User saved = userRepository.save(user);
        log.info("[UserService] Updated active status for user id={} to {}", id, user.isActive());
        return toResponse(saved);
    }

    private void lockAdminRole() {
        roleRepository.findByNameWithLock("ROLE_ADMIN");
    }

    private String normalizeRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role name must not be blank");
        }
        String trimmed = roleName.trim().toUpperCase();
        if ("ROLE_ADMIN".equals(trimmed) || "ADMIN".equals(trimmed)) {
            return "ROLE_ADMIN";
        }
        if ("ROLE_USER".equals(trimmed) || "USER".equals(trimmed)) {
            return "ROLE_USER";
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid role: " + roleName + ". Allowed roles: ROLE_USER, ROLE_ADMIN");
    }

    private boolean validateStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must not be blank");
        }
        String trimmed = status.trim().toUpperCase();
        if ("ACTIVE".equals(trimmed)) {
            return true;
        }
        if ("INACTIVE".equals(trimmed)) {
            return false;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid status: " + status + ". Allowed values: ACTIVE, INACTIVE");
    }

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        List<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getName).toList()
                : Collections.emptyList();
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roleNames)
                .active(user.isActive())
                .build();
    }
}
