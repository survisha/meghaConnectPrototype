package com.survisha.meghaconnect.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.response.ErrorResponse;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("[SECURITY] Unauthenticated request method={} path={} reason={}",
            request.getMethod(), request.getRequestURI(), authException.getClass().getSimpleName());

        ErrorResponse error = new ErrorResponse(
            ErrorCodeConstants.USER_NOT_AUTHENTICATED,
            "Authentication is required or the access token is invalid or expired.",
            "AUTH-" + System.nanoTime(),
            HttpStatus.UNAUTHORIZED.value()
        );
        error.setPath(request.getRequestURI());
        error.setRequestId(RequestContextUtil.getRequestId());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), error);
    }
}
