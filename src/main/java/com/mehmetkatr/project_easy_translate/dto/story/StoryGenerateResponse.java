package com.mehmetkatr.project_easy_translate.dto.story;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoryGenerateResponse {
    private String title;
    private String story;
    private String turkishTranslation;
    private String translatedStory;
    private String translatedLanguage;
    private String translatedWordsCsv;
    private String model;
    private int tokensUsed;
}
