package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.dto.response.StoryDTO;
import com.mehmetkatr.project_easy_translate.entity.Story;
import com.mehmetkatr.project_easy_translate.repository.StoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StoryService {

    private final StoryRepository storyRepository;

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String resolveTranslatedStory(StoryDTO dto) {
        String translatedStory = clean(dto.getTranslatedStory());
        if (translatedStory != null) return translatedStory;
        return clean(dto.getTurkishTranslation());
    }

    private String resolveTranslatedLanguage(StoryDTO dto) {
        String translatedLanguage = clean(dto.getTranslatedLanguage());
        if (translatedLanguage != null) return translatedLanguage.toLowerCase();
        return clean(dto.getTurkishTranslation()) != null ? "tr" : null;
    }

    private String resolveTurkishTranslation(StoryDTO dto, String translatedStory, String translatedLanguage) {
        String turkishTranslation = clean(dto.getTurkishTranslation());
        if (turkishTranslation != null) return turkishTranslation;
        if ("tr".equalsIgnoreCase(translatedLanguage)) return translatedStory;
        return null;
    }

    public Story addStory(Long userId, StoryDTO dto) {
        Date now = new Date();

        String finalStoryName =
                (dto.getStoryName() == null || dto.getStoryName().isBlank())
                        ? "Story " + now.getTime()
                        : dto.getStoryName().trim();
        String translatedStory = resolveTranslatedStory(dto);
        String translatedLanguage = resolveTranslatedLanguage(dto);
        String turkishTranslation = resolveTurkishTranslation(dto, translatedStory, translatedLanguage);

        Story story = Story.builder()
                .userId(userId)
                .storyName(finalStoryName)
                .promptWords(dto.getPromptWords())
                .content(dto.getContent())
                .turkishTranslation(turkishTranslation)
                .translatedStory(translatedStory)
                .translatedLanguage(translatedLanguage)
                .translatedWordsCsv(clean(dto.getTranslatedWordsCsv()))
                .wordMappingsJson(clean(dto.getWordMappingsJson()))
                .language(dto.getLanguage() == null ? "English" : dto.getLanguage())
                .starred(Boolean.TRUE.equals(dto.getStarred())) // default false
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
        return storyRepository.findByCreatedAtAfter(date);
    }

    public List<Story> findByCreatedAtBefore(LocalDateTime date) {
        return storyRepository.findByCreatedAtBefore(date);
    }

    public List<Story> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        return storyRepository.findByCreatedAtBetween(start, end);
    }

    public boolean deleteStory(Long userId, Long storyId) {
        return storyRepository.findByIdAndUserId(storyId, userId)
                .map(story -> {
                    storyRepository.delete(story);
                    return true;
                })
                .orElse(false);
    }

    public Story updateStoryName(Long userId, Long storyId, String storyName) {
        return storyRepository.findByIdAndUserId(storyId, userId)
                .map(story -> {
                    story.setStoryName(storyName == null || storyName.isBlank() ? story.getStoryName() : storyName.trim());
                    return storyRepository.save(story);
                })
                .orElse(null);
    }

    public Story updateStoryStarred(Long userId, Long storyId, boolean starred) {
        return storyRepository.findByIdAndUserId(storyId, userId)
                .map(story -> {
                    story.setStarred(starred);
                    return storyRepository.save(story);
                })
                .orElse(null);
    }

    public Story updateStory(Long userId, Long storyId, StoryDTO dto) {
        return storyRepository.findByIdAndUserId(storyId, userId)
                .map(story -> {
                    String translatedStory = resolveTranslatedStory(dto);
                    String translatedLanguage = resolveTranslatedLanguage(dto);
                    String turkishTranslation = resolveTurkishTranslation(dto, translatedStory, translatedLanguage);

                    if (dto.getStoryName() != null && !dto.getStoryName().isBlank()) {
                        story.setStoryName(dto.getStoryName().trim());
                    }
                    if (dto.getPromptWords() != null) {
                        story.setPromptWords(dto.getPromptWords());
                    }
                    if (dto.getContent() != null) {
                        story.setContent(dto.getContent());
                    }
                    if (dto.getTurkishTranslation() != null || translatedStory != null || translatedLanguage != null) {
                        story.setTurkishTranslation(turkishTranslation);
                    }
                    if (translatedStory != null) {
                        story.setTranslatedStory(translatedStory);
                    }
                    if (translatedLanguage != null) {
                        story.setTranslatedLanguage(translatedLanguage);
                    }
                    if (dto.getTranslatedWordsCsv() != null) {
                        story.setTranslatedWordsCsv(clean(dto.getTranslatedWordsCsv()));
                    }
                    if (dto.getWordMappingsJson() != null) {
                        story.setWordMappingsJson(clean(dto.getWordMappingsJson()));
                    }
                    if (dto.getLanguage() != null && !dto.getLanguage().isBlank()) {
                        story.setLanguage(dto.getLanguage());
                    }
                    if (dto.getStarred() != null) {
                        story.setStarred(dto.getStarred());
                    }
                    return storyRepository.save(story);
                })
                .orElse(null);
    }
}
