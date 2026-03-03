package com.mehmetkatr.project_easy_translate.controller.auth;

import com.mehmetkatr.project_easy_translate.dto.auth.*;
import com.mehmetkatr.project_easy_translate.service.AuthRateLimitService;
import com.mehmetkatr.project_easy_translate.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthRateLimitService authRateLimitService;
    @Value("${app.auth.enable-google-dev-endpoint:false}")
    private boolean enableGoogleDevEndpoint;

    @PostMapping("/register")
    public ResponseEntity<GenericMessageResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        throttle(httpRequest, "register", 12, Duration.ofMinutes(5));
        userService.register(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                GenericMessageResponse.builder()
                        .code("VERIFICATION_REQUIRED")
                        .message("Registration successful. Please verify your email with the code sent to your inbox.")
                        .build()
        );
    }

    @PostMapping("/verify-email")
    public ResponseEntity<GenericMessageResponse> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request,
            HttpServletRequest httpRequest
    ) {
        throttle(httpRequest, "verify-email", 30, Duration.ofMinutes(5));
        userService.verifyEmail(request.getEmail(), request.getCode());
        return ResponseEntity.ok(
                GenericMessageResponse.builder()
                        .code("EMAIL_VERIFIED")
                        .message("Email verified successfully. You can now login.")
                        .build()
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<GenericMessageResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest
    ) {
        throttle(httpRequest, "resend-verification", 10, Duration.ofMinutes(5));
        userService.resendVerification(request.getEmail());
        return ResponseEntity.ok(
                GenericMessageResponse.builder()
                        .code("VERIFICATION_SENT")
                        .message("If the email exists and is not verified, a new verification code has been sent.")
                        .build()
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<GenericMessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        throttle(httpRequest, "forgot-password", 10, Duration.ofMinutes(5));
        userService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(
                GenericMessageResponse.builder()
                        .code("RESET_SENT")
                        .message("If the email exists, a password reset code has been sent.")
                        .build()
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<GenericMessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        throttle(httpRequest, "reset-password", 20, Duration.ofMinutes(5));
        userService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(
                GenericMessageResponse.builder()
                        .code("PASSWORD_RESET_SUCCESS")
                        .message("Password reset successful. You can now login.")
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        throttle(httpRequest, "login", 25, Duration.ofMinutes(5));
        AuthResponse response = userService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/social/google")
    public ResponseEntity<AuthResponse> socialGoogleLogin(
            @Valid @RequestBody GoogleTokenLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        throttle(httpRequest, "social-google", 25, Duration.ofMinutes(5));
        AuthResponse response = userService.loginWithGoogleIdToken(request.getIdToken(), request.getPreferredUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/social/google/dev")
    public ResponseEntity<AuthResponse> socialGoogleDevLogin(
            @Valid @RequestBody SocialLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        if (!enableGoogleDevEndpoint) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        throttle(httpRequest, "social-google-dev", 25, Duration.ofMinutes(5));
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

    private void throttle(HttpServletRequest request, String action, int limit, Duration window) {
        authRateLimitService.assertAllowed(action + ":" + resolveClientIp(request), limit, window);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (!candidate.isBlank()) {
                    return candidate;
                }
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
