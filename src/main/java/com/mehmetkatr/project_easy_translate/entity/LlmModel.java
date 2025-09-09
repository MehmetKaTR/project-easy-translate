package com.mehmetkatr.project_easy_translate.entity;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "llm_models")
public class LlmModel {

    @Id
    private String id;

    private String name;

    private String description;

    private Status status;

    public enum Status {
        ACTIVE,
        INACTIVE,
    }
}
