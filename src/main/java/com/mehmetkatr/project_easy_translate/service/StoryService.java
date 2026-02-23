package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.dto.StoryDTO;
import com.mehmetkatr.project_easy_translate.entity.Story;
import com.mehmetkatr.project_easy_translate.repository.StoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StoryService {

    private final StoryRepository storyRepository;

    public Story addStory(Long userId, StoryDTO dto) {
        Date now = new Date();

        String finalStoryName =
                (dto.getStoryName() == null || dto.getStoryName().isBlank())
                        ? "Story " + now.getTime()
                        : dto.getStoryName().trim();

        Story story = Story.builder()
                .userId(userId)
                .storyName(finalStoryName)
                .promptWords(dto.getPromptWords())
                .content(dto.getContent())
                .language(dto.getLanguage() == null ? "English" : dto.getLanguage())
                .starred(Boolean.TRUE.equals(dto.getStarred())) // default false
                .createdAt(now)
                .updatedAt(now)
                .build();

        return storyRepository.save(story);
    }

    public List<Story> findAll() {
        return storyRepository.findAll();
    }

    public List<Story> findByUserId(Long id) {
        return storyRepository.findByUserId(id);
    }

    public List<Story> findStarredByUserId(Long userId) {
        return storyRepository.findByUserIdAndStarred(userId, true);
    }

    public List<Story> findByPromptWord(String promptWord) {
        return storyRepository.findByPromptWords(promptWord);
    }

    public List<Story> findByCreatedAtAfter(LocalDateTime date) {
        Date mongoDate = Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
        return storyRepository.findByCreatedAtAfter(mongoDate);
    }

    public List<Story> findByCreatedAtBefore(LocalDateTime date) {
        Date mongoDate = Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
        return storyRepository.findByCreatedAtBefore(mongoDate);
    }

    public List<Story> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        Date startDate = Date.from(start.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(end.atZone(ZoneId.systemDefault()).toInstant());
        return storyRepository.findByCreatedAtBetween(startDate, endDate);
    }

    public boolean deleteStory(Long userId, String storyId) {
        return storyRepository.findByIdAndUserId(storyId, userId)
                .map(story -> {
                    storyRepository.delete(story);
                    return true;
                })
                .orElse(false);
    }

    public Story updateStoryName(Long userId, String storyId, String storyName) {
        return storyRepository.findByIdAndUserId(storyId, userId)
                .map(story -> {
                    story.setStoryName(storyName == null || storyName.isBlank() ? story.getStoryName() : storyName.trim());
                    story.setUpdatedAt(new Date());
                    return storyRepository.save(story);
                })
                .orElse(null);
    }

    public Story updateStoryStarred(Long userId, String storyId, boolean starred) {
        return storyRepository.findByIdAndUserId(storyId, userId)
                .map(story -> {
                    story.setStarred(starred);
                    story.setUpdatedAt(new Date());
                    return storyRepository.save(story);
                })
                .orElse(null);
    }

    public Story updateStory(Long userId, String storyId, StoryDTO dto) {
        return storyRepository.findByIdAndUserId(storyId, userId)
                .map(story -> {
                    if (dto.getStoryName() != null && !dto.getStoryName().isBlank()) {
                        story.setStoryName(dto.getStoryName().trim());
                    }
                    if (dto.getPromptWords() != null) {
                        story.setPromptWords(dto.getPromptWords());
                    }
                    if (dto.getContent() != null) {
                        story.setContent(dto.getContent());
                    }
                    if (dto.getLanguage() != null && !dto.getLanguage().isBlank()) {
                        story.setLanguage(dto.getLanguage());
                    }
                    if (dto.getStarred() != null) {
                        story.setStarred(dto.getStarred());
                    }
                    story.setUpdatedAt(new Date());
                    return storyRepository.save(story);
                })
                .orElse(null);
    }
}
