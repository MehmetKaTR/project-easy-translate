package com.mehmetkatr.project_easy_translate.dto.story;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StoryGenerateResponse {
    private String title;
    private String story;
    private String turkishTranslation;
    private String translatedStory;
    private String translatedLanguage;
    private String translatedWordsCsv;
    private List<WordMapping> wordMappings;
    private String model;
    private int tokensUsed;

    @Getter
    @Builder
    public static class WordMapping {
        private String sourceWord;
        private String sourceHint;
        private String targetWord;
        private String hintStatus;
    }
}
