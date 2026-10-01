package com.mehmetkatr.project_easy_translate.dto.response;

import java.util.List;

public record WordDetailResponse(
        String word,
        String languageCode,
        String targetLanguage,
        String partOfSpeech,
        String definition,
        String definitionTranslation,
        List<String> synonyms,
        List<Example> examples,
        String source,
        String definitionSource,
        String examplesSource,
        String translationSource
) {
    public record Example(String sentence, String translation) {
    }
}
