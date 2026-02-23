package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.repository.WordListRepository;
import com.mehmetkatr.project_easy_translate.repository.WordRepository;
import jakarta.persistence.EntityNotFoundException;
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

    public List<Word> getAllWordsByUser(Long userId) {
        return wordRepository.findAllByUserId(userId);
    }

    public List<Word> getStarredWordsByUser(Long userId) {
        return wordRepository.findStarredByUserId(userId);
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

    public List<Word> getByWordListAndStarred(WordList wordList, boolean starred) {
        return wordRepository.findByWordListAndStarred(wordList, starred);
    }

    public Word addWord(Word word) {
        return wordRepository.save(word);
    }

    public Word updateStarred(Long wordId, boolean starred) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new EntityNotFoundException("Word not found: " + wordId));
        word.setStarred(starred);
        return wordRepository.save(word);
    }

    public void deleteWord(Word word) {
        wordRepository.delete(word);
    }

    public void deleteWordById(Long wordId) {
        wordRepository.deleteById(wordId);
    }
}
