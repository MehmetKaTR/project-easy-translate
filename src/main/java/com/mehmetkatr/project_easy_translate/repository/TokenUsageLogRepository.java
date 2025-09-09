package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.TokenUsageLog;
import com.mehmetkatr.project_easy_translate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TokenUsageLogRepository extends JpaRepository<TokenUsageLog, Long> {

    List<TokenUsageLog> findByUser(User user);

    List<TokenUsageLog> findByUserAndStoryId(User user, String storyId);

    List<TokenUsageLog> findByStoryId(String storyId);

}
