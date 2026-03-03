package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.dto.admin.AdminUserUpdateRequest;
import com.mehmetkatr.project_easy_translate.entity.Admin;
import com.mehmetkatr.project_easy_translate.entity.AdminActionLog;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.exception.ResourceConflictException;
import com.mehmetkatr.project_easy_translate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserManagementService {

    private final UserRepository userRepository;
    private final AdminService adminService;
    private final AdminActionLogService adminActionLogService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.account.deletion.grace-days:7}")
    private int accountDeletionGraceDays;

    @Transactional(readOnly = true)
    public List<User> listUsers(String query, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        if (!normalized.isBlank()) {
            users = users.stream()
                    .filter(u -> containsIgnoreCase(u.getUsername(), normalized) || containsIgnoreCase(u.getEmail(), normalized))
                    .toList();
        }
        if (users.size() <= safeLimit) {
            return users;
        }
        return users.subList(0, safeLimit);
    }

    public User updateUser(String adminIdentifier, Long userId, AdminUserUpdateRequest request) {
        Admin admin = adminService.getByIdentifierOrThrow(adminIdentifier);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getUsername() != null) {
            String nextUsername = normalizeUsername(request.getUsername());
            if (nextUsername.isBlank()) {
                throw new IllegalArgumentException("Username cannot be blank");
            }
            Optional<User> usernameConflict = userRepository.findFirstByUsernameIgnoreCase(nextUsername);
            if (usernameConflict.isPresent() && !usernameConflict.get().getId().equals(user.getId())) {
                throw new ResourceConflictException("USERNAME_ALREADY_EXISTS", "This username is already in use");
            }
            user.setUsername(nextUsername);
        }

        if (request.getEmail() != null) {
            String nextEmail = normalizeEmail(request.getEmail());
            if (nextEmail.isBlank()) {
                throw new IllegalArgumentException("Email cannot be blank");
            }
            Optional<User> emailConflict = userRepository.findFirstByEmailIgnoreCase(nextEmail);
            if (emailConflict.isPresent() && !emailConflict.get().getId().equals(user.getId())) {
                throw new ResourceConflictException("EMAIL_ALREADY_EXISTS", "This email is already in use");
            }
            user.setEmail(nextEmail);
        }

        if (request.getPassword() != null) {
            String nextPassword = request.getPassword().trim();
            if (nextPassword.length() < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters");
            }
            user.setPasswordHash(passwordEncoder.encode(nextPassword));
        }

        if (request.getSubscriptionLevel() != null && user.getSubscriptionLevel() != request.getSubscriptionLevel()) {
            user.setSubscriptionLevel(request.getSubscriptionLevel());
            adminActionLogService.logAction(admin, user, AdminActionLog.AdminActionType.UPDATE_SUBSCRIPTION);
        }

        if (request.getTokenBalance() != null && user.getTokenBalance() != request.getTokenBalance()) {
            if (request.getTokenBalance() < 0) {
                throw new IllegalArgumentException("Token balance cannot be negative");
            }
            user.setTokenBalance(request.getTokenBalance());
            adminActionLogService.logAction(admin, user, AdminActionLog.AdminActionType.UPDATE_TOKEN_LIMIT);
        }

        if (request.getEmailVerified() != null) {
            user.setEmailVerified(request.getEmailVerified());
            if (Boolean.TRUE.equals(request.getEmailVerified())) {
                user.setEmailVerificationCode(null);
                user.setEmailVerificationExpiresAt(null);
            }
        }

        if (request.getOnboardingCompleted() != null) {
            user.setOnboardingCompleted(request.getOnboardingCompleted());
        }

        if (request.getPendingDeletion() != null && user.isPendingDeletion() != request.getPendingDeletion()) {
            LocalDateTime now = LocalDateTime.now();
            if (request.getPendingDeletion()) {
                user.setPendingDeletion(true);
                user.setDeletionRequestedAt(now);
                user.setDeletionScheduledAt(now.plusDays(Math.max(1, accountDeletionGraceDays)));
                adminActionLogService.logAction(admin, user, AdminActionLog.AdminActionType.SUSPEND_USER);
            } else {
                user.setPendingDeletion(false);
                user.setDeletionRequestedAt(null);
                user.setDeletionScheduledAt(null);
                adminActionLogService.logAction(admin, user, AdminActionLog.AdminActionType.REACTIVATE_USER);
            }
        }

        return userRepository.save(user);
    }

    public void deleteUser(String adminIdentifier, Long userId) {
        Admin admin = adminService.getByIdentifierOrThrow(adminIdentifier);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        adminActionLogService.logAction(admin, null, AdminActionLog.AdminActionType.DELETE_USER);
        userRepository.delete(user);
    }

    private boolean containsIgnoreCase(String value, String normalizedQuery) {
        if (value == null) return false;
        return value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
