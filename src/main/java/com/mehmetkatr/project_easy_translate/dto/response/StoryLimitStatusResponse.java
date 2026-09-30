package com.mehmetkatr.project_easy_translate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StoryLimitStatusResponse {
    private int dailyLimit;
    private int usedToday;
    private int remainingToday;
    private String resetAt;
    private boolean limitReached;
    private String plan;
}
