package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Minimal Gemini istemcisi: bir prompt gonderir, modelin metin cevabini doner.
 * (StoryGenerationService kendi Gemini cagrisini ayri tutuyor; bu, WordDetail fallback icin sade bir istemci.)
 */
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    @Value("${gemini.api-version:v1beta}")
    private String apiVersion;

    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key yapilandirilmamis");
        }
        String url = "https://generativelanguage.googleapis.com/" + apiVersion
                + "/models/" + model + ":generateContent?key=" + apiKey;

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
            throw new IllegalStateException("Gemini cagrisi basarisiz: " + e.getMessage(), e);
        }
    }
}
