package com.mehmetkatr.project_easy_translate.controller;

import com.mehmetkatr.project_easy_translate.dto.request.LlmModelRequest;
import com.mehmetkatr.project_easy_translate.dto.response.LlmModelResponse;
import com.mehmetkatr.project_easy_translate.entity.LlmModel;
import com.mehmetkatr.project_easy_translate.service.LlmModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin: yapay zeka (LLM) modellerini yönetir — listeleme, ekleme, aktif/pasif yapma.
 * Tüm uçlar ADMIN rolü ile korunur (/api/admin/** SecurityConfig'te kısıtlı).
 */
@RestController
@RequestMapping("/api/admin/llm-models")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LlmModelController {

    private final LlmModelService llmModelService;

    @GetMapping
    public ResponseEntity<List<LlmModelResponse>> list() {
        List<LlmModelResponse> response = llmModelService.findAll().stream()
                .map(LlmModelResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<LlmModelResponse> create(@Valid @RequestBody LlmModelRequest request) {
        LlmModel saved = llmModelService.create(request.name(), request.description(), request.status());
        return ResponseEntity.status(HttpStatus.CREATED).body(LlmModelResponse.from(saved));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<LlmModelResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam LlmModel.Status status
    ) {
        return ResponseEntity.ok(LlmModelResponse.from(llmModelService.updateStatus(id, status)));
    }
}
