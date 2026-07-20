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

import java.util.Optional;

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
        ReflectionTestUtils.setField(bootstrap, "configuredInitialPassword", "Megha@TW26");

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

        when(userRepository.findByNormalizedUsername("superaadmin")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("Megha@TW26", "plain-or-stale")).thenReturn(false);
        when(passwordEncoder.encode("Megha@TW26")).thenReturn("$2a$10$encoded");

        bootstrap.run(applicationArguments);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertEquals("superaadmin", saved.getUsername());
        assertEquals(User.UserRole.SUPER_ADMIN, saved.getRole());
        assertEquals("$2a$10$encoded", saved.getPasswordHash());
        assertNull(saved.getDepartment());
        assertTrue(saved.isActive());
        assertFalse(saved.isLocked());
        assertTrue(saved.isPasswordChangeRequired());
    }
}
