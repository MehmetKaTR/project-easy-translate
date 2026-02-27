package com.mehmetkatr.project_easy_translate.controller.auth;

import com.mehmetkatr.project_easy_translate.dto.auth.AuthResponse;
import com.mehmetkatr.project_easy_translate.dto.auth.GoogleTokenLoginRequest;
import com.mehmetkatr.project_easy_translate.dto.auth.LoginRequest;
import com.mehmetkatr.project_easy_translate.dto.auth.RegisterRequest;
import com.mehmetkatr.project_easy_translate.dto.auth.SocialLoginRequest;
import com.mehmetkatr.project_easy_translate.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/social/google")
    public ResponseEntity<AuthResponse> socialGoogleLogin(@Valid @RequestBody GoogleTokenLoginRequest request) {
        AuthResponse response = userService.loginWithGoogleIdToken(request.getIdToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/social/google/dev")
    public ResponseEntity<AuthResponse> socialGoogleDevLogin(@Valid @RequestBody SocialLoginRequest request) {
        AuthResponse response = userService.socialLogin(request.getEmail(), request.getUsername());
        return ResponseEntity.ok(response);
    }
}
