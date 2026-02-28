package com.mehmetkatr.project_easy_translate.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserPreferencesResponse {
    private Long userId;
    private String preferredLanguage;
    private String themePreference;
}
