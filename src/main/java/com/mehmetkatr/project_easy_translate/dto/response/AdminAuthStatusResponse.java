package com.mehmetkatr.project_easy_translate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminAuthStatusResponse {
    private boolean hasAdmin;
}
