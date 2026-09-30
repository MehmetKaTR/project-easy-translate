package com.mehmetkatr.project_easy_translate.dto.response;

import com.mehmetkatr.project_easy_translate.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminUserSummaryResponse {
    private Long id;
    private String username;
    private String email;
    private User.SubscriptionLevel subscriptionLevel;
    private int tokenBalance;
    private boolean emailVerified;
    private boolean pendingDeletion;
    private boolean onboardingCompleted;
    private String createdAt;
    private String updatedAt;
}
