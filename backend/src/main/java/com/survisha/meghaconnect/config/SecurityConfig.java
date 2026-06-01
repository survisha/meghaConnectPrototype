package com.survisha.meghaconnect.config;

import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.repository.UserRepository;
import com.survisha.meghaconnect.repository.VisitorRepository;
import com.survisha.meghaconnect.security.ApiRateLimitFilter;
import com.survisha.meghaconnect.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Collections;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final VisitorRepository visitorRepository;

    @Value("${meghaconnect.security.require-https:false}")
    private boolean requireHttps;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthFilter,
                                           ApiRateLimitFilter apiRateLimitFilter,
                                           CorsConfigurationSource corsConfigurationSource) throws Exception {
        if (requireHttps) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        return http
            // Enable CORS with custom configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // CSRF is disabled because this is a stateless REST API that uses
            // JWT Bearer tokens in the Authorization header (not session cookies).
            // CSRF attacks exploit cookie-based authentication, which is not used here.
            // The frontend SPA sends JWT in Authorization header, not via cookies.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Allow all OPTIONS requests (CORS preflight)
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Public API endpoints. Sensitive visitor, KYC, AI, QR, and file APIs are handled by JWT/RBAC.
                .antMatchers(
                    "/api/v1/auth/**",
                    "/error",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/metrics/**",
                    "/actuator/prometheus",
                    "/api/actuator/health/**",
                    "/api/actuator/info",
                    "/api/actuator/metrics/**",
                    "/api/actuator/prometheus",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/webjars/**",
                    "/api/swagger-ui.html",
                    "/api/swagger-ui/**",
                    "/api/v3/api-docs/**",
                    "/api/webjars/**"
                ).permitAll()
                .antMatchers("/api/v1/guest-appointments").permitAll() // Public guest appointment requests
                .antMatchers(
                    "/api/v1/visitor/auth/check-mobile",
                    "/api/v1/visitor/auth/check-registration",
                    "/api/v1/visitor/auth/search-registrations",
                    "/api/v1/visitor/auth/generate-otp",
                    "/api/v1/visitor/auth/validate-otp",
                    "/api/v1/visitor/auth/register"
                ).permitAll()
                .antMatchers(
                    "/api/v1/kyc/verify/epic",
                    "/api/v1/kyc/aadhaar/generate-qr",
                    "/api/v1/kyc/aadhaar/result/**",
                    "/api/v1/kyc/aadhaar/kycResults"
                ).permitAll()
                .antMatchers("/api/v1/reference/**").permitAll() // Public reference data dropdowns
                .antMatchers("/api/ai/chatbot", "/api/v1/ai/chatbot").permitAll()
                // All other API requests require authentication
                .antMatchers("/api/**").authenticated()
                
                // Allow frontend routes (Angular handles routing)
                .anyRequest().permitAll()
            )
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(apiRateLimitFilter, JwtAuthenticationFilter.class)
            .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return (String username) -> {
            log.debug("[SECURITY] Loading user details for username: {}", username);

            if (username != null && username.startsWith("visitor_")) {
                Long visitorId = parseVisitorId(username);
                if (visitorId != null && visitorRepository.existsById(visitorId)) {
                    return org.springframework.security.core.userdetails.User.builder()
                        .username(username)
                        .password("VISITOR_JWT_AUTH")
                        .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_PUBLIC")))
                        .build();
                }
                log.warn("[SECURITY] Visitor principal not found: {}", username);
                throw new UsernameNotFoundException("Visitor not found: " + username);
            }
            
            User u = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[SECURITY] User not found in database: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
            
            log.debug("[SECURITY] User found - Username: {}, Role: {}, Active: {}, Locked: {}",
                u.getUsername(), u.getRole(), u.isActive(), u.isLocked());
            
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(u.getUsername())
                .password(u.getPasswordHash())
                .disabled(!u.isActive())
                .accountLocked(u.isLocked())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())))
                .build();
            
            log.debug("[SECURITY] UserDetails created - Authorities: {}", userDetails.getAuthorities());
            return userDetails;
        };
    }

    private Long parseVisitorId(String username) {
        try {
            return Long.parseLong(username.substring("visitor_".length()));
        } catch (Exception e) {
            return null;
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
