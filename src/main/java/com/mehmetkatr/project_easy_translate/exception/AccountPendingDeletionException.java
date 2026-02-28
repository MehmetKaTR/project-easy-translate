package com.mehmetkatr.project_easy_translate.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AccountPendingDeletionException extends RuntimeException {

    private final LocalDateTime retryAt;

    public AccountPendingDeletionException(String message, LocalDateTime retryAt) {
        super(message);
        this.retryAt = retryAt;
    }
}
