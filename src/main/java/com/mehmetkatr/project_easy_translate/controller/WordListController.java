package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.dto.WordListDTO;
import com.mehmetkatr.project_easy_translate.security.AuthenticatedUserResolver;
import com.mehmetkatr.project_easy_translate.entity.User;

import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.service.WordListService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/word_lists")
@RequiredArgsConstructor
public class WordListController {

    private final WordListService wordListService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping("/all")
    public ResponseEntity<List<WordListDTO>> getAllWordListsByUser(@RequestParam(required = false) Long userId) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);

        List<WordList> wordLists = wordListService.getWordListsByUser(authenticatedUserId);

        List<WordListDTO> dtoList = wordLists.stream()
                .map(WordListDTO::new)
                .toList();

        return ResponseEntity.ok(dtoList);
    }

    @PostMapping("/add")
    public ResponseEntity<WordListDTO> addWordList(@RequestParam(required = false) Long userId, @RequestBody WordListDTO dto) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);

        User user = new User();
        user.setId(authenticatedUserId);

        WordList newList = new WordList();
        newList.setUser(user);
        newList.setName(dto.getName());
        newList.setHexColorCode(dto.getColor());
        newList.setWords(new ArrayList<>());

        WordList saved = wordListService.saveWordList(newList);

        return ResponseEntity.ok(new WordListDTO(saved));
    }

    @PutMapping("/update")
    @Transactional
    public ResponseEntity<WordListDTO> updateWordList(
            @RequestParam(required = false) Long userId,
            @RequestParam Long wordListId,
            @RequestBody WordListDTO dto) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);

        User user = new User();
        user.setId(authenticatedUserId);

        WordList wordList = new WordList();
        wordList.setId(wordListId);
        wordList.setUser(user);
        wordList.setName(dto.getName());
        wordList.setHexColorCode(dto.getColor());

        List<Word> updatedWords = new ArrayList<>();

        for (Word w : dto.getWords()) {
            w.setWordList(wordList);
            updatedWords.add(w);
        }

        wordList.setWords(updatedWords);

        WordList saved = wordListService.saveWordList(wordList);

        return ResponseEntity.ok(new WordListDTO(saved));
    }


    @DeleteMapping("/delete")
    public ResponseEntity<WordListDTO> deleteWordList(
            @RequestParam(required = false) Long userId,
            @RequestParam Long wordListId) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);

        // Kullanıcıyı oluştur / doğrula
        User user = new User();
        user.setId(authenticatedUserId);

        // Kullanıcının WordList'ini al
        Optional<WordList> wordListOpt = wordListService.getWordListsByUserAndWordListId(user, wordListId);

        if (wordListOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WordList wordList = wordListOpt.get();

        // WordList'i sil
        wordListService.deleteWordList(wordList.getId());

        // Silinen WordList'i DTO ile dön
        return ResponseEntity.ok(new WordListDTO(wordList));
    }

}
