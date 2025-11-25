package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.repository.WordListRepository;
import com.mehmetkatr.project_easy_translate.repository.WordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public void deleteWordList(Long id) {
        WordList existing = getWordListById(id);
        wordListRepository.delete(existing);
    }

    public WordList updateWordsInWordList(Long wordListId, List<Long> selectedWordIds) {
        WordList wordList = wordListRepository.findById(wordListId)
                .orElseThrow(() -> new IllegalArgumentException("WordList not found with id: " + wordListId));

        List<Word> existingWords = wordList.getWords();

        List<Word> wordsToAdd = wordRepository.findAllById(selectedWordIds).stream()
                .filter(word -> !existingWords.contains(word))
                .toList();
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
        List<Word> wordsToAdd = wordRepository.findAllById(wordIds).stream()
                .filter(word -> wordList.getWords().stream().noneMatch(w -> w.getId().equals(word.getId())))
                .toList();
        wordList.getWords().addAll(wordsToAdd);
        wordListRepository.save(wordList);
    }

    public void deleteWordsFromWordList(Long wordListId, List<Long> wordIds) {
        WordList wordList = getWordListById(wordListId);
        wordList.getWords().removeIf(word -> wordIds.contains(word.getId()));
        wordListRepository.save(wordList);
    }

}
