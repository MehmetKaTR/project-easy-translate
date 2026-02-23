package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.dto.StoryDTO;
import com.mehmetkatr.project_easy_translate.entity.Story;
import com.mehmetkatr.project_easy_translate.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StoryController {

    private final StoryService storyService;

    @PostMapping("/add")
    public ResponseEntity<StoryDTO> addStory(
            @RequestParam Long userId,
            @RequestBody StoryDTO dto
    ) {
        Story saved = storyService.addStory(userId, dto);
        return ResponseEntity.ok(new StoryDTO(saved));
    }

    @GetMapping("/all")
    public ResponseEntity<List<StoryDTO>> findAll() {
        List<StoryDTO> result = storyService.findAll().stream().map(StoryDTO::new).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/byUser")
    public ResponseEntity<List<StoryDTO>> findByUserId(@RequestParam Long userId) {
        List<StoryDTO> result = storyService.findByUserId(userId).stream().map(StoryDTO::new).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/starred")
    public ResponseEntity<List<StoryDTO>> findStarredByUser(@RequestParam Long userId) {
        List<StoryDTO> result = storyService.findStarredByUserId(userId).stream().map(StoryDTO::new).toList();
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteStory(
            @RequestParam Long userId,
            @RequestParam String storyId
    ) {
        boolean deleted = storyService.deleteStory(userId, storyId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/updateName")
    public ResponseEntity<StoryDTO> updateStoryName(
            @RequestParam Long userId,
            @RequestParam String storyId,
            @RequestBody StoryDTO dto
    ) {
        Story updated = storyService.updateStoryName(userId, storyId, dto.getStoryName());
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new StoryDTO(updated));
    }

    @PutMapping("/updateStarred")
    public ResponseEntity<StoryDTO> updateStoryStarred(
            @RequestParam Long userId,
            @RequestParam String storyId,
            @RequestParam boolean starred
    ) {
        Story updated = storyService.updateStoryStarred(userId, storyId, starred);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new StoryDTO(updated));
    }

    @PutMapping("/update")
    public ResponseEntity<StoryDTO> updateStory(
            @RequestParam Long userId,
            @RequestParam String storyId,
            @RequestBody StoryDTO dto
    ) {
        Story updated = storyService.updateStory(userId, storyId, dto);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new StoryDTO(updated));
    }
}
