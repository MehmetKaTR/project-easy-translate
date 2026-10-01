package com.mehmetkatr.project_easy_translate.dto.response;

import com.mehmetkatr.project_easy_translate.entity.UserWord;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Size(max = 100)
    @Pattern(
            regexp = "^\\p{L}[\\p{L} '’\\-]*$",
            message = "Word must be a real word or phrase (letters, spaces, hyphen or apostrophe only)")
    private String word;

    @Size(max = 255)
    private String translated;

    @Size(max = 10)
    private String languageCode;

    // Target language the user translated into (drives word-detail translations).
    @Size(max = 10)
    private String targetLanguageCode;

    private Long wordListId;
    private Boolean starred;

    // UserWord -> DTO: id = userWord.id, word/languageCode global Word'den, translated/starred kullaniciya ait
    public WordDTO(UserWord userWord) {
        this.id = userWord.getId();
        this.word = userWord.getWord().getText();
        this.translated = userWord.getTranslated();
        this.languageCode = userWord.getWord().getLanguageCode();
        this.targetLanguageCode = userWord.getTargetLanguageCode();
        this.wordListId = userWord.getWordLists().stream().map(WordList::getId).findFirst().orElse(null);
        this.starred = userWord.isStarred();
    }
}
