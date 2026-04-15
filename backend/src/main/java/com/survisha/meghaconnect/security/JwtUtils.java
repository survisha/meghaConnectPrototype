package com.survisha.meghaconnect.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * Extract JWT token from HTTP request Authorization header
     */
    public String extractTokenFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            return null;
        } catch (Exception e) {
            log.error("Error extracting token from request", e);
            return null;
        }
    }

    /**
     * Extract username from JWT token
     */
    public String extractUsernameFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Error extracting username from token", e);
            return null;
        }
    }

    /**
     * Get username from HTTP request
     */
    public String getUsernameFromRequest(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                return extractUsernameFromToken(token);
            }
            return null;
        } catch (Exception e) {
            log.error("Error getting username from request", e);
            return null;
        }
    }

    /**
     * Extract role from JWT token
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            
            // First try to get the "role" claim (clean, without ROLE_ prefix)
            Object role = claims.get("role");
            if (role != null && role instanceof String) {
                return (String) role;
            }
            
            // Fallback: try "authority" claim (with ROLE_ prefix)
            Object authority = claims.get("authority");
            if (authority != null && authority instanceof String) {
                String authStr = (String) authority;
                // Remove ROLE_ prefix if present
                return authStr.replace("ROLE_", "");
            }
            
            // Fallback: try "roles" claim (old format - collection)
            Object roles = claims.get("roles");
            if (roles instanceof Collection<?>) {
                Collection<?> rolesList = (Collection<?>) roles;
                if (!rolesList.isEmpty()) {
                    Object firstRole = rolesList.iterator().next();
                    if (firstRole instanceof String) {
                        return ((String) firstRole).replace("ROLE_", "");
                    } else if (firstRole instanceof Map) {
                        Map<?, ?> roleMap = (Map<?, ?>) firstRole;
                        Object roleValue = roleMap.get("authority");
                        if (roleValue != null) {
                            return ((String) roleValue).replace("ROLE_", "");
                        }
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error extracting role from token", e);
            return null;
        }
    }

    /**
     * Get all claims from JWT token
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /**
     * Get signing key from secret
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}
