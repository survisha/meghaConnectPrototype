package com.survisha.meghaconnect.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Single source of truth for endpoints executed before authentication.
 * Keep the HTTP method on each matcher so similarly named protected routes are
 * not accidentally exposed.
 */
@Component
public class PublicEndpointRequestMatcher implements RequestMatcher {

    private final RequestMatcher delegate = new OrRequestMatcher(List.of(
        post("/api/v1/auth/login"),
        post("/api/v1/auth/generate-otp"),
        post("/api/v1/auth/validate-otp"),
        post("/api/v1/auth/resend-otp"),
        post("/api/v1/auth/forgot-password"),
        post("/api/v1/auth/reset-password"),
        get("/api/v1/captcha/generate"),
        post("/api/v1/captcha/validate"),

        // Citizen registration/login aliases used by the current web client.
        post("/api/v1/visitor/auth/check-mobile"),
        post("/api/v1/visitor/auth/check-registration"),
        post("/api/v1/visitor/auth/search-registrations"),
        post("/api/v1/visitor/auth/generate-otp"),
        post("/api/v1/visitor/auth/validate-otp"),
        post("/api/v1/visitor/auth/register")
    ));

    @Override
    public boolean matches(HttpServletRequest request) {
        return delegate.matches(request);
    }

    private static RequestMatcher get(String path) {
        return new AntPathRequestMatcher(path, HttpMethod.GET.name());
    }

    private static RequestMatcher post(String path) {
        return new AntPathRequestMatcher(path, HttpMethod.POST.name());
    }
}
