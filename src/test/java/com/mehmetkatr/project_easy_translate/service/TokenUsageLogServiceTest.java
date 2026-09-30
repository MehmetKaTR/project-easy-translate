package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.User;
import com.mehmetkatr.project_easy_translate.repository.TokenUsageLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenUsageLogServiceTest {

    @Mock
    private TokenUsageLogRepository tokenUsageLogRepository;

    @InjectMocks
    private TokenUsageLogService tokenUsageLogService;

    private final LocalDateTime start = LocalDateTime.now().minusDays(1);
    private final LocalDateTime end = LocalDateTime.now();

    @Test
    void sumReturnsZeroWhenRepositoryReturnsNull() {
        when(tokenUsageLogRepository.sumTokensByUserBetween(any(), any(), any())).thenReturn(null);

        assertThat(tokenUsageLogService.sumTokensByUserBetween(new User(), start, end)).isZero();
    }

    @Test
    void sumReturnsRepositoryValue() {
        when(tokenUsageLogRepository.sumTokensByUserBetween(any(), any(), any())).thenReturn(1500L);

        assertThat(tokenUsageLogService.sumTokensByUserBetween(new User(), start, end)).isEqualTo(1500);
    }

    @Test
    void sumByUserIdDelegatesToUserBasedQuery() {
        when(tokenUsageLogRepository.sumTokensByUserBetween(any(User.class), any(), any())).thenReturn(200L);

        assertThat(tokenUsageLogService.sumTokensByUserIdBetween(5L, start, end)).isEqualTo(200);
    }

    @Test
    void countReturnsZeroWhenRepositoryReturnsNull() {
        when(tokenUsageLogRepository.countByUserBetween(any(), any(), any())).thenReturn(null);

        assertThat(tokenUsageLogService.countByUserBetween(new User(), start, end)).isZero();
    }
}
