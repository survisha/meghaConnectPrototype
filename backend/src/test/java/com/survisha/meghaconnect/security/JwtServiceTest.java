package com.survisha.meghaconnect.security;

import com.survisha.meghaconnect.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String RAW_SECRET = "CHANGE_ME_MIN_256_BIT_SECRET_FOR_UAT_ONLY";

    @Test
    void superAdminTokenCarriesAuthorityAndWorksWithoutDepartment() {
        JwtService jwtService = new JwtService();
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", RAW_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 86400000L);
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", RAW_SECRET);

        User appUser = User.builder()
            .username("superaadmin")
            .role(User.UserRole.SUPER_ADMIN)
            .fullName("Super Admin")
            .passwordHash("$2a$10$encoded")
            .build();
        appUser.setId(1L);
        UserDetails details = new org.springframework.security.core.userdetails.User(
            "superaadmin",
            appUser.getPasswordHash(),
            List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        );

        String token = jwtService.generateToken(details, appUser);

        assertEquals("superaadmin", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, details));
        assertEquals("SUPER_ADMIN", jwtUtils.getRoleFromToken(token));
    }
}
