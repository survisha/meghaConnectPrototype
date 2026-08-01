package com.survisha.meghaconnect.security;

import com.survisha.meghaconnect.entity.User;
import com.survisha.meghaconnect.util.DateTimeUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, null);
    }

    public String generateToken(UserDetails userDetails, User user) {
        Map<String, Object> claims = new HashMap<>();
        
        // Extract the primary role/authority as a string
        String role = userDetails.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .findFirst()
            .orElse("ROLE_PUBLIC");
        
        // Store role as a simple string (remove ROLE_ prefix for cleaner storage)
        String cleanRole = role.replace("ROLE_", "");
        claims.put("role", cleanRole);
        
        // Also store the full authority for backward compatibility
        claims.put("authority", role);

        if (user != null) {
            claims.put("userId", user.getId());
            claims.put("passwordChangeRequired", user.isPasswordChangeRequired());
            claims.put("credentialsVersion", user.getCredentialsVersion());
            if (user.getDepartment() != null) {
                claims.put("departmentId", user.getDepartment().getId());
                claims.put("departmentCode", user.getDepartment().getDepartmentCode());
            }
        }
        
        LocalDateTime issuedAtIst = DateTimeUtil.nowIST();
        Date issuedAt = toJwtDate(issuedAtIst);
        Date expiresAt = toJwtDate(issuedAtIst.plus(Duration.ofMillis(jwtExpirationMs)));

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(issuedAt)
            .setExpiration(expiresAt)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
            .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isTokenValid(String token, UserDetails userDetails, User user) {
        if (!isTokenValid(token, userDetails)) {
            return false;
        }
        Object claim = claims(token).get("credentialsVersion");
        long tokenVersion = claim instanceof Number ? ((Number) claim).longValue() : 0L;
        return user == null || tokenVersion == user.getCredentialsVersion();
    }

    private Claims claims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token).getBody();
    }

    private boolean isTokenExpired(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
            .parseClaimsJws(token).getBody().getExpiration().before(toJwtDate(DateTimeUtil.nowIST()));
    }

    private SecretKey getSigningKey() {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        } catch (RuntimeException ex) {
            return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        }
    }

    private Date toJwtDate(LocalDateTime istDateTime) {
        return Date.from(istDateTime.atZone(DateTimeUtil.IST_ZONE).toInstant());
    }
}
