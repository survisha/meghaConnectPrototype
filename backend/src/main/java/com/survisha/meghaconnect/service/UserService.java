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
import com.survisha.meghaconnect.monitoring.MonitoredOperation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import com.survisha.meghaconnect.repository.DepartmentAccessRequestRepository;
import com.survisha.meghaconnect.entity.DepartmentAccessRequest;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final com.survisha.meghaconnect.security.AccessPolicy accessPolicy;

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final DepartmentAccessRequestRepository departmentAccessRequestRepository;

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

    @MonitoredOperation(value = "department_user_load", category = MonitoredOperation.Category.DATABASE)
    public List<UserResponse> getUserResponsesForActor(String actor) {
        User currentUser = requireActor(actor);
        if (currentUser.getRole() == User.UserRole.DEPARTMENT_ADMIN
                && currentUser.getDepartment() != null) {
            return userRepository.findByDepartment_Id(currentUser.getDepartment().getId()).stream()
                    .map(this::toResponse)
                    .toList();
        }
        if (currentUser.getRole() == User.UserRole.SUPER_ADMIN || currentUser.getRole() == User.UserRole.ADMIN) {
            return getAllUserResponses();
        }
        throw new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                ErrorCodeConstants.UNAUTHORIZED_ACCESS_MSG, 403);
    }

    @MonitoredOperation(value = "department_user_load", category = MonitoredOperation.Category.DATABASE)
    public Page<UserResponse> getUserResponsesForActor(
            String actor, String search, User.UserRole role, Boolean active, Boolean locked,
            Long requestedDepartmentId, Pageable pageable) {
        User currentUser = requireActor(actor);
        Long scopedDepartmentId = requestedDepartmentId;
        if (currentUser.getRole() == User.UserRole.DEPARTMENT_ADMIN) {
            Department department = currentUser.getDepartment();
            if (department == null || department.getId() == null) {
                throw new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                        "Department Admin is not assigned to a department", 403);
            }
            if (department.getStatus() != Department.DepartmentStatus.ACTIVE) {
                throw new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                        "Department is inactive", 403);
            }
            if (requestedDepartmentId != null && !requestedDepartmentId.equals(department.getId())) {
                throw new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                        ErrorCodeConstants.UNAUTHORIZED_ACCESS_MSG, 403);
            }
            scopedDepartmentId = department.getId();
        } else if (currentUser.getRole() != User.UserRole.SUPER_ADMIN
                && currentUser.getRole() != User.UserRole.ADMIN) {
            throw new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                    ErrorCodeConstants.UNAUTHORIZED_ACCESS_MSG, 403);
        }

        Specification<User> specification = Specification.where(null);
        if (scopedDepartmentId != null) {
            Long departmentId = scopedDepartmentId;
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("department").get("id"), departmentId));
        }
        if (role != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        if (active != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        if (locked != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("locked"), locked));
        }
        String term = trimToNull(search);
        if (term != null) {
            String pattern = "%" + term.toLowerCase(java.util.Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("username")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(root.get("phoneNumber"), pattern)));
        }
        return userRepository.findAll(specification, pageable).map(this::toResponse);
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
        return userRepository.findByNormalizedUsername(username);
    }

    /**
     * Get full name by username
     */
    public String getFullNameByUsername(String username) {
        log.debug("Fetching full name for username: {}", username);
        return userRepository.findByNormalizedUsername(username)
            .map(User::getFullName)
            .orElse(username);
    }

    public UserResponse getUserResponseForActor(Long id, String actor) {
        User actorUser = requireActor(actor);
        User target = requireVisibleTarget(id, actorUser);
        return toResponse(target);
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
        String username = normalizeUsername(request.getUsername());
        String fullName = trimToNull(request.getFullName());
        String password = trimToNull(request.getPassword());
        String email = trimToNull(request.getEmail());
        String phoneNumber = trimToNull(request.getPhoneNumber());
        User actorUser = userRepository.findByNormalizedUsername(actor).orElse(null);
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
        if (userRepository.existsByNormalizedUsername(username)) {
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
                .temporaryPasswordCreatedAt(com.survisha.meghaconnect.util.DateTimeUtil.nowIST())
                .build();
        user.setCreatedBy(actor);
        user.setUpdatedBy(actor);
        User saved = userRepository.save(user);
        log.info("User created username={} role={} by={}", saved.getUsername(), saved.getRole(), actor);
        auditLogService.log("USER", saved.getId(), "USER_CREATED",
                "User created with temporary credentials", actor);
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
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lastFailedLoginAt(user.getLastFailedLoginAt())
                .lockedAt(user.getLockedAt())
                .lockReason(user.getLockReason())
                .passwordChangedAt(user.getPasswordChangedAt())
                .temporaryPasswordCreatedAt(user.getTemporaryPasswordCreatedAt())
                .unlockedBy(user.getUnlockedBy())
                .unlockedAt(user.getUnlockedAt())
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
        User actorUser = requireActor(actor);
        requireVisibleTarget(user, actorUser);
        ensureActorCanManageTarget(actorUser, user);
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
        user.setOfflineAccess(Boolean.TRUE.equals(request.getOfflineAccess()));
        user.setUpdatedBy(actor);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse setActive(Long id, boolean active, String actor) {
        User actorUser = requireActor(actor);
        User user = requireVisibleTarget(id, actorUser);
        ensureActorCanManageTarget(actorUser, user);
        user.setActive(active);
        user.setUpdatedBy(actor);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse unlockUser(Long id, String actor) {
        User actorUser = requireActor(actor);
        User user = requireVisibleTarget(id, actorUser);
        ensureActorCanUnlock(actorUser, user);
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLastFailedLoginAt(null);
        user.setLockedAt(null);
        user.setLockReason(null);
        user.setUnlockedBy(actor);
        user.setUnlockedAt(com.survisha.meghaconnect.util.DateTimeUtil.nowIST());
        user.setUpdatedBy(actor);
        User saved = userRepository.save(user);
        auditLogService.log("USER", saved.getId(), "ACCOUNT_UNLOCKED", "Account unlocked", actor);
        return toResponse(saved);
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

    private String normalizeUsername(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase();
    }

    private void validateCreatorCanAssignRole(User actorUser, User.UserRole role) {
        if (actorUser == null) {
            return;
        }
        if (accessPolicy.canAssignRole(actorUser, role)) return;
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
        if (role == User.UserRole.DEPARTMENT_ADMIN
                && !departmentAccessRequestRepository.existsByDepartmentCodeIgnoreCaseAndRequestStatus(
                    department.getDepartmentCode(), DepartmentAccessRequest.Status.APPROVED)) {
            throw new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                    "Department access request is not approved", 403);
        }
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

    private User requireActor(String actor) {
        return userRepository.findByNormalizedUsername(actor)
                .orElseThrow(() -> new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                        ErrorCodeConstants.UNAUTHORIZED_ACCESS_MSG, 403));
    }

    private User requireVisibleTarget(Long id, User actor) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new MeghaConnectException(ErrorCodeConstants.USER_NOT_FOUND,
                        ErrorCodeConstants.format(ErrorCodeConstants.USER_NOT_FOUND_MSG, id), 404));
        return requireVisibleTarget(target, actor);
    }

    private User requireVisibleTarget(User target, User actor) {
        if (actor.getRole() == User.UserRole.SUPER_ADMIN || actor.getRole() == User.UserRole.ADMIN) {
            return target;
        }
        if (actor.getRole() == User.UserRole.DEPARTMENT_ADMIN
                && actor.getDepartment() != null && target.getDepartment() != null
                && actor.getDepartment().getId().equals(target.getDepartment().getId())) {
            return target;
        }
        throw new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                ErrorCodeConstants.UNAUTHORIZED_ACCESS_MSG, 403);
    }

    private void ensureActorCanManageTarget(User actor, User target) {
        if (target.getRole() == User.UserRole.SUPER_ADMIN && actor.getRole() != User.UserRole.SUPER_ADMIN) {
            throw new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                    "Only Super Admin can manage a Super Admin account", 403);
        }
    }

    private void ensureActorCanUnlock(User actor, User target) {
        ensureActorCanManageTarget(actor, target);
        if (actor.getRole() == User.UserRole.DEPARTMENT_ADMIN
                && (target.getRole() == User.UserRole.DEPARTMENT_ADMIN
                    || target.getRole() == User.UserRole.SUPER_ADMIN)) {
            throw new MeghaConnectException(ErrorCodeConstants.UNAUTHORIZED_ACCESS,
                    "Department Admin cannot unlock this account", 403);
        }
    }
}
