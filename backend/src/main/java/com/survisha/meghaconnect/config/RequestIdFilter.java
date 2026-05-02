package com.survisha.meghaconnect.config;

import com.survisha.meghaconnect.util.RequestContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = (String) request.getAttribute(RequestContextUtil.REQUEST_ID_ATTRIBUTE);
        if (requestId == null || requestId.isBlank()) {
            requestId = RequestContextUtil.resolveRequestId(request.getHeader(RequestContextUtil.REQUEST_ID_HEADER));
            request.setAttribute(RequestContextUtil.REQUEST_ID_ATTRIBUTE, requestId);
        }

        long startTime = System.currentTimeMillis();
        RequestContextUtil.setRequestId(requestId);
        response.setHeader(RequestContextUtil.REQUEST_ID_HEADER, requestId);

        try {
            log.info("HTTP request started method={} path={}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            response.setHeader(RequestContextUtil.REQUEST_ID_HEADER, requestId);
            log.info("HTTP request completed method={} path={} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    System.currentTimeMillis() - startTime);
            RequestContextUtil.clear();
        }
    }
}
