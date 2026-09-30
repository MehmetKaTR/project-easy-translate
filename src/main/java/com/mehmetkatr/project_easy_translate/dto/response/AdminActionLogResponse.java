package com.mehmetkatr.project_easy_translate.dto.response;

import com.mehmetkatr.project_easy_translate.entity.AdminActionLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminActionLogResponse {
    private Long id;
    private String adminUsername;
    private Long targetUserId;
    private String targetUsername;
    private AdminActionLog.AdminActionType actionType;
    private String createdAt;
}
