package com.mehmetkatr.project_easy_translate.entity;

import org.springframework.data.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "stories")
public class Story {

    @Id
    private String id;

    @Field("user_id")
    private Long userId;

    @Field("story_name")
    private String storyName;

    @Field("prompt_words")
    private String promptWords;

    private String content;

    @Field("turkish_translation")
    private String turkishTranslation;

    private String language;

    @Builder.Default
    private boolean starred = false;

    @Field("created_at")
    private Date createdAt;

    @Field("updated_at")
    private Date updatedAt;
}
