package com.mehmetkatr.project_easy_translate.entity;
import com.mehmetkatr.project_easy_translate.entity.base.BaseDocument;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "llm_models")
public class LlmModel extends BaseDocument{

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
