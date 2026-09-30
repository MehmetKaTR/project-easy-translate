package com.mehmetkatr.project_easy_translate.dto.response;

import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WordDTO {
    private Long id;
    private String word;
    private String translated;
    private String languageCode;
    private Long wordListId;
    private Boolean starred;

    public WordDTO(Word word) {
        this.id = word.getId();
        this.word = word.getWord();
        this.translated = word.getTranslated();
        this.languageCode = word.getLanguageCode();
        this.wordListId = word.getWordLists().stream().map(WordList::getId).findFirst().orElse(null);
        this.starred = word.isStarred();
    }
}
