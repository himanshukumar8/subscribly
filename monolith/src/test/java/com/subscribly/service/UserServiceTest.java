package com.subscribly.service;

import com.subscribly.dto.UserResponse;
import com.subscribly.entity.Role;
import com.subscribly.entity.User;
import com.subscribly.repository.RoleRepository;
import com.subscribly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private User adminUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userRole = new Role(1L, "ROLE_USER");
        adminRole = new Role(2L, "ROLE_ADMIN");

        sampleUser = User.builder()
                .id(1L)
                .fullName("Test User")
                .email("test@example.com")
                .password("hashed_pw")
                .roles(new ArrayList<>(List.of(userRole)))
                .active(true)
                .build();

        adminUser = User.builder()
                .id(2L)
                .fullName("Admin User")
                .email("admin@example.com")
                .password("hashed_admin_pw")
                .roles(new ArrayList<>(List.of(adminRole)))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("findByEmail: returns user when email exists")
    void testFindByEmailFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        Optional<User> result = userService.findByEmail("test@example.com");
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    @DisplayName("findByEmail: returns empty for null or blank email")
    void testFindByEmailBlank() {
        assertTrue(userService.findByEmail(null).isEmpty());
        assertTrue(userService.findByEmail("   ").isEmpty());
    }

    @Test
    @DisplayName("getAllUsers: returns converted UserResponse list without password")
    void testGetAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<UserResponse> users = userService.getAllUsers();
        assertEquals(1, users.size());
        assertEquals("test@example.com", users.get(0).getEmail());
        assertEquals(List.of("ROLE_USER"), users.get(0).getRoles());
    }

    @Test
    @DisplayName("updateUserRole: updates role successfully and normalizes shorthand ADMIN")
    void testUpdateUserRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse res = userService.updateUserRole(1L, "ADMIN");
        assertNotNull(res);
        assertEquals(List.of("ROLE_ADMIN"), res.getRoles());
    }

    @Test
    @DisplayName("updateUserRole: throws 404 when user id does not exist")
    void testUpdateUserRoleUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserRole(999L, "ROLE_ADMIN")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("User not found with id: 999"));
    }

    @Test
    @DisplayName("updateUserRole: throws 400 when role is invalid")
    void testUpdateUserRoleInvalidRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserRole(1L, "SUPER_HACKER")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid role"));
    }

    @Test
    @DisplayName("updateUserRole: blocks demotion of the last remaining active administrator (409 Conflict)")
    void testUpdateUserRoleLastAdminDemotionBlocked() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.countActiveAdministrators()).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserRole(2L, "ROLE_USER", "otheradmin@example.com")
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Cannot demote the last remaining active administrator"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserRole: blocks self-demotion by operating administrator (409 Conflict)")
    void testUpdateUserRoleSelfDemotionBlocked() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserRole(2L, "ROLE_USER", "admin@example.com")
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Self-demotion is not permitted"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserStatus: updates active status successfully")
    void testUpdateUserStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse res = userService.updateUserStatus(1L, "INACTIVE");
        assertFalse(res.isActive());

        UserResponse resActive = userService.updateUserStatus(1L, "ACTIVE");
        assertTrue(resActive.isActive());
    }

    @Test
    @DisplayName("updateUserStatus: throws 404 when user id does not exist")
    void testUpdateUserStatusNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserStatus(999L, "INACTIVE")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("updateUserStatus: throws 400 when status value is invalid")
    void testUpdateUserStatusInvalidValue() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserStatus(1L, "BANANA")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Allowed values: ACTIVE, INACTIVE"));
    }

    @Test
    @DisplayName("updateUserStatus: blocks deactivation of the last remaining active administrator (409 Conflict)")
    void testUpdateUserStatusLastAdminDeactivationBlocked() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countActiveAdministrators()).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserStatus(2L, "INACTIVE", "otheradmin@example.com")
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Cannot deactivate the last remaining active administrator"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserStatus: blocks self-deactivation by operating administrator (400 Bad Request)")
    void testUpdateUserStatusSelfDeactivationBlocked() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserStatus(2L, "INACTIVE", "admin@example.com")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Administrators cannot deactivate their own active account"));
        verify(userRepository, never()).save(any());
    }
}
