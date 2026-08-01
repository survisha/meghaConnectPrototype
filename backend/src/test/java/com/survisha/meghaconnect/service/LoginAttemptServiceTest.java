package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginAttemptServiceTest {
    private UserRepository repository;
    private LoginAttemptService service;
    private User user;
    @BeforeEach void setUp() {
        repository = mock(UserRepository.class);
        service = new LoginAttemptService(repository, mock(AuditLogService.class));
        ReflectionTestUtils.setField(service, "maxFailedAttempts", 3);
        user = User.builder().id(7L).username("operator").role(User.UserRole.DEO).active(true).build();
        when(repository.findForLoginUpdate("operator")).thenReturn(Optional.of(user));
    }
    @Test void thirdFailureLocksPersistedAccount() {
        assertFalse(service.recordFailure("operator"));
        assertFalse(service.recordFailure("operator"));
        assertTrue(service.recordFailure("operator"));
        assertTrue(user.isLocked()); assertEquals(3, user.getFailedLoginAttempts());
        assertNotNull(user.getLockedAt()); assertNotNull(user.getLockReason());
    }
    @Test void successResetsAttemptState() {
        user.setFailedLoginAttempts(2); user.setLastFailedLoginAt(java.time.LocalDateTime.now());
        service.recordSuccess("operator");
        assertEquals(0, user.getFailedLoginAttempts()); assertNull(user.getLastFailedLoginAt());
        assertNotNull(user.getLastLogin());
    }
}
