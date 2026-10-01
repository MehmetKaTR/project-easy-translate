package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.dto.response.PagedResponse;
import com.mehmetkatr.project_easy_translate.dto.response.WordDTO;
import com.mehmetkatr.project_easy_translate.dto.response.WordDetailResponse;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.entity.UserWord;
import com.mehmetkatr.project_easy_translate.entity.WordList;
import com.mehmetkatr.project_easy_translate.security.AuthenticatedUserResolver;
import com.mehmetkatr.project_easy_translate.service.WordDetailService;
import com.mehmetkatr.project_easy_translate.service.WordListService;
import com.mehmetkatr.project_easy_translate.service.WordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
@Transactional
public class WordController {

    private final WordService wordService;
    private final WordListService wordListService;
    private final WordDetailService wordDetailService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping("/{userWordId}/detail")
    public ResponseEntity<WordDetailResponse> getWordDetail(
            @PathVariable Long userWordId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "tr") String target) {
        Long uid = authenticatedUserResolver.resolveUserId(userId);
        return wordService.findUserWord(uid, userWordId)
                .map(userWord -> ResponseEntity.ok(wordDetailService.getOrCreate(userWord.getWord(), target)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<PagedResponse<WordDTO>> getAllWordsByUser(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Long uid = authenticatedUserResolver.resolveUserId(userId);
        Page<WordDTO> page = wordService.getUserWords(uid, pageable).map(WordDTO::new);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @GetMapping("/all_words")
    public ResponseEntity<PagedResponse<WordDTO>> getAllWordsByAll(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Long uid = authenticatedUserResolver.resolveUserId(userId);
        Optional<WordList> allList = resolveAllList(uid);
        if (allList.isEmpty()) return ResponseEntity.notFound().build();
        Page<WordDTO> page = wordService.getUserWordsByWordList(allList.get().getId(), uid, pageable).map(WordDTO::new);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @GetMapping("/starred")
    public ResponseEntity<PagedResponse<WordDTO>> getStarredWords(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Long uid = authenticatedUserResolver.resolveUserId(userId);
        Page<WordDTO> page = wordService.getStarredUserWords(uid, pageable).map(WordDTO::new);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @GetMapping("/byWordList")
    public ResponseEntity<PagedResponse<WordDTO>> getAllWordsByWordList(
            @RequestParam(required = false) Long userId,
            @RequestParam Long wordListId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Long uid = authenticatedUserResolver.resolveUserId(userId);
        if (wordListService.getWordListsByUserAndWordListId(refUser(uid), wordListId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Page<WordDTO> page = wordService.getUserWordsByWordList(wordListId, uid, pageable).map(WordDTO::new);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @PutMapping("/add")
    public ResponseEntity<WordDTO> addWord(
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody WordDTO dto) {
        Long uid = authenticatedUserResolver.resolveUserId(userId);
        Optional<WordList> allList = resolveAllList(uid);
        if (allList.isEmpty()) return ResponseEntity.badRequest().build();

        UserWord saved = wordService.addUserWord(
                uid, dto.getWord(), dto.getTranslated(), dto.getLanguageCode(), dto.getTargetLanguageCode(),
                Boolean.TRUE.equals(dto.getStarred()), List.of(allList.get()));
        return ResponseEntity.ok(new WordDTO(saved));
    }

    @PutMapping("/addToWordList")
    public ResponseEntity<WordDTO> addWordToWordList(
            @RequestParam(required = false) Long userId,
            @RequestParam long wordListId,
            @Valid @RequestBody WordDTO dto) {
        Long uid = authenticatedUserResolver.resolveUserId(userId);
        Optional<WordList> targetList = wordListService.getWordListsByUserAndWordListId(refUser(uid), wordListId);
        if (targetList.isEmpty()) return ResponseEntity.notFound().build();
        Optional<WordList> allList = resolveAllList(uid);
        if (allList.isEmpty()) return ResponseEntity.badRequest().build();

        UserWord saved = wordService.addUserWord(
                uid, dto.getWord(), dto.getTranslated(), dto.getLanguageCode(), dto.getTargetLanguageCode(),
                Boolean.TRUE.equals(dto.getStarred()), List.of(allList.get(), targetList.get()));
        return ResponseEntity.ok(new WordDTO(saved));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteWord(
            @RequestParam(required = false) Long userId,
            @RequestParam Long wordId) {
        Long uid = authenticatedUserResolver.resolveUserId(userId);
        boolean deleted = wordService.deleteUserWord(uid, wordId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/update")
    public ResponseEntity<WordDTO> updateWord(
            @RequestParam(required = false) Long userId,
            @RequestParam Long wordId,
            @Valid @RequestBody WordDTO dto) {
        Long uid = authenticatedUserResolver.resolveUserId(userId);
        Optional<UserWord> uwOpt = wordService.findUserWord(uid, wordId);
        if (uwOpt.isEmpty()) return ResponseEntity.notFound().build();

        UserWord userWord = uwOpt.get();
        // Not: kelime metni (word) global/paylasimli oldugu icin burada degistirilmez; sadece kullaniciya ait alanlar.
        if (dto.getTranslated() != null) userWord.setTranslated(dto.getTranslated());
        if (dto.getStarred() != null) userWord.setStarred(dto.getStarred());

        return ResponseEntity.ok(new WordDTO(wordService.save(userWord)));
    }

    @PutMapping("/updateStarred")
    public ResponseEntity<WordDTO> updateStarred(
            @RequestParam(required = false) Long userId,
            @RequestParam Long wordId,
            @RequestParam boolean starred) {
        Long uid = authenticatedUserResolver.resolveUserId(userId);
        UserWord updated = wordService.updateStarred(uid, wordId, starred);
        return ResponseEntity.ok(new WordDTO(updated));
    }

    private Optional<WordList> resolveAllList(Long uid) {
        return wordListService.getWordListsByUserAndName(refUser(uid), "All");
    }

    private User refUser(Long uid) {
        User user = new User();
        user.setId(uid);
        return user;
    }
}
