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

    private static final long DEFAULT_USER_TTL_MS = 3_600_000L; // 1 hour
    private static final long DEFAULT_ADMIN_TTL_MS = 86_400_000L; // 24 hours
    private static final long MIN_USER_TTL_MS = 60_000L; // 1 minute
    private static final long MIN_ADMIN_TTL_MS = 600_000L; // 10 minutes

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
        long ttlMs = normalizeTtl(jwtExpirationMs, DEFAULT_USER_TTL_MS, MIN_USER_TTL_MS, "user");
        return generateToken(username, role, ttlMs);
    }

    public String generateAdminToken(String username) {
        long ttlMs = normalizeTtl(adminJwtExpirationMs, DEFAULT_ADMIN_TTL_MS, MIN_ADMIN_TTL_MS, "admin");
        return generateToken(username, "ROLE_ADMIN", ttlMs);
    }

    private long normalizeTtl(Long configured, long fallback, long min, String tokenType) {
        if (configured == null || configured <= 0L) {
            log.warn("{} JWT expiration not configured or invalid. Using fallback: {}ms", tokenType, fallback);
            return fallback;
        }
        if (configured < min) {
            log.warn("{} JWT expiration too low ({}ms). Using fallback: {}ms", tokenType, configured, fallback);
            return fallback;
        }
        return configured;
    }

    private String generateToken(String username, String role, long ttlMs) {
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
        long ttlMs = normalizeTtl(jwtExpirationMs, DEFAULT_USER_TTL_MS, MIN_USER_TTL_MS, "user");
        return Math.max(1L, ttlMs / 1000L);
    }
}
