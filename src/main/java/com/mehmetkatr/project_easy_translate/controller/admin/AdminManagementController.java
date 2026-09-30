package com.mehmetkatr.project_easy_translate.controller.admin;

import com.mehmetkatr.project_easy_translate.dto.response.AdminActionLogResponse;
import com.mehmetkatr.project_easy_translate.dto.response.AdminUserSummaryResponse;
import com.mehmetkatr.project_easy_translate.dto.request.AdminUserUpdateRequest;
import com.mehmetkatr.project_easy_translate.entity.AdminActionLog;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.service.AdminActionLogService;
import com.mehmetkatr.project_easy_translate.service.AdminUserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminManagementController {

    private final AdminUserManagementService adminUserManagementService;
    private final AdminActionLogService adminActionLogService;

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserSummaryResponse>> listUsers(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "50") int limit
    ) {
        List<AdminUserSummaryResponse> response = adminUserManagementService.listUsers(query, limit)
                .stream()
                .map(this::toUserSummary)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<AdminUserSummaryResponse> updateUser(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        User updated = adminUserManagementService.updateUser(authentication.getName(), userId, request);
        return ResponseEntity.ok(toUserSummary(updated));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(Authentication authentication, @PathVariable Long userId) {
        adminUserManagementService.deleteUser(authentication.getName(), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AdminActionLogResponse>> getLogs(@RequestParam(defaultValue = "50") int limit) {
        List<AdminActionLogResponse> response = adminActionLogService.findRecent(limit)
                .stream()
                .map(this::toLogResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    private AdminUserSummaryResponse toUserSummary(User user) {
        return AdminUserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .subscriptionLevel(user.getSubscriptionLevel())
                .tokenBalance(user.getTokenBalance())
                .emailVerified(user.isEmailVerified())
                .pendingDeletion(user.isPendingDeletion())
                .onboardingCompleted(user.isOnboardingCompleted())
                .createdAt(user.getCreatedAt() == null ? null : user.getCreatedAt().toString())
                .updatedAt(user.getUpdatedAt() == null ? null : user.getUpdatedAt().toString())
                .build();
    }

    private AdminActionLogResponse toLogResponse(AdminActionLog log) {
        return AdminActionLogResponse.builder()
                .id(log.getId())
                .adminUsername(log.getAdmin() == null ? null : log.getAdmin().getUsername())
                .targetUserId(log.getTargetUser() == null ? null : log.getTargetUser().getId())
                .targetUsername(log.getTargetUser() == null ? null : log.getTargetUser().getUsername())
                .actionType(log.getActionType())
                .createdAt(log.getCreatedAt() == null ? null : log.getCreatedAt().toString())
                .build();
    }
}
