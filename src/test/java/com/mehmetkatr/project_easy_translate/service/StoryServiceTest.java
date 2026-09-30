package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.dto.response.StoryDTO;
import com.mehmetkatr.project_easy_translate.entity.Story;
import com.mehmetkatr.project_easy_translate.repository.StoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryServiceTest {

    @Mock
    private StoryRepository storyRepository;

    @InjectMocks
    private StoryService storyService;

    @Test
    void addStoryPersistsWithOwnerAndName() {
        when(storyRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StoryDTO dto = new StoryDTO();
        dto.setStoryName("My Story");
        dto.setContent("Once upon a time");

        Story result = storyService.addStory(7L, dto);

        assertThat(result.getUserId()).isEqualTo(7L);
        assertThat(result.getStoryName()).isEqualTo("My Story");
        assertThat(result.getContent()).isEqualTo("Once upon a time");
    }

    @Test
    void addStoryGeneratesDefaultNameWhenBlank() {
        when(storyRepository.save(any(Story.class))).thenAnswer(invocation -> invocation.getArgument(0));
        StoryDTO dto = new StoryDTO();

        Story result = storyService.addStory(7L, dto);

        assertThat(result.getStoryName()).startsWith("Story ");
    }

    @Test
    void deleteStoryReturnsFalseWhenNotFound() {
        when(storyRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.empty());

        assertThat(storyService.deleteStory(7L, 1L)).isFalse();
    }

    @Test
    void deleteStoryRemovesAndReturnsTrueWhenFound() {
        Story story = Story.builder().id(1L).userId(7L).build();
        when(storyRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(story));

        boolean deleted = storyService.deleteStory(7L, 1L);

        assertThat(deleted).isTrue();
        verify(storyRepository).delete(story);
    }

    @Test
    void updateStoryNameChangesName() {
        Story story = Story.builder().id(1L).userId(7L).storyName("Old").build();
        when(storyRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(story));
        when(storyRepository.save(story)).thenReturn(story);

        Story result = storyService.updateStoryName(7L, 1L, "New");

        assertThat(result.getStoryName()).isEqualTo("New");
    }

    @Test
    void updateStoryStarredReturnsNullWhenNotFound() {
        when(storyRepository.findByIdAndUserId(1L, 7L)).thenReturn(Optional.empty());

        assertThat(storyService.updateStoryStarred(7L, 1L, true)).isNull();
    }
}
