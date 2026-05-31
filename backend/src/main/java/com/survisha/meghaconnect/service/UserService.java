package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.CreateUserRequest;
import com.survisha.meghaconnect.dto.UpdateUserRequest;
import com.survisha.meghaconnect.dto.UserResponse;
import com.survisha.meghaconnect.entity.User;
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
        String phoneNumber = trimToNull(request.getPhoneNumber());
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

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .role(request.getRole())
                .phoneNumber(phoneNumber)
                .active(request.getActive() == null || Boolean.TRUE.equals(request.getActive()))
                .offlineAccess(Boolean.TRUE.equals(request.getOfflineAccess()))
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
                .role(user.getRole())
                .phoneNumber(user.getPhoneNumber())
                .active(user.isActive())
                .locked(user.isLocked())
                .offlineAccess(user.isOfflineAccess())
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
        String phoneNumber = trimToNull(request.getPhoneNumber());
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
        user.setFullName(fullName);
        user.setRole(request.getRole());
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
}
