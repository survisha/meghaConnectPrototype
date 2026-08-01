package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.AuthRequest;
import com.survisha.meghaconnect.dto.AuthResponse;
import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.security.JwtService;
import com.survisha.meghaconnect.exception.*;
import com.survisha.meghaconnect.repository.UserRepository;
import com.survisha.meghaconnect.captcha.CaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import com.survisha.meghaconnect.dto.ChangeTemporaryPasswordRequest;
import com.survisha.meghaconnect.util.DateTimeUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final CaptchaService captchaService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    /**
     * Authenticate user and generate JWT token
     */
    public AuthResponse login(AuthRequest request) {
        captchaService.validateForLogin(request.getCaptchaId(), request.getCaptchaValue());
        String username = normalizeUsername(request.getUsername());
        log.info("[AUTH] Login attempt - Username: {}", username);
        
        try {
            log.debug("[AUTH] Starting authentication for user: {}", username);
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );
            log.info("[AUTH] Authentication successful for user: {}", username);
            loginAttemptService.recordSuccess(username);
            
            UserDetails user = userDetailsService.loadUserByUsername(username);
            User appUser = userRepository.findByNormalizedUsername(username)
                .orElseThrow(() -> new MeghaConnectException(
                    ErrorCodeConstants.USER_NOT_FOUND,
                    ErrorCodeConstants.format(ErrorCodeConstants.USER_NOT_FOUND_MSG, username),
                    404
                ));
            if (appUser.getRole() != User.UserRole.SUPER_ADMIN
                    && appUser.getDepartment() != null
                    && appUser.getDepartment().getStatus() != com.survisha.meghaconnect.entity.Department.DepartmentStatus.ACTIVE) {
                throw new MeghaConnectException(
                    ErrorCodeConstants.USER_ACCOUNT_INACTIVE,
                    "User department is inactive",
                    403
                );
            }
            log.debug("[AUTH] UserDetails loaded - Username: {}, Authorities: {}", 
                user.getUsername(), user.getAuthorities());
            
            String token = jwtService.generateToken(user, appUser);
            log.debug("[AUTH] JWT token generated for user: {}", username);
            
            String fullName = userService.getFullNameByUsername(username);
            
            String role = user.getAuthorities().iterator().next().getAuthority();
            
            AuthResponse response = AuthResponse.builder()
                .token(token)
                .accessToken(token)
                .tokenType("Bearer")
                .username(user.getUsername())
                .fullName(fullName)
                .role(role)
                .userId(appUser.getId())
                .departmentId(appUser.getDepartment() != null ? appUser.getDepartment().getId() : null)
                .departmentCode(appUser.getDepartment() != null ? appUser.getDepartment().getDepartmentCode() : null)
                .departmentName(appUser.getDepartment() != null ? appUser.getDepartment().getDepartmentName() : null)
                .passwordChangeRequired(appUser.isPasswordChangeRequired())
                .expiresIn(86400L)
                .build();
            
            log.info("[AUTH] Login successful - Username: {}, Role: {}, FullName: {}", 
                username, response.getRole(), fullName);
            
            return response;
        } catch (BadCredentialsException e) {
            log.warn("[AUTH] Failed login username={} reason=invalid_credentials", username);
            if (loginAttemptService.recordFailure(username)) {
                throw new MeghaConnectException(
                        ErrorCodeConstants.USER_ACCOUNT_LOCKED,
                        ErrorCodeConstants.USER_ACCOUNT_LOCKED_MSG,
                        423);
            }
            throw new MeghaConnectException(
                ErrorCodeConstants.INVALID_CREDENTIALS,
                ErrorCodeConstants.INVALID_CREDENTIALS_MSG,
                401
            );
        } catch (LockedException e) {
            log.warn("[AUTH] Failed login username={} reason=account_locked", username);
            throw new MeghaConnectException(
                ErrorCodeConstants.USER_ACCOUNT_LOCKED,
                ErrorCodeConstants.USER_ACCOUNT_LOCKED_MSG,
                423
            );
        } catch (DisabledException e) {
            log.warn("[AUTH] Failed login username={} reason=account_inactive", username);
            throw new MeghaConnectException(
                ErrorCodeConstants.USER_ACCOUNT_INACTIVE,
                ErrorCodeConstants.USER_ACCOUNT_INACTIVE_MSG,
                403
            );
        } catch (AuthenticationException e) {
            log.warn("[AUTH] Failed login username={} reason=authentication_failed type={}",
                username, e.getClass().getSimpleName());
            throw new MeghaConnectException(
                ErrorCodeConstants.INVALID_CREDENTIALS,
                ErrorCodeConstants.INVALID_CREDENTIALS_MSG,
                401
            );
        } catch (MeghaConnectException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("[AUTH] Login failed for user: {} - Error: {} - Message: {}",
                username, e.getClass().getSimpleName(), e.getMessage());
            throw new MeghaConnectException(
                ErrorCodeConstants.UNEXPECTED_ERROR,
                ErrorCodeConstants.UNEXPECTED_ERROR_MSG,
                500
            );
        }
    }

    @Transactional
    public void changeTemporaryPassword(String username, ChangeTemporaryPasswordRequest request) {
        User user = userRepository.findForLoginUpdate(username)
                .orElseThrow(() -> new MeghaConnectException(ErrorCodeConstants.USER_NOT_FOUND,
                        ErrorCodeConstants.USER_NOT_FOUND_MSG, 404));
        if (!user.isPasswordChangeRequired()) {
            throw new MeghaConnectException(ErrorCodeConstants.GENERAL_ERROR,
                    "Temporary password change is not required", 409);
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new MeghaConnectException(ErrorCodeConstants.INVALID_CREDENTIALS,
                    "Current password is incorrect", 400);
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new MeghaConnectException(ErrorCodeConstants.GENERAL_ERROR,
                    "New password must differ from the temporary password", 400);
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangeRequired(false);
        user.setPasswordChangedAt(DateTimeUtil.nowIST());
        user.setCredentialsVersion(user.getCredentialsVersion() + 1);
        user.setUpdatedBy(username);
        userRepository.save(user);
        auditLogService.log("USER", user.getId(), "FIRST_PASSWORD_CHANGED",
                "Initial temporary password changed", username);
    }

    private String normalizeUsername(String username) {
        return username == null ? null : username.trim();
    }
}
