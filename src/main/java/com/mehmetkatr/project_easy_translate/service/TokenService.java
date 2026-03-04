package com.mehmetkatr.project_easy_translate.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private Long jwtExpirationMs;
    @Value("${app.admin.jwt.expiration-ms:86400000}")
    private Long adminJwtExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String role) {
        return generateToken(username, role, jwtExpirationMs);
    }

    public String generateAdminToken(String username) {
        return generateToken(username, "ROLE_ADMIN", adminJwtExpirationMs != null ? adminJwtExpirationMs : jwtExpirationMs);
    }

    private String generateToken(String username, String role, Long expirationMs) {
        long ttlMs = expirationMs != null ? expirationMs : 3600000L;
        if (ttlMs < 1000L) {
            ttlMs = 1000L;
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired");
        } catch (JwtException e) {
            log.debug("JWT token invalid");
        }
        return false;
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("role", String.class);
    }

    public long getAccessTokenExpiresInSeconds() {
        return Math.max(1L, (jwtExpirationMs == null ? 0L : jwtExpirationMs) / 1000L);
    }
}
