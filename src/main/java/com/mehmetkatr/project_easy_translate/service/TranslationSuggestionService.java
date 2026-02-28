package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class TranslationSuggestionService {

    private final ObjectMapper objectMapper;

    public String suggest(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String source = normalizeLang(sourceLang, "en");
        String target = normalizeLang(targetLang, "tr");

        String uri = UriComponentsBuilder
                .fromUriString("https://translate.googleapis.com/translate_a/single")
                .queryParam("client", "gtx")
                .queryParam("sl", source)
                .queryParam("tl", target)
                .queryParam("dt", "t")
                .queryParam("q", text.trim())
                .toUriString();

        String raw = RestClient.create()
                .get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(raw == null ? "[]" : raw);
            JsonNode sentences = root.path(0);
            if (!sentences.isArray()) return "";

            StringBuilder out = new StringBuilder();
            for (JsonNode sentence : sentences) {
                String part = sentence.path(0).asText("");
                if (!part.isBlank()) {
                    if (out.length() > 0) out.append(' ');
                    out.append(part.trim());
                }
            }
            return out.toString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String normalizeLang(String input, String fallback) {
        String value = (input == null ? "" : input.trim().toLowerCase());
        if (value.isBlank()) return fallback;
        return switch (value) {
            case "en", "tr", "de", "fr", "es", "it", "pt", "ru", "ar", "ja", "ko", "zh" -> value;
            default -> fallback;
        };
    }
}
