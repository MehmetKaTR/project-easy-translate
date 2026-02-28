package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.TokenUsageLog;
import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.repository.TokenUsageLogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TokenUsageLogService {

    private final TokenUsageLogRepository tokenUsageLogRepository;

    public List<TokenUsageLog> findByUser(User user) {
        return tokenUsageLogRepository.findByUser(user);
    }

    public TokenUsageLog findByStoryId(String storyId) {
        return tokenUsageLogRepository.findByStoryId(storyId);
    }

    public void save(TokenUsageLog tokenUsageLog) {
        tokenUsageLogRepository.save(tokenUsageLog);
    }

    public int findTokenUsageByStoryId(String storyId) {
        TokenUsageLog tokenUsageLog = tokenUsageLogRepository.findByStoryId(storyId);
        return tokenUsageLog.getTokensUsed();
    }

    public int sumTokensByUserBetween(User user, LocalDateTime start, LocalDateTime end) {
        Long total = tokenUsageLogRepository.sumTokensByUserBetween(user, start, end);
        return total == null ? 0 : total.intValue();
    }

    public long countByUserBetween(User user, LocalDateTime start, LocalDateTime end) {
        Long total = tokenUsageLogRepository.countByUserBetween(user, start, end);
        return total == null ? 0L : total;
    }

}
