package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.dto.request.StoryGenerateRequest;
import com.mehmetkatr.project_easy_translate.dto.response.StoryGenerateResponse;
import com.mehmetkatr.project_easy_translate.dto.response.StoryLimitStatusResponse;
import com.mehmetkatr.project_easy_translate.dto.request.StoryWordValidationRequest;
import com.mehmetkatr.project_easy_translate.dto.response.StoryWordValidationResponse;
import com.mehmetkatr.project_easy_translate.security.AuthenticatedUserResolver;
import com.mehmetkatr.project_easy_translate.service.StoryGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/story")
@RequiredArgsConstructor
public class StoryGenerationController {

    private final StoryGenerationService storyGenerationService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @PostMapping("/generate")
    public ResponseEntity<StoryGenerateResponse> generateStory(
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody StoryGenerateRequest request
    ) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        StoryGenerateResponse response = storyGenerationService.generateStory(authenticatedUserId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-words")
    public ResponseEntity<StoryWordValidationResponse> validateWords(
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody StoryWordValidationRequest request
    ) {
        authenticatedUserResolver.resolveUserId(userId);
        StoryWordValidationResponse response = storyGenerationService.validateWords(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/limit-status")
    public ResponseEntity<StoryLimitStatusResponse> getLimitStatus(
            @RequestParam(required = false) Long userId
    ) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        StoryLimitStatusResponse response = storyGenerationService.getDailyLimitStatus(authenticatedUserId);
        return ResponseEntity.ok(response);
    }
}
