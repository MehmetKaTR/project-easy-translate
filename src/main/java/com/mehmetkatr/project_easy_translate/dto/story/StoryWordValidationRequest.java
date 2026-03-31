package com.mehmetkatr.project_easy_translate.dto.story;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StoryWordValidationRequest {

    @Valid
    @NotEmpty
    private List<StoryGenerateRequest.WordItem> words;

    private String translationTarget;
}
