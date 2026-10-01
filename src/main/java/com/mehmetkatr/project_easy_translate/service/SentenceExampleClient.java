package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tatoeba (tatoeba.org) ornek cumle kaynagi — ucretsiz, key yok, sinirsiz.
 * Free Dictionary tanim verip ornek cumle vermedigi durumlarda ( or. "give rise to")
 * devreye girer; buldugu cumleler daha sonra Google Translate ile hedef dile cevrilir.
 */
@Component
@RequiredArgsConstructor
public class SentenceExampleClient {

    private static final Logger log = LoggerFactory.getLogger(SentenceExampleClient.class);

    // Tatoeba ISO 639-3 dil kodlari
    private static final Map<String, String> LANG_3 = Map.of(
            "en", "eng", "tr", "tur", "de", "deu", "es", "spa",
            "fr", "fra", "it", "ita", "pt", "por", "ru", "rus");

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public List<String> lookup(String phrase, String languageCode, int max) {
        String term = phrase == null ? "" : phrase.trim();
        if (term.isEmpty()) return List.of();
        String from = LANG_3.getOrDefault(
                (languageCode == null ? "en" : languageCode.trim().toLowerCase()), "eng");

        try {
            URI uri = UriComponentsBuilder
                    .fromUriString("https://tatoeba.org/en/api_v0/search")
                    .queryParam("from", from)
                    .queryParam("query", term)
                    .queryParam("sort", "relevance")
                    .encode()
                    .build()
                    .toUri();

            String body = restClient.get()
                    .uri(uri)
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            JsonNode results = root.path("results");
            if (!results.isArray()) return List.of();

            String needle = term.toLowerCase();
            List<String> exact = new ArrayList<>();
            List<String> fallback = new ArrayList<>();
            for (JsonNode item : results) {
                String text = item.path("text").asText(null);
                if (text == null || text.isBlank()) continue;
                String clean = text.trim();
                if (clean.length() > 240) continue; // asiri uzun alintilari ele
                if (clean.toLowerCase().contains(needle)) {
                    exact.add(clean);
                } else {
                    fallback.add(clean);
                }
                if (exact.size() >= max) break;
            }

            List<String> out = new ArrayList<>(exact);
            for (String s : fallback) {
                if (out.size() >= max) break;
                out.add(s);
            }
            return out;
        } catch (Exception e) {
            log.info("Tatoeba ornek bulunamadi/hata ({}): {}", term, e.getMessage());
            return List.of();
        }
    }
}
