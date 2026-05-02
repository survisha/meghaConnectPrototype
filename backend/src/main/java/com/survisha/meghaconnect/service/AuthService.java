package com.survisha.meghaconnect.service;

import com.survisha.meghaconnect.dto.AuthRequest;
import com.survisha.meghaconnect.dto.AuthResponse;
import com.survisha.meghaconnect.security.JwtService;
import com.survisha.meghaconnect.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserService userService;

    /**
     * Authenticate user and generate JWT token
     */
    public AuthResponse login(AuthRequest request) {
        log.info("[AUTH] Login attempt - Username: {}", request.getUsername());
        
        try {
            log.debug("[AUTH] Starting authentication for user: {}", request.getUsername());
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            log.info("[AUTH] Authentication successful for user: {}", request.getUsername());
            
            UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());
            log.debug("[AUTH] UserDetails loaded - Username: {}, Authorities: {}", 
                user.getUsername(), user.getAuthorities());
            
            String token = jwtService.generateToken(user);
            log.debug("[AUTH] JWT token generated for user: {}", request.getUsername());
            
            String fullName = userService.getFullNameByUsername(request.getUsername());
            
            String role = user.getAuthorities().iterator().next().getAuthority();
            
            AuthResponse response = AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .fullName(fullName)
                .role(role)
                .expiresIn(86400L)
                .build();
            
            log.info("[AUTH] Login successful - Username: {}, Role: {}, FullName: {}", 
                request.getUsername(), response.getRole(), fullName);
            
            return response;
        } catch (BadCredentialsException e) {
            log.error("[AUTH] Login failed for user: {} - Invalid credentials", request.getUsername());
            throw new MeghaConnectException(
                ErrorCodeConstants.INVALID_CREDENTIALS,
                ErrorCodeConstants.INVALID_CREDENTIALS_MSG,
                401
            );
        } catch (Exception e) {
            log.error("[AUTH] Login failed for user: {} - Error: {} - Message: {}", 
                request.getUsername(), e.getClass().getSimpleName(), e.getMessage());
            throw new MeghaConnectException(
                ErrorCodeConstants.INVALID_CREDENTIALS,
                ErrorCodeConstants.INVALID_CREDENTIALS_MSG,
                401
            );
        }
    }
}
