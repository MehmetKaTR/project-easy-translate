package com.mehmetkatr.project_easy_translate.dto;

import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import lombok.Getter;

import java.util.List;

@Getter
public class WordListDTO {
    private Long id;
    private String name;
    private String color;
    private List<Word> words;

    public WordListDTO(){}

    public WordListDTO(WordList wordList) {
        this.id = wordList.getId();
        this.name = wordList.getName();
        this.color = wordList.getHexColorCode();
        this.words = wordList.getWords();
    }
}


