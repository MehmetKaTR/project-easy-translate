package com.mehmetkatr.project_easy_translate.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StoryWordValidationResponse {
    private List<Item> items;

    @Getter
    @Builder
    public static class Item {
        private String clientId;
        private String word;
        private String originalHint;
        private String status;
        private String approvedHint;
        private String suggestedHint;
        private String reason;
    }
}
