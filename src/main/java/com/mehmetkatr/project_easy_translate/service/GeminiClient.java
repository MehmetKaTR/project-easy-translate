package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gemini istemcisi: bir prompt gonderir, modelin metin cevabini doner.
 * GEMINI_MODEL birincil; basarisizsa (404/429/503) GEMINI_FALLBACK_MODELS sirayla denenir.
 */
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash-lite}")
    private String model;

    @Value("${gemini.fallback-models:}")
    private String fallbackModels;

    @Value("${gemini.api-version:v1beta}")
    private String apiVersion;

    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key yapilandirilmamis");
        }
        Exception last = null;
        for (String m : modelChain()) {
            try {
                String out = callModel(m, prompt);
                if (out != null && !out.isBlank()) return out;
            } catch (Exception e) {
                last = e;
                log.info("[gemini] model {} basarisiz: {}", m, e.getMessage());
            }
        }
        throw new IllegalStateException("Gemini cagrisi basarisiz (tum modeller): "
                + (last == null ? "bos cevap" : last.getMessage()), last);
    }

    /** Birincil + fallback modeller, sirali ve tekilsiz. */
    private List<String> modelChain() {
        Set<String> chain = new LinkedHashSet<>();
        if (model != null && !model.isBlank()) chain.add(model.trim());
        if (fallbackModels != null && !fallbackModels.isBlank()) {
            for (String f : fallbackModels.split(",")) {
                String t = f.trim();
                if (!t.isEmpty()) chain.add(t);
            }
        }
        return new ArrayList<>(chain);
    }

    private String callModel(String modelName, String prompt) {
        String url = "https://generativelanguage.googleapis.com/" + apiVersion
                + "/models/" + modelName + ":generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        try {
            String response = restClient.post()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            return root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText("");
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
