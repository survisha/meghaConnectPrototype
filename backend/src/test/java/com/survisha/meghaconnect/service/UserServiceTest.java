package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.CreateUserRequest;
import com.survisha.meghaconnect.dto.UserResponse;
import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserEncryptsPasswordAndSavesActiveUser() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .fullName("New User")
                .password("secret1")
                .role(User.UserRole.DATA_ENTRY_OPERATOR)
                .phoneNumber("9876543210")
                .build();

        when(roleService.existsByRoleName("DATA_ENTRY_OPERATOR")).thenReturn(true);
        when(passwordEncoder.encode("secret1")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        UserResponse response = userService.createUser(request, "admin");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertEquals(42L, response.getId());
        assertEquals("$2a$10$encoded", saved.getPasswordHash());
        assertNotEquals("secret1", saved.getPasswordHash());
        assertTrue(saved.isActive());
        assertEquals("admin", saved.getCreatedBy());
        assertEquals("9876543210", saved.getPhoneNumber());
    }

    @Test
    void createUserRejectsDuplicateUsername() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("admin")
                .fullName("Admin")
                .password("secret1")
                .role(User.UserRole.ADMIN)
                .build();

        when(userRepository.existsByUsername("admin")).thenReturn(true);

        MeghaConnectException ex = assertThrows(MeghaConnectException.class,
                () -> userService.createUser(request, "admin"));

        assertEquals(409, ex.getHttpStatus());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserRejectsRoleMissingFromRolesTable() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("security1")
                .fullName("Security")
                .password("secret1")
                .role(User.UserRole.SECURITY)
                .build();

        when(roleService.existsByRoleName("SECURITY")).thenReturn(false);

        MeghaConnectException ex = assertThrows(MeghaConnectException.class,
                () -> userService.createUser(request, "admin"));

        assertEquals(400, ex.getHttpStatus());
        verify(userRepository, never()).save(any(User.class));
    }
}
