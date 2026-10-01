package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Free Dictionary API (dictionaryapi.dev) istemcisi — ucretsiz, key yok.
 * Kelime bulunamazsa (404) ya da herhangi bir hata olursa Optional.empty() doner → Gemini fallback.
 */
@Component
@RequiredArgsConstructor
public class FreeDictionaryClient {

    private static final Logger log = LoggerFactory.getLogger(FreeDictionaryClient.class);
    private static final int MAX_EXAMPLES = 2;
    private static final int MAX_SYNONYMS = 6;

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public Optional<Result> lookup(String word, String languageCode) {
        String lang = (languageCode == null || languageCode.isBlank()) ? "en" : languageCode.toLowerCase();
        try {
            String body = restClient.get()
                    .uri("https://api.dictionaryapi.dev/api/v2/entries/{lang}/{word}", lang, word.trim())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body == null ? "[]" : body);
            if (!root.isArray() || root.isEmpty()) return Optional.empty();

            JsonNode meanings = root.get(0).path("meanings");
            if (!meanings.isArray() || meanings.isEmpty()) return Optional.empty();

            JsonNode firstMeaning = meanings.get(0);
            String partOfSpeech = textOrNull(firstMeaning.path("partOfSpeech"));

            JsonNode definitions = firstMeaning.path("definitions");
            String definition = (definitions.isArray() && !definitions.isEmpty())
                    ? textOrNull(definitions.get(0).path("definition"))
                    : null;

            List<String> examples = new ArrayList<>();
            for (JsonNode d : definitions) {
                String ex = textOrNull(d.path("example"));
                if (ex != null) {
                    examples.add(ex);
                    if (examples.size() >= MAX_EXAMPLES) break;
                }
            }

            List<String> synonyms = new ArrayList<>();
            for (JsonNode s : firstMeaning.path("synonyms")) {
                synonyms.add(s.asText());
                if (synonyms.size() >= MAX_SYNONYMS) break;
            }

            if (definition == null && examples.isEmpty()) return Optional.empty();
            return Optional.of(new Result(partOfSpeech, definition, examples, synonyms));
        } catch (Exception e) {
            log.info("Free Dictionary bulunamadi/hata ({}): {}", word, e.getMessage());
            return Optional.empty();
        }
    }

    private String textOrNull(JsonNode node) {
        String value = node.asText(null);
        return (value == null || value.isBlank()) ? null : value;
    }

    public record Result(String partOfSpeech, String definition, List<String> examples, List<String> synonyms) {
    }
}
