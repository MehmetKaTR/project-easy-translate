package com.mehmetkatr.project_easy_translate.dto;

import com.mehmetkatr.project_easy_translate.entity.Word;
import lombok.Getter;

@Getter
public class WordDTO {
    private Long id;
    private String word;
    private String translated;
    private String languageCode;
    private Long wordListId;

    public WordDTO(Word word) {
        this.id = word.getId();
        this.word = word.getWord();
        this.translated = word.getTranslated();
        this.languageCode = word.getLanguageCode();
        this.wordListId = word.getWordList().getId();
    }
}
