package com.mehmetkatr.project_easy_translate.service;

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

    public List<Story> findAll() {
        return storyRepository.findAll();
    }

    public List<Story> findByUserId(Long id) {
        return storyRepository.findByUserId(id);
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

}
