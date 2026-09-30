package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.dto.request.UserPreferencesRequest;
import com.mehmetkatr.project_easy_translate.dto.response.UserPreferencesResponse;
import com.mehmetkatr.project_easy_translate.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserPreferencesResponse> getPreferences(Authentication authentication) {
        return ResponseEntity.ok(userService.getPreferences(authentication.getName()));
    }

    @PutMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserPreferencesResponse> updatePreferences(
            Authentication authentication,
            @RequestBody UserPreferencesRequest request
    ) {
        return ResponseEntity.ok(
                userService.updatePreferences(
                        authentication.getName(),
                        request.getPreferredLanguage(),
                        request.getThemePreference()
                )
        );
    }
}
