package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.dto.StoryDTO;
import com.mehmetkatr.project_easy_translate.entity.Story;
import com.mehmetkatr.project_easy_translate.security.AuthenticatedUserResolver;
import com.mehmetkatr.project_easy_translate.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @PostMapping("/add")
    public ResponseEntity<StoryDTO> addStory(
            @RequestParam(required = false) Long userId,
            @RequestBody StoryDTO dto
    ) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Story saved = storyService.addStory(authenticatedUserId, dto);
        return ResponseEntity.ok(new StoryDTO(saved));
    }

    @GetMapping("/all")
    public ResponseEntity<List<StoryDTO>> findAll(@RequestParam(required = false) Long userId) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        List<StoryDTO> result = storyService.findByUserId(authenticatedUserId).stream().map(StoryDTO::new).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/byUser")
    public ResponseEntity<List<StoryDTO>> findByUserId(@RequestParam(required = false) Long userId) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        List<StoryDTO> result = storyService.findByUserId(authenticatedUserId).stream().map(StoryDTO::new).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/starred")
    public ResponseEntity<List<StoryDTO>> findStarredByUser(@RequestParam(required = false) Long userId) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        List<StoryDTO> result = storyService.findStarredByUserId(authenticatedUserId).stream().map(StoryDTO::new).toList();
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteStory(
            @RequestParam(required = false) Long userId,
            @RequestParam String storyId
    ) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        boolean deleted = storyService.deleteStory(authenticatedUserId, storyId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/updateName")
    public ResponseEntity<StoryDTO> updateStoryName(
            @RequestParam(required = false) Long userId,
            @RequestParam String storyId,
            @RequestBody StoryDTO dto
    ) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Story updated = storyService.updateStoryName(authenticatedUserId, storyId, dto.getStoryName());
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new StoryDTO(updated));
    }

    @PutMapping("/updateStarred")
    public ResponseEntity<StoryDTO> updateStoryStarred(
            @RequestParam(required = false) Long userId,
            @RequestParam String storyId,
            @RequestParam boolean starred
    ) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Story updated = storyService.updateStoryStarred(authenticatedUserId, storyId, starred);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new StoryDTO(updated));
    }

    @PutMapping("/update")
    public ResponseEntity<StoryDTO> updateStory(
            @RequestParam(required = false) Long userId,
            @RequestParam String storyId,
            @RequestBody StoryDTO dto
    ) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Story updated = storyService.updateStory(authenticatedUserId, storyId, dto);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new StoryDTO(updated));
    }
}
