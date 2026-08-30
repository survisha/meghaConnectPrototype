package com.survisha.meghaconnect.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import com.survisha.meghaconnect.repository.UserRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PublicEndpointRequestMatcher publicEndpoints;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String applicationPath = contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)
                ? requestUri.substring(contextPath.length()) : requestUri;
        boolean skip = applicationPath.startsWith("/actuator/") || publicEndpoints.matches(request);
        if (skip) {
            log.debug("[SECURITY] JWT skipped method={} path={} publicEndpoint=true",
                request.getMethod(), request.getRequestURI());
        }
        return skip;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("[SECURITY] JWT filter path={} authorizationPresent={} bearerHeader={}",
                request.getRequestURI(), authHeader != null, false);
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        log.debug("[SECURITY] JWT filter path={} authorizationPresent=true bearerHeader=true tokenLength={}",
            request.getRequestURI(), jwt.length());
        try {
            final String username = jwtService.extractUsername(jwt);
            log.debug("[SECURITY] JWT extracted subject={} path={}", username, request.getRequestURI());
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                com.survisha.meghaconnect.entity.User appUser = username.startsWith("visitor_") ? null
                        : userRepository.findByNormalizedUsername(username).orElse(null);
                boolean tokenValid = jwtService.isTokenValid(jwt, userDetails, appUser);
                log.debug("[SECURITY] JWT validation result path={} userFound=true tokenValid={} authorities={}",
                    request.getRequestURI(), tokenValid, userDetails.getAuthorities());
                if (tokenValid) {
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("[SECURITY] JWT authenticated username={} securityContextAuthenticated=true authorities={}",
                        username, userDetails.getAuthorities());
                }
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            log.warn("[SECURITY] JWT validation failed path={} reason={} message={}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
