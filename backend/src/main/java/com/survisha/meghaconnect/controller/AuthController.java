package com.survisha.meghaconnect.controller;

import com.survisha.meghaconnect.dto.AuthRequest;
import com.survisha.meghaconnect.dto.AuthResponse;
import com.survisha.meghaconnect.repository.UserRepository;
import com.survisha.meghaconnect.security.JwtService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
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
            
            String fullName = userRepository.findByUsername(request.getUsername())
                    .map(u -> u.getFullName())
                    .orElse(request.getUsername());
            
            AuthResponse response = AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .fullName(fullName)
                .role(user.getAuthorities().iterator().next().getAuthority())
                .expiresIn(86400L)
                .build();
            
            log.info("[AUTH] Login successful - Username: {}, Role: {}, FullName: {}", 
                request.getUsername(), response.getRole(), fullName);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[AUTH] Login failed for user: {} - Error: {} - Message: {}", 
                request.getUsername(), e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }
}
