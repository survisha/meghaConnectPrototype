package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.AuthRequest;
import com.survisha.meghaconnect.dto.AuthResponse;
import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.exception.MeghaConnectException;
import com.survisha.meghaconnect.repository.UserRepository;
import com.survisha.meghaconnect.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void superAdminLoginReturnsBearerAccessTokenWithoutDepartment() {
        User appUser = User.builder()
            .username("superaadmin")
            .fullName("Super Admin")
            .role(User.UserRole.SUPER_ADMIN)
            .active(true)
            .locked(false)
            .passwordHash("$2a$10$encoded")
            .build();
        appUser.setId(1L);
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            "superaadmin",
            appUser.getPasswordHash(),
            List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        );

        when(userDetailsService.loadUserByUsername("superaadmin")).thenReturn(userDetails);
        when(userRepository.findByNormalizedUsername("superaadmin")).thenReturn(Optional.of(appUser));
        when(jwtService.generateToken(userDetails, appUser)).thenReturn("jwt-token");
        when(userService.getFullNameByUsername("superaadmin")).thenReturn("Super Admin");

        AuthResponse response = authService.login(new AuthRequest("  superaadmin  ", "Megha@TW26"));

        assertEquals("jwt-token", response.getToken());
        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("ROLE_SUPER_ADMIN", response.getRole());
        assertNull(response.getDepartmentId());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void invalidSuperAdminPasswordReturns401() {
        doThrow(new BadCredentialsException("bad credentials"))
            .when(authenticationManager).authenticate(any());

        MeghaConnectException ex = assertThrows(MeghaConnectException.class,
            () -> authService.login(new AuthRequest("superaadmin", "wrong")));

        assertEquals(401, ex.getHttpStatus());
    }

    @Test
    void disabledSuperAdminLoginIsRejected() {
        doThrow(new DisabledException("disabled"))
            .when(authenticationManager).authenticate(any());

        MeghaConnectException ex = assertThrows(MeghaConnectException.class,
            () -> authService.login(new AuthRequest("superaadmin", "Megha@TW26")));

        assertEquals(403, ex.getHttpStatus());
    }

    @Test
    void lockedSuperAdminLoginIsRejected() {
        doThrow(new LockedException("locked"))
            .when(authenticationManager).authenticate(any());

        MeghaConnectException ex = assertThrows(MeghaConnectException.class,
            () -> authService.login(new AuthRequest("superaadmin", "Megha@TW26")));

        assertEquals(423, ex.getHttpStatus());
    }
}
