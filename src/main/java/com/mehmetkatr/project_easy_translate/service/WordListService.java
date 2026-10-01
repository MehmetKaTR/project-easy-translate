package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.dto.response.WordListSummaryResponse;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.entity.UserWord;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.repository.UserWordRepository;
import com.mehmetkatr.project_easy_translate.repository.WordListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WordListService {

    private final WordListRepository wordListRepository;
    private final UserWordRepository userWordRepository;

    public WordList getWordListById(Long id) {
        return wordListRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("WordList not found with id: " + id));
    }

    public List<WordList> getWordListsByUser(User user) {
        return wordListRepository.findByUserAndNameContainingIgnoreCase(user, "");
    }

    public Optional<WordList> getWordListsByUserAndName(User user, String name) {
        return wordListRepository.findByUserAndName(user, name);
    }

    public Optional<WordList> getWordListsByUserAndWordListId(User user, Long wordlistId) {
        return wordListRepository.findByUserAndId(user, wordlistId);
    }

    public Page<WordListSummaryResponse> getWordListSummaries(Long userId, Pageable pageable) {
        return wordListRepository.findSummariesByUserId(userId, pageable);
    }

    public WordList createWordList(WordList wordList) {
        wordListRepository.findByUserAndName(wordList.getUser(), wordList.getName())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("This user already has a WordList with the same name.");
                });
        return wordListRepository.save(wordList);
    }

    public WordList saveWordList(WordList wordList) {
        return wordListRepository.save(wordList);
    }

    public void deleteWordList(Long id) {
        wordListRepository.delete(getWordListById(id));
    }

    /**
     * Bir listenin iceriğini (hangi UserWord'ler bu listede) gunceller.
     * requestedUserWordIds = kullanicinin UserWord id'leri (frontend WordDTO.id gonderir).
     * M:N'in sahibi UserWord.wordLists oldugu icin degisikligi orada yapariz.
     */
    public WordList updateWordListOwnedByUser(
            Long userId,
            WordList existing,
            String name,
            String color,
            List<Long> requestedUserWordIds
    ) {
        if (existing.getUser() == null || !Objects.equals(existing.getUser().getId(), userId)) {
            throw new AccessDeniedException("You cannot update another user's word list");
        }

        String normalizedName = name == null ? "" : name.trim();
        if (!normalizedName.isBlank()) {
            wordListRepository.findByUserAndName(existing.getUser(), normalizedName)
                    .filter(wl -> !wl.getId().equals(existing.getId()))
                    .ifPresent(conflict -> {
                        throw new IllegalArgumentException("This user already has a WordList with the same name.");
                    });
            existing.setName(normalizedName);
        }

        String normalizedColor = color == null ? "" : color.trim();
        if (!normalizedColor.isBlank()) {
            existing.setHexColorCode(normalizedColor);
        }

        if (requestedUserWordIds != null) {
            Set<UserWord> selected = userWordRepository.findAllById(requestedUserWordIds).stream()
                    .filter(uw -> Objects.equals(uw.getUser().getId(), userId))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Set<UserWord> current = new LinkedHashSet<>(existing.getUserWords());

            for (UserWord uw : current) {
                if (!selected.contains(uw)) {
                    uw.getWordLists().remove(existing);
                }
            }
            for (UserWord uw : selected) {
                uw.getWordLists().add(existing);
            }

            Set<UserWord> affected = new LinkedHashSet<>(current);
            affected.addAll(selected);
            userWordRepository.saveAll(affected);
        }

        return wordListRepository.save(existing);
    }
}
