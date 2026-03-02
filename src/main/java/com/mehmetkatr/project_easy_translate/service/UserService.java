package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.dto.auth.AccountDeletionResponse;
import com.mehmetkatr.project_easy_translate.dto.auth.AuthResponse;
import com.mehmetkatr.project_easy_translate.dto.auth.UserPreferencesResponse;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.exception.AccountPendingDeletionException;
import com.mehmetkatr.project_easy_translate.exception.AuthenticationFailedException;
import com.mehmetkatr.project_easy_translate.exception.EmailNotVerifiedException;
import com.mehmetkatr.project_easy_translate.exception.InvalidCodeException;
import com.mehmetkatr.project_easy_translate.exception.ResourceConflictException;
import com.mehmetkatr.project_easy_translate.repository.UserRepository;
import com.mehmetkatr.project_easy_translate.repository.WordListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final WordListRepository wordListRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final EmailDeliveryService emailDeliveryService;

    @Value("${app.account.deletion.grace-days:7}")
    private int accountDeletionGraceDays;

    @Value("${app.auth.email-verification-code-ttl-minutes:15}")
    private int emailVerificationCodeTtlMinutes;

    @Value("${app.auth.password-reset-code-ttl-minutes:15}")
    private int passwordResetCodeTtlMinutes;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> findBySubscriptionLevel(User.SubscriptionLevel subscriptionLevel) {
        return userRepository.findBySubscriptionLevel(subscriptionLevel);
    }

    @Transactional
    public User registerUser(String username, String email, String rawPassword) {
        purgeExpiredPendingDeletions();

        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);

        userRepository.findFirstByEmailIgnoreCase(normalizedEmail)
                .ifPresent(this::throwRegistrationConflictForEmail);

        userRepository.findFirstByUsernameIgnoreCase(normalizedUsername)
                .ifPresent(this::throwRegistrationConflictForUsername);

        User user = User.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .emailVerified(false)
                .subscriptionLevel(User.SubscriptionLevel.FREE)
                .tokenBalance(0)
                .pendingDeletion(false)
                .preferredLanguage(User.PreferredLanguage.EN)
                .themePreference(User.ThemePreference.LIGHT)
                .build();

        User savedUser = userRepository.save(user);
        ensureDefaultWordList(savedUser);

        issueEmailVerificationCode(savedUser);
        emailDeliveryService.sendVerificationCode(savedUser.getEmail(), savedUser.getEmailVerificationCode());

        return savedUser;
    }

    @Transactional
    public User registerOrLoginSocial(String email, String username) {
        purgeExpiredPendingDeletions();

        String normalizedEmail = normalizeEmail(email);
        String normalizedUsername = normalizeUsername(username);

        Optional<User> existingUser = userRepository.findFirstByEmailIgnoreCase(normalizedEmail);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            assertAccountNotPendingDeletion(user);
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                user.setEmailVerificationCode(null);
                user.setEmailVerificationExpiresAt(null);
                userRepository.save(user);
            }
            return user;
        }

        String resolvedUsername = resolveAvailableSocialUsername(normalizedUsername);

        User user = User.builder()
                .username(resolvedUsername)
                .email(normalizedEmail)
                .passwordHash("")
                .emailVerified(true)
                .subscriptionLevel(User.SubscriptionLevel.FREE)
                .tokenBalance(0)
                .pendingDeletion(false)
                .preferredLanguage(User.PreferredLanguage.EN)
                .themePreference(User.ThemePreference.LIGHT)
                .build();

        User savedUser = userRepository.save(user);
        ensureDefaultWordList(savedUser);
        return savedUser;
    }

    @Transactional
    public void register(String username, String email, String rawPassword) {
        registerUser(username, email, rawPassword);
    }

    public AuthResponse login(String username, String rawPassword) {
        String normalizedUsername = normalizeUsername(username);

        User user = userRepository.findFirstByUsernameIgnoreCase(normalizedUsername)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid username or password"));

        assertAccountNotPendingDeletion(user);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid username or password");
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Email is not verified. Please verify your email first.", user.getEmail());
        }

        String token = tokenService.generateToken(user.getUsername(), "ROLE_USER");
        return buildAuthResponse(user, token);
    }

    @Transactional
    public AuthResponse socialLogin(String email, String username) {
        User user = registerOrLoginSocial(email, username);
        String token = tokenService.generateToken(user.getUsername(), "ROLE_USER");
        return buildAuthResponse(user, token);
    }

    @Transactional
    public AuthResponse loginWithGoogleIdToken(String idToken) {
        return loginWithGoogleIdToken(idToken, null);
    }

    @Transactional
    public AuthResponse loginWithGoogleIdToken(String idToken, String preferredUsername) {
        GoogleTokenVerifierService.VerifiedGoogleUser googleUser = googleTokenVerifierService.verifyIdToken(idToken);
        String normalizedEmail = normalizeEmail(googleUser.email());
        Optional<User> existingUser = userRepository.findFirstByEmailIgnoreCase(normalizedEmail);
        if (existingUser.isPresent()) {
            return socialLogin(googleUser.email(), googleUser.username());
        }

        String requestedUsername = normalizeUsername(preferredUsername);
        if (requestedUsername.isBlank()) {
            throw new ResourceConflictException("GOOGLE_USERNAME_REQUIRED", "Choose a username to complete Google sign-up");
        }

        return socialLogin(googleUser.email(), requestedUsername);
    }

    @Transactional
    public void verifyEmail(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findFirstByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new InvalidCodeException("Invalid or expired verification code"));

        assertAccountNotPendingDeletion(user);

        if (user.isEmailVerified()) {
            return;
        }

        if (user.getEmailVerificationCode() == null || user.getEmailVerificationExpiresAt() == null) {
            throw new InvalidCodeException("Invalid or expired verification code");
        }

        String normalizedCode = normalizeCode(code);
        if (!normalizedCode.equals(user.getEmailVerificationCode())) {
            throw new InvalidCodeException("Invalid or expired verification code");
        }

        if (user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCodeException("Invalid or expired verification code");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationCode(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public void resendVerification(String email) {
        String normalizedEmail = normalizeEmail(email);
        Optional<User> userOpt = userRepository.findFirstByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        assertAccountNotPendingDeletion(user);

        if (user.isEmailVerified()) {
            return;
        }

        issueEmailVerificationCode(user);
        emailDeliveryService.sendVerificationCode(user.getEmail(), user.getEmailVerificationCode());
    }

    @Transactional
    public void requestPasswordReset(String identifier) {
        Optional<User> userOpt = findByIdentifier(identifier);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        assertAccountNotPendingDeletion(user);

        issuePasswordResetCode(user);
        emailDeliveryService.sendPasswordResetCode(user.getEmail(), user.getPasswordResetCode());
    }

    @Transactional
    public void resetPassword(String identifier, String code, String newPassword) {
        User user = findByIdentifier(identifier)
                .orElseThrow(() -> new InvalidCodeException("Invalid or expired reset code"));

        assertAccountNotPendingDeletion(user);

        if (user.getPasswordResetCode() == null || user.getPasswordResetExpiresAt() == null) {
            throw new InvalidCodeException("Invalid or expired reset code");
        }

        String normalizedCode = normalizeCode(code);
        if (!normalizedCode.equals(user.getPasswordResetCode())) {
            throw new InvalidCodeException("Invalid or expired reset code");
        }

        if (user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidCodeException("Invalid or expired reset code");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetCode(null);
        user.setPasswordResetExpiresAt(null);
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public AccountDeletionResponse requestAccountDeletion(String authenticatedUsername) {
        User user = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledAt = now.plusDays(Math.max(1, accountDeletionGraceDays));

        if (user.isPendingDeletion() && user.getDeletionScheduledAt() != null && user.getDeletionScheduledAt().isAfter(now)) {
            return AccountDeletionResponse.builder()
                    .status("PENDING_DELETION")
                    .message("Your account deletion request is already active")
                    .scheduledDeletionAt(user.getDeletionScheduledAt().toString())
                    .graceDays(Math.max(1, accountDeletionGraceDays))
                    .build();
        }

        user.setPendingDeletion(true);
        user.setDeletionRequestedAt(now);
        user.setDeletionScheduledAt(scheduledAt);
        userRepository.save(user);

        return AccountDeletionResponse.builder()
                .status("PENDING_DELETION")
                .message("Your account will be permanently deleted after the grace period")
                .scheduledDeletionAt(scheduledAt.toString())
                .graceDays(Math.max(1, accountDeletionGraceDays))
                .build();
    }

    public UserPreferencesResponse getPreferences(String authenticatedUsername) {
        User user = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
        return toPreferencesResponse(user);
    }

    @Transactional
    public UserPreferencesResponse updatePreferences(String authenticatedUsername, String preferredLanguage, String themePreference) {
        User user = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        if (preferredLanguage != null && !preferredLanguage.isBlank()) {
            user.setPreferredLanguage(parsePreferredLanguage(preferredLanguage));
        }
        if (themePreference != null && !themePreference.isBlank()) {
            user.setThemePreference(parseThemePreference(themePreference));
        }

        User saved = userRepository.save(user);
        return toPreferencesResponse(saved);
    }

    @Transactional
    public int purgeExpiredPendingDeletions() {
        List<User> expiredUsers = userRepository.findByPendingDeletionTrueAndDeletionScheduledAtBefore(LocalDateTime.now());
        if (expiredUsers.isEmpty()) {
            return 0;
        }

        userRepository.deleteAll(expiredUsers);
        return expiredUsers.size();
    }

    @Transactional
    public void markOnboardingCompleted(String authenticatedUsername) {
        User user = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        if (user.isOnboardingCompleted()) {
            return;
        }

        user.setOnboardingCompleted(true);
        userRepository.save(user);
    }

    private void ensureDefaultWordList(User savedUser) {
        WordList generalList = WordList.builder()
                .name("All")
                .user(savedUser)
                .hexColorCode("#EEEEEE")
                .build();
        wordListRepository.save(generalList);
    }

    private void issueEmailVerificationCode(User user) {
        user.setEmailVerificationCode(generateCode());
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusMinutes(Math.max(5, emailVerificationCodeTtlMinutes)));
        userRepository.save(user);
    }

    private void issuePasswordResetCode(User user) {
        user.setPasswordResetCode(generateCode());
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(Math.max(5, passwordResetCodeTtlMinutes)));
        userRepository.save(user);
    }

    private String generateCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private String resolveAvailableSocialUsername(String preferredUsername) {
        String base = preferredUsername == null ? "" : preferredUsername.trim();
        if (base.isBlank()) {
            base = "user";
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }

        if (userRepository.findFirstByUsernameIgnoreCase(base).isEmpty()) {
            return base;
        }

        for (int i = 0; i < 50; i++) {
            String suffix = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));
            String candidate = (base + "_" + suffix);
            if (candidate.length() > 50) {
                candidate = candidate.substring(0, 50);
            }
            if (userRepository.findFirstByUsernameIgnoreCase(candidate).isEmpty()) {
                return candidate;
            }
        }

        throw new ResourceConflictException("USERNAME_ALREADY_EXISTS", "Could not allocate a unique username for social login");
    }

    private Optional<User> findByIdentifier(String identifier) {
        String normalized = identifier == null ? "" : identifier.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        if (normalized.contains("@")) {
            return userRepository.findFirstByEmailIgnoreCase(normalizeEmail(normalized));
        }

        return userRepository.findFirstByUsernameIgnoreCase(normalized)
                .or(() -> userRepository.findFirstByEmailIgnoreCase(normalizeEmail(normalized)));
    }

    private void throwRegistrationConflictForEmail(User existingUser) {
        assertAccountNotPendingDeletion(existingUser);
        throw new ResourceConflictException("EMAIL_ALREADY_EXISTS", "This email is already in use");
    }

    private void throwRegistrationConflictForUsername(User existingUser) {
        assertAccountNotPendingDeletion(existingUser);
        throw new ResourceConflictException("USERNAME_ALREADY_EXISTS", "This username is already in use");
    }

    private void assertAccountNotPendingDeletion(User user) {
        if (!user.isPendingDeletion()) {
            return;
        }

        LocalDateTime scheduledAt = user.getDeletionScheduledAt();
        if (scheduledAt == null) {
            throw new AccountPendingDeletionException(
                    "This account is pending deletion",
                    LocalDateTime.now().plusDays(Math.max(1, accountDeletionGraceDays))
            );
        }

        if (scheduledAt.isAfter(LocalDateTime.now())) {
            throw new AccountPendingDeletionException(
                    "This account is pending deletion. You can try again after the grace period",
                    scheduledAt
            );
        }
    }

    private User.PreferredLanguage parsePreferredLanguage(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return User.PreferredLanguage.valueOf(normalized);
        } catch (Exception ignored) {
            throw new IllegalArgumentException("Invalid preferredLanguage. Allowed values: TR, EN");
        }
    }

    private User.ThemePreference parseThemePreference(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return User.ThemePreference.valueOf(normalized);
        } catch (Exception ignored) {
            throw new IllegalArgumentException("Invalid themePreference. Allowed values: LIGHT, DARK");
        }
    }

    private UserPreferencesResponse toPreferencesResponse(User user) {
        return UserPreferencesResponse.builder()
                .userId(user.getId())
                .preferredLanguage(user.getPreferredLanguage().name())
                .themePreference(user.getThemePreference().name())
                .build();
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(token)
                .tokenType("Bearer")
                .plan(user.getSubscriptionLevel().name())
                .preferredLanguage(user.getPreferredLanguage().name())
                .themePreference(user.getThemePreference().name())
                .emailVerified(user.isEmailVerified())
                .onboardingCompleted(user.isOnboardingCompleted())
                .build();
    }
}
