package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.dto.WordDTO;
import com.mehmetkatr.project_easy_translate.security.AuthenticatedUserResolver;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.service.WordListService;
import com.mehmetkatr.project_easy_translate.service.WordService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;
    private final WordListService wordListService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping("/all")
    public ResponseEntity<List<WordDTO>> getAllWordsByUser(@RequestParam(required = false) Long userId) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        List<WordDTO> dtoList = wordService.getAllWordsByUser(authenticatedUserId).stream().map(WordDTO::new).toList();
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/all_words")
    @Transactional
    public ResponseEntity<List<WordDTO>> getAllWordsByAll(@RequestParam(required = false) Long userId) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        User user = new User();
        user.setId(authenticatedUserId);

        Optional<WordList> wordListOpt = wordListService.getWordListsByUserAndName(user, "All");
        if (wordListOpt.isEmpty()) return ResponseEntity.notFound().build();

        List<WordDTO> dtoList = wordListOpt.get().getWords().stream().map(WordDTO::new).toList();
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/byWordList")
    @Transactional
    public ResponseEntity<List<WordDTO>> getAllWordsByWordList(@RequestParam(required = false) Long userId, @RequestParam Long wordListId) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        User user = new User();
        user.setId(authenticatedUserId);

        Optional<WordList> wordListOpt = wordListService.getWordListsByUserAndWordListId(user, wordListId);
        if (wordListOpt.isEmpty()) return ResponseEntity.notFound().build();

        List<WordDTO> dtoList = wordListOpt.get().getWords().stream().map(WordDTO::new).toList();
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/starred")
    public ResponseEntity<List<WordDTO>> getStarredWords(@RequestParam(required = false) Long userId) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        List<WordDTO> dtoList = wordService.getStarredWordsByUser(authenticatedUserId).stream().map(WordDTO::new).toList();
        return ResponseEntity.ok(dtoList);
    }

    @PutMapping("/add")
    public ResponseEntity<WordDTO> addWord(@RequestParam(required = false) Long userId, @RequestBody WordDTO wordDTO) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        User user = new User();
        user.setId(authenticatedUserId);

        Optional<WordList> allList = wordListService.getWordListsByUserAndName(user, "All");
        if (allList.isEmpty()) return ResponseEntity.badRequest().build();

        Word word = Word.builder()
                .word(wordDTO.getWord())
                .translated(wordDTO.getTranslated())
                .languageCode(wordDTO.getLanguageCode())
                .wordList(allList.get())
                .starred(Boolean.TRUE.equals(wordDTO.getStarred()))
                .build();

        Word saved = wordService.addWord(word);
        return ResponseEntity.ok(new WordDTO(saved));
    }

    @PutMapping("/addToWordList")
    public ResponseEntity<WordDTO> addWordToWordList(
            @RequestParam(required = false) Long userId,
            @RequestParam long wordListId,
            @RequestBody WordDTO wordDTO
    ) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        User user = new User();
        user.setId(authenticatedUserId);

        Optional<WordList> targetListOpt = wordListService.getWordListsByUserAndWordListId(user, wordListId);
        if (targetListOpt.isEmpty()) return ResponseEntity.notFound().build();

        Word newWord = Word.builder()
                .word(wordDTO.getWord())
                .translated(wordDTO.getTranslated())
                .languageCode(wordDTO.getLanguageCode())
                .wordList(targetListOpt.get())
                .starred(Boolean.TRUE.equals(wordDTO.getStarred()))
                .build();

        Word saved = wordService.addWord(newWord);
        return ResponseEntity.ok(new WordDTO(saved));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<WordDTO> deleteWord(@RequestParam(required = false) Long userId, @RequestParam Long wordId) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        User user = new User();
        user.setId(authenticatedUserId);

        List<WordList> wordLists = wordListService.getWordListsByUser(user);
        if (wordLists == null || wordLists.isEmpty()) return ResponseEntity.badRequest().build();

        Optional<Word> wordOpt = wordLists.stream()
                .flatMap(wl -> wordService.getWordsByWordList(wl).stream())
                .filter(w -> w.getId().equals(wordId))
                .findFirst();

        if (wordOpt.isEmpty()) return ResponseEntity.notFound().build();

        Word word = wordOpt.get();
        wordService.deleteWordById(word.getId());

        return ResponseEntity.ok(new WordDTO(word));
    }

    @PutMapping("/update")
    public ResponseEntity<WordDTO> updateWord(
            @RequestParam(required = false) Long userId,
            @RequestParam Long wordId,
            @RequestBody WordDTO wordDTO
    ) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        User user = new User();
        user.setId(authenticatedUserId);

        List<WordList> wordLists = wordListService.getWordListsByUser(user);
        if (wordLists == null || wordLists.isEmpty()) return ResponseEntity.badRequest().build();

        Optional<Word> wordOpt = wordLists.stream()
                .flatMap(wl -> wordService.getWordsByWordList(wl).stream())
                .filter(w -> w.getId().equals(wordId))
                .findFirst();

        if (wordOpt.isEmpty()) return ResponseEntity.notFound().build();

        Word word = wordOpt.get();

        if (wordDTO.getWord() != null) word.setWord(wordDTO.getWord());
        if (wordDTO.getTranslated() != null) word.setTranslated(wordDTO.getTranslated());
        if (wordDTO.getLanguageCode() != null) word.setLanguageCode(wordDTO.getLanguageCode());
        if (wordDTO.getStarred() != null) word.setStarred(wordDTO.getStarred()); // KRITIK

        Word updated = wordService.addWord(word);
        return ResponseEntity.ok(new WordDTO(updated));
    }

    @PutMapping("/updateStarred")
    public ResponseEntity<WordDTO> updateStarred(
            @RequestParam(required = false) Long userId,
            @RequestParam Long wordId,
            @RequestParam boolean starred
    ) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        User user = new User();
        user.setId(authenticatedUserId);

        List<WordList> wordLists = wordListService.getWordListsByUser(user);
        if (wordLists == null || wordLists.isEmpty()) return ResponseEntity.badRequest().build();

        Optional<Word> wordOpt = wordLists.stream()
                .flatMap(wl -> wordService.getWordsByWordList(wl).stream())
                .filter(w -> w.getId().equals(wordId))
                .findFirst();

        if (wordOpt.isEmpty()) return ResponseEntity.notFound().build();

        Word word = wordOpt.get();
        word.setStarred(starred);

        Word updated = wordService.addWord(word);
        return ResponseEntity.ok(new WordDTO(updated));
    }
}
