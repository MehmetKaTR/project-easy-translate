package com.mehmetkatr.project_easy_translate.dto;

import com.mehmetkatr.project_easy_translate.entity.Story;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryDTO {
    private String id;
    private Long userId;
    private String storyName;
    private String promptWords;
    private String content;
    private String language;
    private Boolean starred; // nullable: update requestte gelmeyebilir
    private Date createdAt;
    private Date updatedAt;

    public StoryDTO(Story story) {
        this.id = story.getId();
        this.userId = story.getUserId();
        this.storyName = story.getStoryName();
        this.promptWords = story.getPromptWords();
        this.content = story.getContent();
        this.language = story.getLanguage();
        this.starred = story.isStarred();
        this.createdAt = story.getCreatedAt();
        this.updatedAt = story.getUpdatedAt();
    }
}
