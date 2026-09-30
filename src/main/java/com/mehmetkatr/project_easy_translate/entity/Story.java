package com.mehmetkatr.project_easy_translate.entity;

import com.mehmetkatr.project_easy_translate.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "stories",
        indexes = {
                @Index(name = "idx_stories_user_id", columnList = "user_id"),
                @Index(name = "idx_stories_starred", columnList = "starred"),
                @Index(name = "idx_stories_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Story extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "story_name")
    private String storyName;

    @Column(name = "prompt_words", columnDefinition = "TEXT")
    private String promptWords;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "turkish_translation", columnDefinition = "LONGTEXT")
    private String turkishTranslation;

    @Column(name = "translated_story", columnDefinition = "LONGTEXT")
    private String translatedStory;

    @Column(name = "translated_language")
    private String translatedLanguage;

    @Column(name = "translated_words_csv", columnDefinition = "LONGTEXT")
    private String translatedWordsCsv;

    // JSON verisi metin olarak saklanır (LONGTEXT). İleride MySQL JSON kolonuna yükseltilebilir.
    @Column(name = "word_mappings_json", columnDefinition = "LONGTEXT")
    private String wordMappingsJson;

    private String language;

    @Builder.Default
    @Column(nullable = false)
    private boolean starred = false;
}
