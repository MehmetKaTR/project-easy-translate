package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.dto.auth.AccountDeletionResponse;
import com.mehmetkatr.project_easy_translate.dto.auth.AuthResponse;
import com.mehmetkatr.project_easy_translate.dto.auth.UserPreferencesResponse;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.exception.AccountPendingDeletionException;
import com.mehmetkatr.project_easy_translate.exception.AuthenticationFailedException;
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

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final WordListRepository wordListRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    @Value("${app.account.deletion.grace-days:7}")
    private int accountDeletionGraceDays;

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
    public User registerOrLoginSocial(String email, String username) {
        purgeExpiredPendingDeletions();

        String normalizedEmail = normalizeEmail(email);
        String normalizedUsername = normalizeUsername(username);

        Optional<User> existingUser = userRepository.findFirstByEmailIgnoreCase(normalizedEmail);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            assertAccountNotPendingDeletion(user);
            return user;
        }

        userRepository.findFirstByUsernameIgnoreCase(normalizedUsername)
                .ifPresent(this::throwRegistrationConflictForUsername);

        User user = User.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash("")
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
    public AuthResponse register(String username, String email, String rawPassword) {
        User savedUser = registerUser(username, email, rawPassword);
        String token = tokenService.generateToken(savedUser.getUsername(), "ROLE_USER");
        return buildAuthResponse(savedUser, token);
    }

    public AuthResponse login(String username, String rawPassword) {
        String normalizedUsername = normalizeUsername(username);

        User user = userRepository.findFirstByUsernameIgnoreCase(normalizedUsername)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid username or password"));

        assertAccountNotPendingDeletion(user);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid username or password");
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
        GoogleTokenVerifierService.VerifiedGoogleUser googleUser = googleTokenVerifierService.verifyIdToken(idToken);
        return socialLogin(googleUser.email(), googleUser.username());
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

    private void ensureDefaultWordList(User savedUser) {
        WordList generalList = WordList.builder()
                .name("All")
                .user(savedUser)
                .hexColorCode("#EEEEEE")
                .build();
        wordListRepository.save(generalList);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
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
                .build();
    }
}
