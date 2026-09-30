package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.dto.response.WordListSummaryResponse;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.repository.WordListRepository;
import com.mehmetkatr.project_easy_translate.repository.WordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WordListService {

    private final WordListRepository wordListRepository;
    private final WordRepository wordRepository;

    public List<WordList> getAllWordLists() {
        return wordListRepository.findAll();
    }

    public WordList getWordListById(Long id) {
        return wordListRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("WordList not found with id: " + id));
    }

    public List<WordList> getWordListsByUser(User user) {
        return wordListRepository.findByUserAndNameContainingIgnoreCase(user, "");
    }

    public List<WordList> getWordListsByUser(Long userId) {
        return wordListRepository.findAllByUserIdWithWords(userId);
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

    public WordList updateWordList(Long id, WordList updatedWordList) {
        WordList existing = getWordListById(id);

        wordListRepository.findByUserAndName(existing.getUser(), updatedWordList.getName())
                .filter(wl -> !wl.getId().equals(id))
                .ifPresent(conflict -> {
                    throw new IllegalArgumentException("This user already has a WordList with the same name.");
                });

        existing.setName(updatedWordList.getName());
        existing.setWords(updatedWordList.getWords());

        return wordListRepository.save(existing);
    }

    public WordList updateWordListOwnedByUser(
            Long userId,
            WordList existing,
            String name,
            String color,
            List<Long> requestedWordIds
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

        Set<Long> dedupedIds = requestedWordIds == null
                ? Set.of()
                : requestedWordIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));

        if (requestedWordIds != null) {
            Map<Long, Word> userWordsById = wordRepository.findAllByUserId(userId).stream()
                    .collect(Collectors.toMap(Word::getId, word -> word));

            Set<Word> selectedWords = new LinkedHashSet<>();
            for (Long wordId : dedupedIds) {
                Word word = userWordsById.get(wordId);
                if (word != null) {
                    selectedWords.add(word);
                }
            }

            // Many-to-many join table owner is Word.wordLists; keep both sides in sync.
            Set<Word> previousWords = new LinkedHashSet<>(existing.getWords());

            for (Word word : previousWords) {
                if (!selectedWords.contains(word)) {
                    word.getWordLists().remove(existing);
                }
            }

            for (Word word : selectedWords) {
                word.getWordLists().add(existing);
            }

            existing.setWords(selectedWords);
        }

        return wordListRepository.save(existing);
    }

    public void deleteWordList(Long id) {
        WordList existing = getWordListById(id);
        wordListRepository.delete(existing);
    }

    public WordList updateWordsInWordList(Long wordListId, List<Long> selectedWordIds) {
        WordList wordList = wordListRepository.findById(wordListId)
                .orElseThrow(() -> new IllegalArgumentException("WordList not found with id: " + wordListId));

        Set<Word> existingWords = wordList.getWords();

        List<Word> wordsToAdd = wordRepository.findAllById(selectedWordIds);
        existingWords.addAll(wordsToAdd);

        existingWords.removeIf(word -> !selectedWordIds.contains(word.getId()));

        wordList.setWords(existingWords);

        return wordListRepository.save(wordList);
    }

    public WordList saveWordList(WordList wordList) {
        return wordListRepository.save(wordList);
    }


    public void addWordsToWordList(Long wordListId, List<Long> wordIds) {
        WordList wordList = getWordListById(wordListId);
        List<Word> wordsToAdd = wordRepository.findAllById(wordIds);
        wordList.getWords().addAll(wordsToAdd);
        wordListRepository.save(wordList);
    }

    public void deleteWordsFromWordList(Long wordListId, List<Long> wordIds) {
        WordList wordList = getWordListById(wordListId);
        wordList.getWords().removeIf(word -> wordIds.contains(word.getId()));
        wordListRepository.save(wordList);
    }

}
