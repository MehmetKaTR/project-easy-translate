package com.mehmetkatr.project_easy_translate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_username", columnList = "username"),
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_pending_deletion", columnList = "pending_deletion,deletion_scheduled_at"),
                @Index(name = "idx_users_email_verified", columnList = "email_verified")
        }
)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 3, max = 50)
    @Column(unique = true, nullable = false)
    private String username;

    @NotNull
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @NotNull
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "email_verification_code", length = 16)
    private String emailVerificationCode;

    @Column(name = "email_verification_expires_at")
    private LocalDateTime emailVerificationExpiresAt;

    @Column(name = "password_reset_code", length = 16)
    private String passwordResetCode;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_level", nullable = false)
    private SubscriptionLevel subscriptionLevel;

    @Min(0)
    @Column(name = "token_balance", nullable = false)
    private int tokenBalance;

    @Builder.Default
    @Column(name = "pending_deletion", nullable = false)
    private boolean pendingDeletion = false;

    @Column(name = "deletion_requested_at")
    private LocalDateTime deletionRequestedAt;

    @Column(name = "deletion_scheduled_at")
    private LocalDateTime deletionScheduledAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", nullable = false)
    private PreferredLanguage preferredLanguage = PreferredLanguage.EN;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "theme_preference", nullable = false)
    private ThemePreference themePreference = ThemePreference.LIGHT;

    // ---------------- Relationships ----------------
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WordList> wordLists;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TokenUsageLog> tokenUsageLogs;

    @OneToMany(mappedBy = "targetUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdminActionLog> targetUserActions;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportExportLog> importExportLogs;

    public enum SubscriptionLevel {
        FREE,
        PREMIUM
    }

    public enum PreferredLanguage {
        TR,
        EN
    }

    public enum ThemePreference {
        LIGHT,
        DARK
    }
}
