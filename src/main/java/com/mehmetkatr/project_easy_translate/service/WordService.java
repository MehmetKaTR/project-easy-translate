package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.UserWord;
import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.repository.UserRepository;
import com.mehmetkatr.project_easy_translate.repository.UserWordRepository;
import com.mehmetkatr.project_easy_translate.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class WordService {

    private final WordRepository wordRepository;
    private final UserWordRepository userWordRepository;
    private final UserRepository userRepository;

    public Word findOrCreateWord(String text, String languageCode) {
        String normalizedText = text.trim();
        return wordRepository.findByTextIgnoreCaseAndLanguageCodeIgnoreCase(normalizedText, languageCode)
                .orElseGet(() -> wordRepository.save(
                        Word.builder()
                                .text(normalizedText)
                                .languageCode(languageCode)
                                .build()));
    }

    public UserWord addUserWord(Long userId, String text, String translated, String languageCode,
                                boolean starred, Collection<WordList> lists) {
        Word word = findOrCreateWord(text, languageCode);

        UserWord userWord = userWordRepository.findByUserIdAndWordId(userId, word.getId())
                .orElseGet(() -> UserWord.builder()
                        .user(userRepository.getReferenceById(userId))
                        .word(word)
                        .build());

        if (translated != null && !translated.isBlank()) {
            userWord.setTranslated(translated);
        }
        userWord.setStarred(starred);
        if (lists != null && !lists.isEmpty()) {
            userWord.getWordLists().addAll(lists);
        }
        return userWordRepository.save(userWord);
    }

    public Page<UserWord> getUserWords(Long userId, Pageable pageable) {
        return userWordRepository.findByUserId(userId, pageable);
    }

    public Page<UserWord> getStarredUserWords(Long userId, Pageable pageable) {
        return userWordRepository.findByUserIdAndStarred(userId, true, pageable);
    }

    public Page<UserWord> getUserWordsByWordList(Long wordListId, Long userId, Pageable pageable) {
        return userWordRepository.findByWordListId(wordListId, userId, pageable);
    }

    public Optional<UserWord> findUserWord(Long userId, Long userWordId) {
        return userWordRepository.findByIdAndUserId(userWordId, userId);
    }

    public UserWord updateStarred(Long userId, Long userWordId, boolean starred) {
        UserWord userWord = userWordRepository.findByIdAndUserId(userWordId, userId)
                .orElseThrow(() -> new IllegalArgumentException("UserWord not found: " + userWordId));
        userWord.setStarred(starred);
        return userWordRepository.save(userWord);
    }

    public boolean deleteUserWord(Long userId, Long userWordId) {
        return userWordRepository.findByIdAndUserId(userWordId, userId)
                .map(userWord -> {
                    userWordRepository.delete(userWord);
                    return true;
                })
                .orElse(false);
    }

    public UserWord save(UserWord userWord) {
        return userWordRepository.save(userWord);
    }
}
