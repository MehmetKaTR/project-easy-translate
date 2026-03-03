package com.mehmetkatr.project_easy_translate.dto.admin;

import com.mehmetkatr.project_easy_translate.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserUpdateRequest {
    private String username;
    private String email;
    private String password;
    private User.SubscriptionLevel subscriptionLevel;
    private Integer tokenBalance;
    private Boolean emailVerified;
    private Boolean pendingDeletion;
    private Boolean onboardingCompleted;
}
