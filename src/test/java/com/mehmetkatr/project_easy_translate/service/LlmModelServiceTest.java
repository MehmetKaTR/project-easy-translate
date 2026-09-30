package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.LlmModel;
import com.mehmetkatr.project_easy_translate.repository.LlmModelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmModelServiceTest {

    @Mock
    private LlmModelRepository llmModelRepository;

    @InjectMocks
    private LlmModelService llmModelService;

    @Test
    void createPersistsModelAndReturnsSaved() {
        LlmModel saved = LlmModel.builder()
                .id(1L).name("gemini-2.5-flash").description("desc").status(LlmModel.Status.ACTIVE)
                .build();
        when(llmModelRepository.save(any(LlmModel.class))).thenReturn(saved);

        LlmModel result = llmModelService.create("gemini-2.5-flash", "desc", LlmModel.Status.ACTIVE);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("gemini-2.5-flash");
        verify(llmModelRepository).save(any(LlmModel.class));
    }

    @Test
    void updateStatusChangesStatusOfExistingModel() {
        LlmModel existing = LlmModel.builder()
                .id(5L).name("g").status(LlmModel.Status.ACTIVE).build();
        when(llmModelRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(llmModelRepository.save(existing)).thenReturn(existing);

        LlmModel result = llmModelService.updateStatus(5L, LlmModel.Status.INACTIVE);

        assertThat(result.getStatus()).isEqualTo(LlmModel.Status.INACTIVE);
        verify(llmModelRepository).save(existing);
    }

    @Test
    void updateStatusThrowsWhenModelNotFound() {
        when(llmModelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> llmModelService.updateStatus(99L, LlmModel.Status.INACTIVE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
