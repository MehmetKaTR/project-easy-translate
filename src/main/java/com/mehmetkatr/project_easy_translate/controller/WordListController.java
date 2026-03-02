package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.dto.WordListDTO;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.security.AuthenticatedUserResolver;
import com.mehmetkatr.project_easy_translate.service.WordListService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
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
        newList.setWords(new LinkedHashSet<>());

        WordList saved = wordListService.createWordList(newList);

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

        Optional<WordList> existingOpt = wordListService.getWordListsByUserAndWordListId(user, wordListId);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Long> requestedWordIds = dto.getWords() == null
                ? null
                : dto.getWords().stream().map(Word::getId).toList();

        WordList saved = wordListService.updateWordListOwnedByUser(
                authenticatedUserId,
                existingOpt.get(),
                dto.getName(),
                dto.getColor(),
                requestedWordIds
        );

        return ResponseEntity.ok(new WordListDTO(saved));
    }


    @DeleteMapping("/delete")
    public ResponseEntity<WordListDTO> deleteWordList(
            @RequestParam(required = false) Long userId,
            @RequestParam Long wordListId) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);

        User user = new User();
        user.setId(authenticatedUserId);

        Optional<WordList> wordListOpt = wordListService.getWordListsByUserAndWordListId(user, wordListId);

        if (wordListOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WordList wordList = wordListOpt.get();
        wordListService.deleteWordList(wordList.getId());

        return ResponseEntity.ok(new WordListDTO(wordList));
    }

}
