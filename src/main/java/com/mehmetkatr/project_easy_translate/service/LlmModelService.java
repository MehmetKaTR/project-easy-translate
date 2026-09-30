package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.LlmModel;
import com.mehmetkatr.project_easy_translate.repository.LlmModelRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LlmModelService {

    private final LlmModelRepository llmModelRepository;

    public List<LlmModel> findByName(String name) {
        return llmModelRepository.findByName(name);
    }

    public List<LlmModel> findByStatus(LlmModel.Status status) {
        return llmModelRepository.findByStatus(status);
    }

    public void save(LlmModel llmModel) {
        llmModelRepository.save(llmModel);
    }

    public List<LlmModel> findAll() {
        return llmModelRepository.findAll();
    }

    public LlmModel create(String name, String description, LlmModel.Status status) {
        LlmModel model = LlmModel.builder()
                .name(name)
                .description(description)
                .status(status)
                .build();
        return llmModelRepository.save(model);
    }

    public LlmModel updateStatus(Long id, LlmModel.Status status) {
        LlmModel model = llmModelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("LLM modeli bulunamadı: " + id));
        model.setStatus(status);
        return llmModelRepository.save(model);
    }
}
