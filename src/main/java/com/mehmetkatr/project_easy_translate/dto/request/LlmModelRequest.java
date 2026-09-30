package com.mehmetkatr.project_easy_translate.dto.request;

import com.mehmetkatr.project_easy_translate.entity.LlmModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin: yeni LLM modeli oluşturma isteği. */
public record LlmModelRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description,
        @NotNull LlmModel.Status status
) {
}
