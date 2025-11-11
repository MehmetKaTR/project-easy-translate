package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.repository.WordListRepository;
import com.mehmetkatr.project_easy_translate.repository.WordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WordService {

    private final WordRepository wordRepository;
    private final WordListRepository wordListRepository;

    public List<Word> getWordsByWordList(WordList wordList) {
        return wordRepository.findByWordList(wordList);
    }

    public List<Word> getWordsByLanguageCode(String languageCode) {
        return wordRepository.findByLanguageCode(languageCode);
    }

    public List<Word> getByWordListAndLanguageCode(WordList wordList, String languageCode) {
        return wordRepository.findByWordListAndLanguageCode(wordList, languageCode);
    }

    public List<Word> getByTranslation(String translated) {
        return wordRepository.findByTranslated(translated);
    }

    public List<Word> getByWordListAndTranslated(WordList wordList, String translated) {
        return wordRepository.findByWordListAndTranslated(wordList, translated);
    }

}
