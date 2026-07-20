package com.survisha.meghaconnect.config;

import com.survisha.meghaconnect.entity.Department;
import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuperAdminBootstrapTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationArguments applicationArguments;

    @Test
    void repairsOnlySuperAdminRecordWithEncodedPasswordAndNoDepartment() {
        SuperAdminBootstrap bootstrap = new SuperAdminBootstrap(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(bootstrap, "configuredUsername", "superadmin");
        ReflectionTestUtils.setField(bootstrap, "configuredInitialPassword", "Megha@TW26");
        ReflectionTestUtils.setField(bootstrap, "passwordRepairEnabled", true);

        Department department = new Department();
        department.setId(99L);
        User existing = User.builder()
            .username(" SuperAAdmin ")
            .fullName("Old Name")
            .role(User.UserRole.ADMIN)
            .department(department)
            .passwordHash("plain-or-stale")
            .active(false)
            .locked(true)
            .build();

        when(userRepository.findAllByNormalizedUsername("superadmin")).thenReturn(List.of());
        when(userRepository.findAllByNormalizedUsername("superaadmin")).thenReturn(List.of(existing));
        when(passwordEncoder.matches("Megha@TW26", "plain-or-stale")).thenReturn(false);
        when(passwordEncoder.encode("Megha@TW26")).thenReturn("$2a$10$encoded");

        bootstrap.run(applicationArguments);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertEquals("superadmin", saved.getUsername());
        assertEquals(User.UserRole.SUPER_ADMIN, saved.getRole());
        assertEquals("$2a$10$encoded", saved.getPasswordHash());
        assertNull(saved.getDepartment());
        assertTrue(saved.isActive());
        assertFalse(saved.isLocked());
        assertTrue(saved.isPasswordChangeRequired());
    }

    @Test
    void deactivatesLegacyDuplicateWhenCanonicalSuperAdminExists() {
        SuperAdminBootstrap bootstrap = new SuperAdminBootstrap(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(bootstrap, "configuredUsername", "superadmin");
        ReflectionTestUtils.setField(bootstrap, "configuredInitialPassword", "Megha@TW26");
        ReflectionTestUtils.setField(bootstrap, "passwordRepairEnabled", false);

        User canonical = User.builder()
            .username("superadmin")
            .fullName("Super Admin")
            .role(User.UserRole.SUPER_ADMIN)
            .passwordHash("$2a$10$canonical")
            .active(true)
            .locked(false)
            .build();
        canonical.setId(1L);

        User legacy = User.builder()
            .username("superaadmin")
            .fullName("Legacy Super Admin")
            .role(User.UserRole.SUPER_ADMIN)
            .passwordHash("$2a$10$legacy")
            .active(true)
            .locked(false)
            .build();
        legacy.setId(2L);

        when(userRepository.findAllByNormalizedUsername("superadmin")).thenReturn(List.of(canonical));
        when(userRepository.findAllByNormalizedUsername("superaadmin")).thenReturn(List.of(legacy));
        when(passwordEncoder.matches("Megha@TW26", "$2a$10$canonical")).thenReturn(true);

        bootstrap.run(applicationArguments);

        assertEquals("superaadmin-inactive-2", legacy.getUsername());
        assertFalse(legacy.isActive());
        assertTrue(legacy.isLocked());
        assertEquals("superadmin", canonical.getUsername());
        verify(userRepository, times(2)).save(any(User.class));
    }
}
