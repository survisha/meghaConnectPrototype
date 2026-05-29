package com.survisha.meghaconnect.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ApiRateLimitFilter extends OncePerRequestFilter {

    @Value("${meghaconnect.rate-limit.window-seconds:60}")
    private long windowSeconds;

    @Value("${meghaconnect.rate-limit.otp.max-requests:10}")
    private int otpMaxRequests;

    @Value("${meghaconnect.rate-limit.kyc.max-requests:30}")
    private int kycMaxRequests;

    @Value("${meghaconnect.rate-limit.ai.max-requests:60}")
    private int aiMaxRequests;

    @Value("${meghaconnect.rate-limit.qr.max-requests:120}")
    private int qrMaxRequests;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Limit limit = resolveLimit(request.getRequestURI());
        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = limit.scope + ":" + clientIp(request);
        Bucket bucket = buckets.compute(key, (ignored, existing) -> nextBucket(existing, limit.maxRequests));
        if (bucket.count > limit.maxRequests) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Bucket nextBucket(Bucket existing, int maxRequests) {
        long now = Instant.now().getEpochSecond();
        if (existing == null || now >= existing.resetAtEpochSecond) {
            return new Bucket(1, now + windowSeconds);
        }
        return new Bucket(Math.min(existing.count + 1, maxRequests + 1), existing.resetAtEpochSecond);
    }

    private Limit resolveLimit(String uri) {
        if (uri == null) {
            return null;
        }
        if (uri.startsWith("/api/v1/visitor/auth")
                || uri.startsWith("/api/v1/auth/validate-otp")
                || uri.startsWith("/api/v1/public/otp")) {
            return new Limit("otp", otpMaxRequests);
        }
        if (uri.startsWith("/api/v1/kyc")) {
            return new Limit("kyc", kycMaxRequests);
        }
        if (uri.startsWith("/api/ai")) {
            return new Limit("ai", aiMaxRequests);
        }
        if (uri.startsWith("/api/v1/qr") || uri.startsWith("/api/v1/visitor-pass")) {
            return new Limit("qr", qrMaxRequests);
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.trim().isEmpty() ? realIp.trim() : request.getRemoteAddr();
    }

    private static class Limit {
        private final String scope;
        private final int maxRequests;

        private Limit(String scope, int maxRequests) {
            this.scope = scope;
            this.maxRequests = maxRequests;
        }
    }

    private static class Bucket {
        private final int count;
        private final long resetAtEpochSecond;

        private Bucket(int count, long resetAtEpochSecond) {
            this.count = count;
            this.resetAtEpochSecond = resetAtEpochSecond;
        }
    }
}
