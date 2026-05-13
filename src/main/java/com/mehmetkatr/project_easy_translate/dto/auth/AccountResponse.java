package com.mehmetkatr.project_easy_translate.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AccountResponse {
    private Long userId;
    private String username;
    private String email;
    private String plan;
    private Boolean emailVerified;
    private Boolean onboardingCompleted;
    private String preferredLanguage;
    private String themePreference;
    private Boolean pendingDeletion;
    private String scheduledDeletionAt;
}
