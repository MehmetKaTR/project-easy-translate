package com.mehmetkatr.project_easy_translate.controller;
import jakarta.validation.Valid;

import com.mehmetkatr.project_easy_translate.dto.response.PagedResponse;
import com.mehmetkatr.project_easy_translate.dto.response.StoryDTO;
import com.mehmetkatr.project_easy_translate.entity.Story;
import com.mehmetkatr.project_easy_translate.security.AuthenticatedUserResolver;
import com.mehmetkatr.project_easy_translate.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
            @Valid @RequestBody StoryDTO dto
    ) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Story saved = storyService.addStory(authenticatedUserId, dto);
        return ResponseEntity.ok(new StoryDTO(saved));
    }

    @GetMapping("/all")
    public ResponseEntity<PagedResponse<StoryDTO>> findAll(@RequestParam(required = false) Long userId, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Page<StoryDTO> page = storyService.findByUserId(authenticatedUserId, pageable).map(StoryDTO::new);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @GetMapping("/byUser")
    public ResponseEntity<PagedResponse<StoryDTO>> findByUserId(@RequestParam(required = false) Long userId, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Page<StoryDTO> page = storyService.findByUserId(authenticatedUserId, pageable).map(StoryDTO::new);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @GetMapping("/starred")
    public ResponseEntity<PagedResponse<StoryDTO>> findStarredByUser(@RequestParam(required = false) Long userId, @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Page<StoryDTO> page = storyService.findStarredByUserId(authenticatedUserId, pageable).map(StoryDTO::new);
        return ResponseEntity.ok(PagedResponse.from(page));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteStory(
            @RequestParam(required = false) Long userId,
            @RequestParam Long storyId
    ) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        boolean deleted = storyService.deleteStory(authenticatedUserId, storyId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/updateName")
    public ResponseEntity<StoryDTO> updateStoryName(
            @RequestParam(required = false) Long userId,
            @RequestParam Long storyId,
            @Valid @RequestBody StoryDTO dto
    ) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Story updated = storyService.updateStoryName(authenticatedUserId, storyId, dto.getStoryName());
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new StoryDTO(updated));
    }

    @PutMapping("/updateStarred")
    public ResponseEntity<StoryDTO> updateStoryStarred(
            @RequestParam(required = false) Long userId,
            @RequestParam Long storyId,
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
            @RequestParam Long storyId,
            @Valid @RequestBody StoryDTO dto
    ) {
        Long authenticatedUserId = authenticatedUserResolver.resolveUserId(userId);
        Story updated = storyService.updateStory(authenticatedUserId, storyId, dto);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new StoryDTO(updated));
    }
}
