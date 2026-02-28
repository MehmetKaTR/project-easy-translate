package com.mehmetkatr.project_easy_translate.exception;

import lombok.Getter;

@Getter
public class ResourceConflictException extends RuntimeException {

    private final String code;

    public ResourceConflictException(String code, String message) {
        super(message);
        this.code = code;
    }
}
