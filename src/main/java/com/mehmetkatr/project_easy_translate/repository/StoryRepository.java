package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.Story;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface StoryRepository extends MongoRepository<Story, Long> {

    List<Story> findByUserId(Long userId);

    List<Story> findByPromptWords(String promptWords);

    List<Story> findByCreatedAtAfter(Date date);

    List<Story> findByCreatedAtBefore(Date date);

    List<Story> findByCreatedAtBetween(Date startDate, Date endDate);

}
