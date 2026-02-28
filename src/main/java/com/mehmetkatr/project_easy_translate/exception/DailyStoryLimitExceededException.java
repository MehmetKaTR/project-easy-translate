package com.mehmetkatr.project_easy_translate.exception;

import java.time.LocalDateTime;

public class DailyStoryLimitExceededException extends RuntimeException {

    private final int dailyLimit;
    private final int usedToday;
    private final LocalDateTime resetAt;

    public DailyStoryLimitExceededException(String message, int dailyLimit, int usedToday, LocalDateTime resetAt) {
        super(message);
        this.dailyLimit = dailyLimit;
        this.usedToday = usedToday;
        this.resetAt = resetAt;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public int getUsedToday() {
        return usedToday;
    }

    public LocalDateTime getResetAt() {
        return resetAt;
    }
}
