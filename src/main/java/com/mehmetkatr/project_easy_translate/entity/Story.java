package com.mehmetkatr.project_easy_translate.entity;
import com.mehmetkatr.project_easy_translate.entity.base.BaseDocument;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "stories")
public class Story extends BaseDocument{

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

    @Field("translated_story")
    private String translatedStory;

    @Field("translated_language")
    private String translatedLanguage;

    @Field("translated_words_csv")
    private String translatedWordsCsv;

    @Field("word_mappings_json")
    private String wordMappingsJson;

    private String language;

    @Builder.Default
    private boolean starred = false;

}
