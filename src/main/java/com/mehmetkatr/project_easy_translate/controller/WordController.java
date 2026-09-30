
package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.dto.response.WordDTO;
import com.mehmetkatr.project_easy_translate.dto.response.PagedResponse;
import com.mehmetkatr.project_easy_translate.security.AuthenticatedUserResolver;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.service.WordListService;
import com.mehmetkatr.project_easy_translate.service.WordService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
@Transactional

public class WordController {

    private final WordService wordService;
    private final WordListService wordListService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping("/all")
    public ResponseEntity<PagedResponse<WordDTO>> getAllWordsByUser(@RequestParam(required = false) Long userId, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Page<WordDTO> page = wordService.getAllWordsByUser(authenticatedUserId, pageable)
                .map(WordDTO::new);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @GetMapping("/all_words")
    @Transactional
    public ResponseEntity<PagedResponse<WordDTO>> getAllWordsByAll(@RequestParam(required = false) Long userId, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Optional<WordList> wordListOpt = resolveAllList(authenticatedUserId);
        if (wordListOpt.isEmpty()) return ResponseEntity.notFound().build();

        Page<WordDTO> page = wordService.getWordsByWordListId(wordListOpt.get().getId(), pageable).map(WordDTO::new);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @GetMapping("/starred")
    public ResponseEntity<PagedResponse<WordDTO>> getStarredWords(@RequestParam(required = false) Long userId, @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Page<WordDTO> page = wordService.getStarredWordsByUser(authenticatedUserId, pageable).map(WordDTO::new);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @GetMapping("/byWordList")
    @Transactional
    public ResponseEntity<PagedResponse<WordDTO>> getAllWordsByWordList(
            @RequestParam(required = false) Long userId,
            @RequestParam Long wordListId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        User user = new User();
        user.setId(authenticatedUserId);

        // sahiplik kontrolü: liste gerçekten bu kullanıcıya mı ait?
        if (wordListService.getWordListsByUserAndWordListId(user, wordListId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Page<WordDTO> page = wordService.getWordsByWordListId(wordListId, pageable).map(WordDTO::new);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @PutMapping("/add")
    public ResponseEntity<WordDTO> addWord(@RequestParam(required = false) Long userId, @RequestBody WordDTO wordDTO) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Optional<WordList> allList = resolveAllList(authenticatedUserId);
        if (allList.isEmpty()) return ResponseEntity.badRequest().build();

        Word word = findExistingWordByContent(authenticatedUserId, wordDTO)
                .orElseGet(() -> {
                    User user = new User();
                    user.setId(authenticatedUserId);
                    return Word.builder()
                            .word(wordDTO.getWord())
                            .translated(wordDTO.getTranslated())
                            .languageCode(wordDTO.getLanguageCode())
                            .user(user)
                            .starred(Boolean.TRUE.equals(wordDTO.getStarred()))
                            .build();
                });

        if (wordDTO.getStarred() != null) {
            word.setStarred(wordDTO.getStarred());
        }
        word.getWordLists().add(allList.get());

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
        Optional<WordList> allListOpt = resolveAllList(authenticatedUserId);
        if (allListOpt.isEmpty()) return ResponseEntity.badRequest().build();

        Optional<Word> existingById = Optional.ofNullable(wordDTO.getId())
                .flatMap(id -> wordService.getAllWordsByUser(authenticatedUserId).stream()
                        .filter(item -> item.getId().equals(id))
                        .findFirst());
        Word word = existingById.or(() -> findExistingWordByContent(authenticatedUserId, wordDTO))
                .orElseGet(() -> {
                    Word created = new Word();
                    created.setWord(wordDTO.getWord());
                    created.setTranslated(wordDTO.getTranslated());
                    created.setLanguageCode(wordDTO.getLanguageCode());
                    created.setStarred(Boolean.TRUE.equals(wordDTO.getStarred()));
                    created.setUser(user);
                    return created;
                });

        if (wordDTO.getStarred() != null) {
            word.setStarred(wordDTO.getStarred());
        }

        // Dogru model: kelime tek kayit, All + hedef klasor iliskisi tutulur.
        word.getWordLists().add(allListOpt.get());
        word.getWordLists().add(targetListOpt.get());

        Word saved = wordService.addWord(word);
        return ResponseEntity.ok(new WordDTO(saved));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<WordDTO> deleteWord(@RequestParam(required = false) Long userId, @RequestParam Long wordId) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Optional<Word> wordOpt = wordService.getAllWordsByUser(authenticatedUserId).stream()
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
        Optional<Word> wordOpt = wordService.getAllWordsByUser(authenticatedUserId).stream()
                .filter(w -> w.getId().equals(wordId))
                .findFirst();

        if (wordOpt.isEmpty()) return ResponseEntity.notFound().build();

        Word word = wordOpt.get();

        if (wordDTO.getWord() != null) word.setWord(wordDTO.getWord());
        if (wordDTO.getTranslated() != null) word.setTranslated(wordDTO.getTranslated());
        if (wordDTO.getLanguageCode() != null) word.setLanguageCode(wordDTO.getLanguageCode());
        if (wordDTO.getStarred() != null) word.setStarred(wordDTO.getStarred());

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
        Optional<Word> wordOpt = wordService.getAllWordsByUser(authenticatedUserId).stream()
                .filter(w -> w.getId().equals(wordId))
                .findFirst();

        if (wordOpt.isEmpty()) return ResponseEntity.notFound().build();

        Word word = wordOpt.get();
        word.setStarred(starred);

        Word updated = wordService.addWord(word);
        return ResponseEntity.ok(new WordDTO(updated));
    }

    private Optional<WordList> resolveAllList(Long authenticatedUserId) {
        User user = new User();
        user.setId(authenticatedUserId);
        return wordListService.getWordListsByUserAndName(user, "All");
    }

    private Optional<Word> findExistingWordByContent(Long userId, WordDTO dto) {
        if (dto == null || isBlank(dto.getWord()) || isBlank(dto.getTranslated()) || isBlank(dto.getLanguageCode())) {
            return Optional.empty();
        }
        return wordService.getAllWordsByUser(userId).stream()
                .filter(item -> normalize(item.getWord()).equals(normalize(dto.getWord())))
                .filter(item -> normalize(item.getTranslated()).equals(normalize(dto.getTranslated())))
                .filter(item -> normalize(item.getLanguageCode()).equals(normalize(dto.getLanguageCode())))
                .findFirst();
    }

    private String normalize(String value) {
        return String.valueOf(value == null ? "" : value).trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
