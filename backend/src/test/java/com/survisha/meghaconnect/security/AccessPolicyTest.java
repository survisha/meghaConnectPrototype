package com.survisha.meghaconnect.security;

import com.survisha.meghaconnect.entity.Department;
import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccessPolicyTest {

    private final UserRepository users = mock(UserRepository.class);
    private final AccessPolicy policy = new AccessPolicy(users);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generalDepartmentAdminCannotManageCmoConfigurationButCanViewAudit() {
        authenticate(departmentAdmin("HEALTH"));

        assertFalse(policy.canManageCmoConfiguration());
        assertTrue(policy.canViewAuditTrail());
    }

    @Test
    void cmoDepartmentAdminCanManageCmoConfigurationAndRequiredRoles() {
        User actor = departmentAdmin("CMO");
        authenticate(actor);

        assertTrue(policy.canManageCmoConfiguration());
        assertTrue(policy.canViewAuditTrail());
        assertTrue(policy.canAssignRole(actor, User.UserRole.APPROVER));
        assertTrue(policy.canAssignRole(actor, User.UserRole.DEO));
        assertTrue(policy.canAssignRole(actor, User.UserRole.HCM));
        assertFalse(policy.canAssignRole(actor, User.UserRole.SUPER_ADMIN));
        assertFalse(policy.canAssignRole(actor, User.UserRole.DEPARTMENT_PA));
    }

    @Test
    void superAdminBehaviorIsPreserved() {
        User actor = User.builder().username("superadmin").role(User.UserRole.SUPER_ADMIN).build();
        authenticate(actor);
        assertTrue(policy.canManageCmoConfiguration());
        assertTrue(policy.canViewAuditTrail());
    }

    private void authenticate(User user) {
        when(users.findByNormalizedUsername(user.getUsername())).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), "n/a", java.util.List.of()));
    }

    private User departmentAdmin(String code) {
        Department department = Department.builder()
                .departmentCode(code)
                .departmentName(code)
                .status(Department.DepartmentStatus.ACTIVE)
                .build();
        department.setId(10L);
        return User.builder()
                .username(code.toLowerCase() + "-admin")
                .role(User.UserRole.DEPARTMENT_ADMIN)
                .department(department)
                .build();
    }
}
