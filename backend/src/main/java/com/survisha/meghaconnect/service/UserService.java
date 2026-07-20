package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.CreateUserRequest;
import com.survisha.meghaconnect.dto.UpdateUserRequest;
import com.survisha.meghaconnect.dto.UserResponse;
import com.survisha.meghaconnect.entity.Department;
import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.repository.DepartmentRepository;
import com.survisha.meghaconnect.repository.UserRepository;
import com.survisha.meghaconnect.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get all users
     */
    public List<User> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }

    public List<UserResponse> getAllUserResponses() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<UserResponse> getUserResponsesForActor(String actor) {
        User currentUser = userRepository.findByUsername(actor).orElse(null);
        if (currentUser != null && currentUser.getRole() == User.UserRole.DEPARTMENT_ADMIN
                && currentUser.getDepartment() != null) {
            return userRepository.findByDepartment_Id(currentUser.getDepartment().getId()).stream()
                    .map(this::toResponse)
                    .toList();
        }
        return getAllUserResponses();
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        return userRepository.findById(id);
    }

    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        log.debug("Fetching user by username: {}", username);
        return userRepository.findByUsername(username);
    }

    /**
     * Get full name by username
     */
    public String getFullNameByUsername(String username) {
        log.debug("Fetching full name for username: {}", username);
        return userRepository.findByUsername(username)
            .map(User::getFullName)
            .orElse(username);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request, String actor) {
        if (request == null) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "request"),
                    400
            );
        }
        String username = trimToNull(request.getUsername());
        String fullName = trimToNull(request.getFullName());
        String password = trimToNull(request.getPassword());
        String email = trimToNull(request.getEmail());
        String phoneNumber = trimToNull(request.getPhoneNumber());
        User actorUser = userRepository.findByUsername(actor).orElse(null);
        if (username == null) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "username"),
                    400
            );
        }
        if (fullName == null) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "fullName"),
                    400
            );
        }
        if (password == null) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "password"),
                    400
            );
        }
        if (userRepository.existsByUsername(username)) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.DUPLICATE_ENTRY,
                    ErrorCodeConstants.format(ErrorCodeConstants.DUPLICATE_ENTRY_MSG, "username"),
                    409
            );
        }
        if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.DUPLICATE_ENTRY,
                    ErrorCodeConstants.format(ErrorCodeConstants.DUPLICATE_ENTRY_MSG, "mobile"),
                    409
            );
        }
        if (email != null && userRepository.existsByEmailIgnoreCase(email)) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.DUPLICATE_ENTRY,
                    ErrorCodeConstants.format(ErrorCodeConstants.DUPLICATE_ENTRY_MSG, "email"),
                    409
            );
        }
        if (request.getRole() == null) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.INVALID_ROLE,
                    ErrorCodeConstants.format(ErrorCodeConstants.INVALID_ROLE_MSG, ""),
                    400
            );
        }
        String roleName = request.getRole().name();
        if (!roleService.existsByRoleName(roleName)) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.ROLE_NOT_FOUND,
                    ErrorCodeConstants.format(ErrorCodeConstants.ROLE_NOT_FOUND_MSG, roleName),
                    400
            );
        }
        validateCreatorCanAssignRole(actorUser, request.getRole());
        Department department = resolveDepartmentForCreate(request.getDepartmentId(), request.getRole(), actorUser);

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .email(email)
                .role(request.getRole())
                .department(department)
                .phoneNumber(phoneNumber)
                .active(request.getActive() == null || Boolean.TRUE.equals(request.getActive()))
                .offlineAccess(Boolean.TRUE.equals(request.getOfflineAccess()))
                .passwordChangeRequired(true)
                .build();
        user.setCreatedBy(actor);
        user.setUpdatedBy(actor);
        User saved = userRepository.save(user);
        log.info("User created username={} role={} by={}", saved.getUsername(), saved.getRole(), actor);
        return toResponse(saved);
    }

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentCode(user.getDepartment() != null ? user.getDepartment().getDepartmentCode() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getDepartmentName() : null)
                .phoneNumber(user.getPhoneNumber())
                .active(user.isActive())
                .locked(user.isLocked())
                .offlineAccess(user.isOfflineAccess())
                .passwordChangeRequired(user.isPasswordChangeRequired())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, String actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new MeghaConnectException(
                        ErrorCodeConstants.USER_NOT_FOUND,
                        ErrorCodeConstants.format(ErrorCodeConstants.USER_NOT_FOUND_MSG, id),
                        404));
        String fullName = trimToNull(request.getFullName());
        String email = trimToNull(request.getEmail());
        String phoneNumber = trimToNull(request.getPhoneNumber());
        User actorUser = userRepository.findByUsername(actor).orElse(null);
        if (fullName == null) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "fullName"),
                    400);
        }
        if (request.getRole() == null || !roleService.existsByRoleName(request.getRole().name())) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.INVALID_ROLE,
                    ErrorCodeConstants.format(ErrorCodeConstants.INVALID_ROLE_MSG, ""),
                    400);
        }
        if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)
                && !phoneNumber.equals(user.getPhoneNumber())) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.DUPLICATE_ENTRY,
                    ErrorCodeConstants.format(ErrorCodeConstants.DUPLICATE_ENTRY_MSG, "mobile"),
                    409);
        }
        if (email != null && userRepository.existsByEmailIgnoreCase(email)
                && !email.equalsIgnoreCase(String.valueOf(user.getEmail()))) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.DUPLICATE_ENTRY,
                    ErrorCodeConstants.format(ErrorCodeConstants.DUPLICATE_ENTRY_MSG, "email"),
                    409);
        }
        validateCreatorCanAssignRole(actorUser, request.getRole());
        Department department = resolveDepartmentForUpdate(request.getDepartmentId(), request.getRole(), actorUser, user);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(request.getRole());
        user.setDepartment(department);
        user.setPhoneNumber(phoneNumber);
        user.setActive(request.getActive() == null || Boolean.TRUE.equals(request.getActive()));
        user.setLocked(Boolean.TRUE.equals(request.getLocked()));
        user.setOfflineAccess(Boolean.TRUE.equals(request.getOfflineAccess()));
        user.setUpdatedBy(actor);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse setActive(Long id, boolean active, String actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new MeghaConnectException(
                        ErrorCodeConstants.USER_NOT_FOUND,
                        ErrorCodeConstants.format(ErrorCodeConstants.USER_NOT_FOUND_MSG, id),
                        404));
        user.setActive(active);
        user.setUpdatedBy(actor);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse unlockUser(Long id, String actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new MeghaConnectException(
                        ErrorCodeConstants.USER_NOT_FOUND,
                        ErrorCodeConstants.format(ErrorCodeConstants.USER_NOT_FOUND_MSG, id),
                        404));
        user.setLocked(false);
        user.setUpdatedBy(actor);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.USER_NOT_FOUND,
                    ErrorCodeConstants.format(ErrorCodeConstants.USER_NOT_FOUND_MSG, id),
                    404);
        }
        userRepository.deleteById(id);
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private void validateCreatorCanAssignRole(User actorUser, User.UserRole role) {
        if (actorUser == null) {
            return;
        }
        if (actorUser.getRole() == User.UserRole.SUPER_ADMIN && role == User.UserRole.DEPARTMENT_ADMIN) {
            return;
        }
        if (actorUser.getRole() == User.UserRole.DEPARTMENT_ADMIN && role == User.UserRole.DEPARTMENT_PA) {
            return;
        }
        if (actorUser.getRole() == User.UserRole.ADMIN) {
            return;
        }
        throw new MeghaConnectException(
                ErrorCodeConstants.INVALID_ROLE,
                "User role " + role + " cannot be assigned by " + actorUser.getRole(),
                403);
    }

    private Department resolveDepartmentForCreate(Long departmentId, User.UserRole role, User actorUser) {
        if (role == User.UserRole.SUPER_ADMIN) {
            return null;
        }
        if (role != User.UserRole.DEPARTMENT_ADMIN && role != User.UserRole.DEPARTMENT_PA
                && actorUser == null && departmentId == null) {
            return null;
        }
        if (actorUser != null && actorUser.getRole() == User.UserRole.DEPARTMENT_ADMIN) {
            Department actorDepartment = requireActorDepartment(actorUser);
            if (departmentId != null && !departmentId.equals(actorDepartment.getId())) {
                throw new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                        "Department Admin cannot create users outside their department", 403);
            }
            ensureActiveDepartment(actorDepartment);
            return actorDepartment;
        }
        if (departmentId == null) {
            throw new MeghaConnectException(
                    ErrorCodeConstants.MISSING_REQUIRED_FIELD,
                    ErrorCodeConstants.format(ErrorCodeConstants.MISSING_REQUIRED_FIELD_MSG, "departmentId"),
                    400);
        }
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new MeghaConnectException(ErrorCodeConstants.CONTENT_NOT_FOUND,
                        "Department not found: " + departmentId, 404));
        ensureActiveDepartment(department);
        return department;
    }

    private Department resolveDepartmentForUpdate(Long departmentId, User.UserRole role, User actorUser, User targetUser) {
        if (role == User.UserRole.SUPER_ADMIN) {
            return null;
        }
        if (role != User.UserRole.DEPARTMENT_ADMIN && role != User.UserRole.DEPARTMENT_PA
                && actorUser == null && departmentId == null) {
            return targetUser.getDepartment();
        }
        if (actorUser != null && actorUser.getRole() == User.UserRole.DEPARTMENT_ADMIN) {
            Department actorDepartment = requireActorDepartment(actorUser);
            if (targetUser.getDepartment() == null || !actorDepartment.getId().equals(targetUser.getDepartment().getId())) {
                throw new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                        "Department Admin cannot modify users outside their department", 403);
            }
            if (departmentId != null && !departmentId.equals(actorDepartment.getId())) {
                throw new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                        "Department Admin cannot change department assignment", 403);
            }
            ensureActiveDepartment(actorDepartment);
            return actorDepartment;
        }
        if (departmentId == null) {
            return targetUser.getDepartment();
        }
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new MeghaConnectException(ErrorCodeConstants.CONTENT_NOT_FOUND,
                        "Department not found: " + departmentId, 404));
        ensureActiveDepartment(department);
        return department;
    }

    private Department requireActorDepartment(User actorUser) {
        if (actorUser.getDepartment() == null) {
            throw new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                    "Authenticated user has no department assignment", 403);
        }
        return actorUser.getDepartment();
    }

    private void ensureActiveDepartment(Department department) {
        if (department.getStatus() != Department.DepartmentStatus.ACTIVE) {
            throw new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                    "Department is inactive", 403);
        }
    }
}
