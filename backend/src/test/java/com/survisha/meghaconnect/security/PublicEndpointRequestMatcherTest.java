package com.survisha.meghaconnect.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicEndpointRequestMatcherTest {

    private final PublicEndpointRequestMatcher matcher = new PublicEndpointRequestMatcher();

    @ParameterizedTest
    @CsvSource({
        "POST,/api/v1/auth/login",
        "POST,/api/v1/auth/generate-otp",
        "POST,/api/v1/auth/validate-otp",
        "POST,/api/v1/auth/resend-otp",
        "POST,/api/v1/auth/forgot-password",
        "POST,/api/v1/auth/reset-password",
        "GET,/api/v1/captcha/generate",
        "POST,/api/v1/captcha/validate"
    })
    void requiredPreAuthenticationEndpointsArePublic(String method, String path) {
        assertTrue(matcher.matches(request(method, path)));
    }

    @ParameterizedTest
    @CsvSource({
        "GET,/api/v1/users",
        "GET,/api/v1/admin/users",
        "GET,/api/v1/appointments",
        "GET,/api/v1/dashboard",
        "GET,/api/v1/auth/validate-otp",
        "POST,/api/v1/captcha/generate"
    })
    void protectedOrWrongMethodRequestsAreNotPublic(String method, String path) {
        assertFalse(matcher.matches(request(method, path)));
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
