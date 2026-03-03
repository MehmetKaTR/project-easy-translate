package com.mehmetkatr.project_easy_translate.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminAuthResponse {
    private Long adminId;
    private String username;
    private String token;
    private String tokenType;
}
