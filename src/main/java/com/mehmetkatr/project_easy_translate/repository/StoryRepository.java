package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.Story;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface StoryRepository extends MongoRepository<Story, String> {

    List<Story> findByUserId(Long userId);

    List<Story> findByUserIdAndStarred(Long userId, boolean starred);

    List<Story> findByPromptWords(String promptWords);

    List<Story> findByCreatedAtAfter(LocalDateTime date);

    List<Story> findByCreatedAtBefore(LocalDateTime date);

    List<Story> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    Optional<Story> findByIdAndUserId(String id, Long userId);

    long deleteByUserId(Long userId);
}
