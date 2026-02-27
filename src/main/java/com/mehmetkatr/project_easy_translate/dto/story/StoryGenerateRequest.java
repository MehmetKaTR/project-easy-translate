package com.mehmetkatr.project_easy_translate.dto.story;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StoryGenerateRequest {

    @NotBlank
    private String level;

    @NotBlank
    private String topic;

    @NotBlank
    private String sentiment;

    @NotBlank
    private String length;

    @NotEmpty
    private List<WordItem> words;

    @Getter
    @Setter
    public static class WordItem {
        @NotBlank
        private String word;
    }
}
