package com.survisha.meghaconnect.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.exception.ErrorCodeConstants;
import com.survisha.meghaconnect.response.ErrorResponse;
import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.warn("[SECURITY] Access denied method={} path={} principal={} authorities={}",
            request.getMethod(),
            request.getRequestURI(),
            authentication != null ? authentication.getName() : "anonymous",
            authentication != null ? authentication.getAuthorities() : "[]");

        ErrorResponse error = new ErrorResponse(
            ErrorCodeConstants.UNAUTHORIZED_ACCESS,
            "You do not have permission to access this resource.",
            "AUTHZ-" + System.nanoTime(),
            HttpStatus.FORBIDDEN.value()
        );
        error.setPath(request.getRequestURI());
        error.setRequestId(RequestContextUtil.getRequestId());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), error);
    }
}
