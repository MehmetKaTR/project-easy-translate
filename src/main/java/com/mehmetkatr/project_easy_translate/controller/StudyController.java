package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.dto.response.StudyCategoriesResponse;
import com.mehmetkatr.project_easy_translate.dto.response.StudyItemResponse;
import com.mehmetkatr.project_easy_translate.dto.response.StudyStatsResponse;
import com.mehmetkatr.project_easy_translate.service.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Kelime calisma (quiz) + SRS uc noktalari.
 * Kelimeler universal; ilerleme/katsayi kullaniciya ozel (userId).
 */
@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    public static final class AnswerRequest {
        public Long entryId;
        public String rating; // AGAIN | HARD | GOOD | EASY
    }

    /** Siradaki quiz kelimeleri (due + yeni). exclude: bu oturumda zaten gosterilen id'ler. */
    @GetMapping("/next")
    public ResponseEntity<List<StudyItemResponse>> next(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "20") int count,
            @RequestParam(required = false) List<Long> exclude,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(studyService.next(userId, count, exclude, category));
    }

    /** Calisma kategorileri (seviye + konu + Anki). */
    @GetMapping("/categories")
    public ResponseEntity<StudyCategoriesResponse> categories() {
        return ResponseEntity.ok(studyService.categories());
    }

    /** KAMP modu: kategorinin belirli grubu (sabit dilim). */
    @GetMapping("/group")
    public ResponseEntity<List<StudyItemResponse>> group(
            @RequestParam Long userId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int group,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(studyService.group(userId, category, group, size));
    }

    /** YANLISLARIM. */
    @GetMapping("/mistakes")
    public ResponseEntity<List<StudyItemResponse>> mistakes(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "20") int count
    ) {
        return ResponseEntity.ok(studyService.mistakes(userId, count));
    }

    /** Cevabi isle (SM-2 katsayi/aralik guncelle). */
    @PostMapping("/answer")
    public ResponseEntity<Void> answer(
            @RequestParam Long userId,
            @RequestBody AnswerRequest req
    ) {
        if (req == null || req.entryId == null) return ResponseEntity.badRequest().build();
        studyService.answer(userId, req.entryId, req.rating);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<StudyStatsResponse> stats(@RequestParam Long userId) {
        return ResponseEntity.ok(studyService.stats(userId));
    }
}
