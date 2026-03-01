package com.mehmetkatr.project_easy_translate.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private Long userId;
    private String username;
    private String token;
    private String tokenType;
    private String plan;
    private String preferredLanguage;
    private String themePreference;
    private Boolean emailVerified;
}
