package com.mehmetkatr.project_easy_translate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

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
                @Index(name = "idx_users_email", columnList = "email")
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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_level", nullable = false)
    private SubscriptionLevel subscriptionLevel;

    @Min(0)
    @Column(name = "token_balance", nullable = false)
    private int tokenBalance;

    // ---------------- Relationships ----------------
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WordList> wordLists;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TokenUsageLog> tokenUsageLogs;

    @OneToMany(mappedBy = "targetUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdminActionLog> targetUserActions;

    //cascade = Parent uzerinde yapilan islemleri (save, delete, update) child’a da uygular
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportExportLog> importExportLogs;

    public enum SubscriptionLevel {
        FREE,
        PREMIUM
    }
}
