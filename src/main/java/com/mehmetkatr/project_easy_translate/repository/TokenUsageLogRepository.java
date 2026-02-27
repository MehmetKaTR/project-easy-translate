package com.mehmetkatr.project_easy_translate.repository;

import com.mehmetkatr.project_easy_translate.entity.TokenUsageLog;
import com.mehmetkatr.project_easy_translate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TokenUsageLogRepository extends JpaRepository<TokenUsageLog, Long> {

    List<TokenUsageLog> findByUser(User user);

    TokenUsageLog findByStoryId(String storyId);

    @Query("SELECT COALESCE(SUM(t.tokensUsed), 0) FROM TokenUsageLog t WHERE t.user = :user AND t.createdAt >= :start AND t.createdAt < :end")
    Long sumTokensByUserBetween(
            @Param("user") User user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}
