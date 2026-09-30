package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRateLimitServiceTest {

    private AuthRateLimitService authRateLimitService;

    @BeforeEach
    void setUp() {
        authRateLimitService = new AuthRateLimitService();
    }

    @Test
    void allowsRequestsUnderLimit() {
        Duration window = Duration.ofMinutes(1);

        assertThatCode(() -> {
            for (int i = 0; i < 3; i++) {
                authRateLimitService.assertAllowed("user@test", 3, window);
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void throwsWhenLimitExceeded() {
        String key = "user@test";
        int max = 3;
        Duration window = Duration.ofMinutes(1);
        for (int i = 0; i < max; i++) {
            authRateLimitService.assertAllowed(key, max, window);
        }

        assertThatThrownBy(() -> authRateLimitService.assertAllowed(key, max, window))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void keepsSeparateCountersPerKey() {
        Duration window = Duration.ofMinutes(1);
        authRateLimitService.assertAllowed("A", 1, window);

        assertThatCode(() -> authRateLimitService.assertAllowed("B", 1, window))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNonPositiveMaxRequests() {
        assertThatThrownBy(() -> authRateLimitService.assertAllowed("k", 0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
