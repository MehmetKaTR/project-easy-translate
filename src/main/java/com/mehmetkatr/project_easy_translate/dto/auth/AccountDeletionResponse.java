package com.mehmetkatr.project_easy_translate.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AccountDeletionResponse {
    private String status;
    private String message;
    private String scheduledDeletionAt;
    private Integer graceDays;
    private Boolean pendingDeletion;
}
