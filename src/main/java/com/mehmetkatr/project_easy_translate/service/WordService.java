package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.dto.response.AdminWordSummaryResponse;
import com.mehmetkatr.project_easy_translate.dto.response.AdminWordUserResponse;
import com.mehmetkatr.project_easy_translate.entity.UserWord;
import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.exception.ResourceConflictException;
import com.mehmetkatr.project_easy_translate.repository.UserRepository;
import com.mehmetkatr.project_easy_translate.repository.UserWordRepository;
import com.mehmetkatr.project_easy_translate.repository.WordDetailRepository;
import com.mehmetkatr.project_easy_translate.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class WordService {

    private final WordRepository wordRepository;
    private final UserWordRepository userWordRepository;
    private final WordDetailRepository wordDetailRepository;
    private final UserRepository userRepository;

    public Word findOrCreateWord(String text, String languageCode) {
        String normalizedText = text.trim();
        Optional<Word> existing = wordRepository.findByTextIgnoreCaseAndLanguageCodeIgnoreCase(normalizedText, languageCode);
        if (existing.isPresent()) {
            if (existing.get().isBlocked()) {
                throw new ResourceConflictException("WORD_BLOCKED", "This word is blocked by an administrator");
            }
            return existing.get();
        }
        return wordRepository.save(
                Word.builder()
                        .text(normalizedText)
                        .languageCode(languageCode)
                        .build());
    }

    // ---- Admin: global kelime yonetimi (blok = soft delete) ----

    public List<AdminWordSummaryResponse> searchAdminWords(String query, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String q = (query == null || query.isBlank()) ? null : query.trim();
        return wordRepository.searchAdminWords(q, PageRequest.of(0, safeLimit));
    }

    public void setWordBlocked(Long wordId, boolean blocked) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("Word not found: " + wordId));
        word.setBlocked(blocked);
        wordRepository.save(word);
    }

    public List<AdminWordUserResponse> listWordUsers(Long wordId) {
        return userWordRepository.findUsersForWord(wordId);
    }

    /**
     * Global kelimeyi tamamen siler (blok DEGIL): tum kullanici kayitlari, liste baglari ve
     * detay cache'i kaldirilir. Bloklu olmadigi icin bir kullanici tekrar eklerse yeniden olusur.
     */
    public void deleteGlobalWord(Long wordId) {
        userWordRepository.deleteWordlistLinksByWordId(wordId);
        userWordRepository.deleteByWordIdNative(wordId);
        wordDetailRepository.deleteByWordIdNative(wordId);
        wordRepository.deleteById(wordId);
    }

    public UserWord addUserWord(Long userId, String text, String translated, String languageCode,
                                String targetLanguageCode, boolean starred, Collection<WordList> lists) {
        Word word = findOrCreateWord(text, languageCode);

        UserWord userWord = userWordRepository.findByUserIdAndWordId(userId, word.getId())
                .orElseGet(() -> UserWord.builder()
                        .user(userRepository.getReferenceById(userId))
                        .word(word)
                        .build());

        if (translated != null && !translated.isBlank()) {
            userWord.setTranslated(translated);
        }
        if (targetLanguageCode != null && !targetLanguageCode.isBlank()) {
            userWord.setTargetLanguageCode(targetLanguageCode.trim().toLowerCase());
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
