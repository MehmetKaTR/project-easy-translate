package com.mehmetkatr.project_easy_translate.dto.response;

import com.mehmetkatr.project_easy_translate.entity.LlmModel;

import java.time.LocalDateTime;

/** Admin: LLM modeli yanıtı. */
public record LlmModelResponse(
        Long id,
        String name,
        String description,
        LlmModel.Status status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static LlmModelResponse from(LlmModel model) {
        return new LlmModelResponse(
                model.getId(),
                model.getName(),
                model.getDescription(),
                model.getStatus(),
                model.getCreatedAt(),
                model.getUpdatedAt()
        );
    }
}
