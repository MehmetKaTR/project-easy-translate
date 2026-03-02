package com.mehmetkatr.project_easy_translate.controller.auth;

import com.mehmetkatr.project_easy_translate.dto.auth.*;
import com.mehmetkatr.project_easy_translate.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<GenericMessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                GenericMessageResponse.builder()
                        .code("VERIFICATION_REQUIRED")
                        .message("Registration successful. Please verify your email with the code sent to your inbox.")
                        .build()
        );
    }

    @PostMapping("/verify-email")
    public ResponseEntity<GenericMessageResponse> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        userService.verifyEmail(request.getEmail(), request.getCode());
        return ResponseEntity.ok(
                GenericMessageResponse.builder()
                        .code("EMAIL_VERIFIED")
                        .message("Email verified successfully. You can now login.")
                        .build()
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<GenericMessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        userService.resendVerification(request.getEmail());
        return ResponseEntity.ok(
                GenericMessageResponse.builder()
                        .code("VERIFICATION_SENT")
                        .message("If the email exists and is not verified, a new verification code has been sent.")
                        .build()
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<GenericMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(
                GenericMessageResponse.builder()
                        .code("RESET_SENT")
                        .message("If the email exists, a password reset code has been sent.")
                        .build()
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<GenericMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(
                GenericMessageResponse.builder()
                        .code("PASSWORD_RESET_SUCCESS")
                        .message("Password reset successful. You can now login.")
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/social/google")
    public ResponseEntity<AuthResponse> socialGoogleLogin(@Valid @RequestBody GoogleTokenLoginRequest request) {
        AuthResponse response = userService.loginWithGoogleIdToken(request.getIdToken(), request.getPreferredUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/social/google/dev")
    public ResponseEntity<AuthResponse> socialGoogleDevLogin(@Valid @RequestBody SocialLoginRequest request) {
        AuthResponse response = userService.socialLogin(request.getEmail(), request.getUsername());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountDeletionResponse> requestDeleteAccount(Authentication authentication) {
        AccountDeletionResponse response = userService.requestAccountDeletion(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/deletion-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountDeletionResponse> getDeletionStatus(Authentication authentication) {
        AccountDeletionResponse response = userService.getAccountDeletionStatus(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/account/cancel-deletion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountDeletionResponse> cancelDeletion(Authentication authentication) {
        AccountDeletionResponse response = userService.cancelAccountDeletion(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/onboarding/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenericMessageResponse> completeOnboarding(Authentication authentication) {
        userService.markOnboardingCompleted(authentication.getName());
        return ResponseEntity.ok(
                GenericMessageResponse.builder()
                        .code("ONBOARDING_COMPLETED")
                        .message("Onboarding marked as completed.")
                        .build()
        );
    }
}
