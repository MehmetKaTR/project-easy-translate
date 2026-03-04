package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.RefreshToken;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.exception.InvalidRefreshTokenException;
import com.mehmetkatr.project_easy_translate.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public record RotationResult(User user, String newRefreshToken) {}

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.auth.refresh-token-ttl-days:30}")
    private int refreshTokenTtlDays;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String issueRefreshToken(User user, String ip, String userAgent) {
        revokeAllActiveForUser(user, ip, userAgent);
        String raw = generateToken();
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(raw))
                .expiresAt(LocalDateTime.now().plusDays(Math.max(1, refreshTokenTtlDays)))
                .createdIp(normalizeIp(ip))
                .createdUserAgent(normalizeUserAgent(userAgent))
                .build();
        refreshTokenRepository.save(entity);
        return raw;
    }

    @Transactional
    public RotationResult rotateRefreshToken(String rawToken, String ip, String userAgent) {
        RefreshToken current = findActiveToken(rawToken);
        User user = current.getUser();
        String newRaw = generateToken();
        String newHash = hash(newRaw);
        LocalDateTime now = LocalDateTime.now();

        current.setRevokedAt(now);
        current.setRevokedIp(normalizeIp(ip));
        current.setRevokedUserAgent(normalizeUserAgent(userAgent));
        current.setReplacedByTokenHash(newHash);
        refreshTokenRepository.save(current);

        RefreshToken replacement = RefreshToken.builder()
                .user(user)
                .tokenHash(newHash)
                .expiresAt(now.plusDays(Math.max(1, refreshTokenTtlDays)))
                .createdIp(normalizeIp(ip))
                .createdUserAgent(normalizeUserAgent(userAgent))
                .build();
        refreshTokenRepository.save(replacement);

        return new RotationResult(user, newRaw);
    }

    @Transactional
    public void revokeByRawToken(String rawToken, String ip, String userAgent) {
        if (rawToken == null || rawToken.isBlank()) return;
        String tokenHash = hash(rawToken.trim());
        RefreshToken token = refreshTokenRepository.findFirstByTokenHash(tokenHash).orElse(null);
        if (token == null || token.getRevokedAt() != null) return;
        token.setRevokedAt(LocalDateTime.now());
        token.setRevokedIp(normalizeIp(ip));
        token.setRevokedUserAgent(normalizeUserAgent(userAgent));
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void revokeAllActiveForUser(User user, String ip, String userAgent) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserAndRevokedAtIsNullAndExpiresAtAfter(user, LocalDateTime.now());
        if (activeTokens.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        String normalizedIp = normalizeIp(ip);
        String normalizedUa = normalizeUserAgent(userAgent);
        for (RefreshToken token : activeTokens) {
            token.setRevokedAt(now);
            token.setRevokedIp(normalizedIp);
            token.setRevokedUserAgent(normalizedUa);
        }
        refreshTokenRepository.saveAll(activeTokens);
    }

    private RefreshToken findActiveToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is missing");
        }
        String tokenHash = hash(rawToken.trim());
        RefreshToken token = refreshTokenRepository.findFirstByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is invalid"));
        if (token.getRevokedAt() != null) {
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
        }
        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }
        return token;
    }

    private String generateToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String normalizeIp(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }

    private String normalizeUserAgent(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > 512 ? trimmed.substring(0, 512) : trimmed;
    }
}
