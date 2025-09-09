package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.Story;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StoryRepository extends MongoRepository<Story, Long> {

    List<Story> findByUserId(Long userId);

    List<Story> findByPromptWords(String promptWords);

    List<Story> findByCreatedAtAfter(LocalDateTime date);

    List<Story> findByCreatedAtBefore(LocalDateTime date);

    List<Story> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

}
