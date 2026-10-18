package com.subscribly.security;

import com.subscribly.config.AdminBootstrapConfig;
import com.subscribly.entity.Role;
import com.subscribly.entity.User;
import com.subscribly.repository.RoleRepository;
import com.subscribly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PlatformTransactionManager transactionManager;

    private AdminBootstrapConfig bootstrapConfig;

    @BeforeEach
    void setUp() {
        bootstrapConfig = new AdminBootstrapConfig(
                userRepository,
                roleRepository,
                passwordEncoder,
                transactionManager
        );
    }

    @Test
    @DisplayName("Disabled bootstrap does not query or seed administrators")
    void testDisabledBootstrapDoesNothing() {
        ReflectionTestUtils.setField(bootstrapConfig, "bootstrapEnabled", false);
        ReflectionTestUtils.setField(bootstrapConfig, "adminPassword", "AStrongValidKey#2026!");

        bootstrapConfig.run();

        verifyNoInterceptors();
    }

    @Test
    @DisplayName("Enabled bootstrap with missing password rejects initialization")
    void testEnabledBootstrapMissingPasswordRejects() {
        ReflectionTestUtils.setField(bootstrapConfig, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(bootstrapConfig, "adminPassword", "");

        boolean result = bootstrapConfig.validateAndSeedAdministrator();

        assertFalse(result, "Bootstrap must fail when password is blank");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Enabled bootstrap with short password (<12 chars) rejects initialization")
    void testEnabledBootstrapShortPasswordRejects() {
        ReflectionTestUtils.setField(bootstrapConfig, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(bootstrapConfig, "adminPassword", "Short#123");

        boolean result = bootstrapConfig.validateAndSeedAdministrator();

        assertFalse(result, "Bootstrap must fail when password has fewer than 12 characters");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Enabled bootstrap with predictable default password (e.g. Password@123) rejects initialization")
    void testEnabledBootstrapPredictableDefaultPasswordRejects() {
        ReflectionTestUtils.setField(bootstrapConfig, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(bootstrapConfig, "adminPassword", "Password@123");

        boolean result = bootstrapConfig.validateAndSeedAdministrator();

        assertFalse(result, "Bootstrap must reject known predictable default passwords");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Enabled bootstrap with previous administrators (e.g. deactivated account) does NOT recreate or overwrite")
    void testBootstrapDoesNotRecreateIfAdminEverExisted() {
        ReflectionTestUtils.setField(bootstrapConfig, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(bootstrapConfig, "adminPassword", "AStrongValidKey#2026!");

        when(userRepository.countAllAdministratorsEver()).thenReturn(1L);

        boolean result = bootstrapConfig.validateAndSeedAdministrator();

        assertFalse(result, "Bootstrap must not recreate admin if one previously existed in database");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Enabled bootstrap with valid high-entropy secret on clean database seeds initial administrator")
    void testBootstrapSeedsInitialAdministratorOnCleanDatabase() {
        String strongSecret = "AStrongValidKey#2026!";
        ReflectionTestUtils.setField(bootstrapConfig, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(bootstrapConfig, "adminEmail", "admin1@example.com");
        ReflectionTestUtils.setField(bootstrapConfig, "adminPassword", strongSecret);

        when(userRepository.countAllAdministratorsEver()).thenReturn(0L);
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(Role.builder().name("ROLE_ADMIN").build()));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(Role.builder().name("ROLE_USER").build()));
        when(userRepository.findByEmail("admin1@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(strongSecret)).thenReturn("bcrypt_hashed_value");

        boolean result = bootstrapConfig.validateAndSeedAdministrator();

        assertTrue(result, "Bootstrap must succeed on clean database with valid secret");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertEquals("admin1@example.com", saved.getEmail());
        assertEquals("bcrypt_hashed_value", saved.getPassword());
        assertTrue(saved.isActive());
        assertTrue(saved.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName())));
    }

    private void verifyNoInterceptors() {
        verify(userRepository, never()).countAllAdministratorsEver();
        verify(userRepository, never()).save(any());
    }
}
