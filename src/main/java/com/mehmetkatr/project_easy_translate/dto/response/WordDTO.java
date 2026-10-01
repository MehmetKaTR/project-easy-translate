package com.mehmetkatr.project_easy_translate.dto.response;

import com.mehmetkatr.project_easy_translate.entity.UserWord;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WordDTO {
    private Long id;

    @NotBlank
    @Size(max = 255)
    private String word;

    @Size(max = 255)
    private String translated;

    @Size(max = 10)
    private String languageCode;

    private Long wordListId;
    private Boolean starred;

    // UserWord -> DTO: id = userWord.id, word/languageCode global Word'den, translated/starred kullaniciya ait
    public WordDTO(UserWord userWord) {
        this.id = userWord.getId();
        this.word = userWord.getWord().getText();
        this.translated = userWord.getTranslated();
        this.languageCode = userWord.getWord().getLanguageCode();
        this.wordListId = userWord.getWordLists().stream().map(WordList::getId).findFirst().orElse(null);
        this.starred = userWord.isStarred();
    }
}
