package com.survisha.meghaconnect.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TemporaryPasswordAccessFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getName().startsWith("visitor_")
                && userRepository.findByNormalizedUsername(authentication.getName())
                    .map(user -> user.isPasswordChangeRequired() && !isAllowed(request))
                    .orElse(false)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "errorCode", "PASSWORD_CHANGE_REQUIRED",
                    "message", "Password change is required before accessing the application"));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.endsWith("/api/v1/auth/change-temporary-password")
                || path.startsWith("/api/v1/captcha/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}
