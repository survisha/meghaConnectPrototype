package com.survisha.meghaconnect.security;

import com.survisha.meghaconnect.util.DateTimeUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
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

    private boolean isTokenExpired(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
            .parseClaimsJws(token).getBody().getExpiration().before(toJwtDate(DateTimeUtil.nowIST()));
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    private Date toJwtDate(LocalDateTime istDateTime) {
        return Date.from(istDateTime.atZone(DateTimeUtil.IST_ZONE).toInstant());
    }
}
