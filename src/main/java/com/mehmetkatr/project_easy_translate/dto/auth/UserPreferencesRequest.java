package com.mehmetkatr.project_easy_translate.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPreferencesRequest {
    private String preferredLanguage;
    private String themePreference;
}
