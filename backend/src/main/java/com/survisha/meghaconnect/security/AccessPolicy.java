package com.survisha.meghaconnect.security;

import com.survisha.meghaconnect.entity.Department;
import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("accessPolicy")
@RequiredArgsConstructor
public class AccessPolicy {

    private final UserRepository userRepository;

    public boolean canManageCmoConfiguration() {
        return currentUser().map(this::canManageCmoConfiguration).orElse(false);
    }

    public boolean canViewAuditTrail() {
        return currentUser().map(user -> user.getRole() == User.UserRole.SUPER_ADMIN
                || user.getRole() == User.UserRole.ADMIN
                || hasActiveDepartmentAdminContext(user)).orElse(false);
    }

    public boolean canAssignRole(User actor, User.UserRole targetRole) {
        if (actor == null || targetRole == null) return false;
        if (actor.getRole() == User.UserRole.SUPER_ADMIN) {
            return targetRole == User.UserRole.DEPARTMENT_ADMIN;
        }
        if (actor.getRole() == User.UserRole.ADMIN) return true;
        if (actor.getRole() != User.UserRole.DEPARTMENT_ADMIN) return false;
        if (isCmoDepartment(actor)) {
            return targetRole == User.UserRole.APPROVER
                    || targetRole == User.UserRole.DEO
                    || targetRole == User.UserRole.HCM;
        }
        return targetRole == User.UserRole.DEO
                || targetRole == User.UserRole.DEPARTMENT_PA
                || targetRole == User.UserRole.HEAD_DEPARTMENT;
    }

    public Optional<User> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return Optional.empty();
        return userRepository.findByNormalizedUsername(authentication.getName());
    }

    public boolean isCmoDepartment(User user) {
        Department department = user == null ? null : user.getDepartment();
        return department != null
                && department.getStatus() == Department.DepartmentStatus.ACTIVE
                && "CMO".equalsIgnoreCase(department.getDepartmentCode());
    }

    private boolean canManageCmoConfiguration(User user) {
        return user.getRole() == User.UserRole.SUPER_ADMIN
                || user.getRole() == User.UserRole.ADMIN
                || (user.getRole() == User.UserRole.DEPARTMENT_ADMIN && isCmoDepartment(user));
    }

    private boolean hasActiveDepartmentAdminContext(User user) {
        return user.getRole() == User.UserRole.DEPARTMENT_ADMIN
                && user.getDepartment() != null
                && user.getDepartment().getId() != null
                && user.getDepartment().getStatus() == Department.DepartmentStatus.ACTIVE;
    }
}
