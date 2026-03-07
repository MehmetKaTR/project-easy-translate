package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.service.TranslationSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final TranslationSuggestionService translationSuggestionService;

    public static final class TranslateSuggestRequest {
        public String text;
        public String source;
        public String target;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "project_easy_translate",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/translate/suggest")
    public ResponseEntity<Map<String, String>> suggestTranslation(
            @RequestParam String text,
            @RequestParam(defaultValue = "en") String source,
            @RequestParam(defaultValue = "tr") String target
    ) {
        String suggested = translationSuggestionService.suggest(text, source, target);
        return ResponseEntity.ok(Map.of(
                "text", text,
                "source", source,
                "target", target,
                "suggestion", suggested
        ));
    }

    @PostMapping("/translate/suggest")
    public ResponseEntity<Map<String, String>> suggestTranslationPost(
            @RequestBody TranslateSuggestRequest request
    ) {
        String text = request == null ? "" : String.valueOf(request.text == null ? "" : request.text);
        String source = request == null ? "en" : String.valueOf(request.source == null ? "en" : request.source);
        String target = request == null ? "tr" : String.valueOf(request.target == null ? "tr" : request.target);

        String suggested = translationSuggestionService.suggest(text, source, target);
        return ResponseEntity.ok(Map.of(
                "text", text,
                "source", source,
                "target", target,
                "suggestion", suggested
        ));
    }
}
