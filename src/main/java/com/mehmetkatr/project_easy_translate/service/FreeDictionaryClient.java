package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Free Dictionary API (freedictionaryapi.com) istemcisi — ucretsiz, key yok.
 * Eski dictionaryapi.dev kapandi; yeni surum farkli bir sema donuyor:
 *
 * {
 *   "word": "...",
 *   "entries": [
 *     { "partOfSpeech": "...",
 *       "senses": [ { "definition": "...", "examples": [..],
 *                     "subsenses": [ { "definition": "...", "examples": [..] } ] } ],
 *       "synonyms": [..], "antonyms": [..] } ]
 * }
 *
 * Gercek tanim cogu zaman "subsenses" icinde olur (ust sense bir kategori basligidir),
 * bu yuzden sense -> subsense seklinde duzlestirip ilk anlamli tanimi aliriz.
 * Kelime bulunamazsa (404) ya da hata olursa Optional.empty() doner -> Gemini fallback.
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
        String lang = (languageCode == null || languageCode.isBlank()) ? "en" : languageCode.trim().toLowerCase();
        String term = word == null ? "" : word.trim();
        if (term.isEmpty()) return Optional.empty();

        // Wiktionary buyuk/kucuk harfe duyarli ve idiomlari genelde bassiz saklar:
        //  - "Break the ice" 404 -> "break the ice"
        //  - "a piece of cake" 404 -> "piece of cake" (bastaki a/an/the atilir)
        // Adaylari sirayla dene, ilk dolu sonucu dondur.
        for (String candidate : candidateTerms(term)) {
            Optional<Result> result = fetchAndParse(candidate, lang);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    /** Aranacak terim varyantlari: orijinal, kucuk harf, bastaki artikel atilmis hali. */
    private List<String> candidateTerms(String term) {
        List<String> out = new ArrayList<>();
        out.add(term);
        String lower = term.toLowerCase();
        if (!out.contains(lower)) out.add(lower);
        String stripped = stripLeadingArticle(lower);
        if (!stripped.equals(lower) && !out.contains(stripped)) out.add(stripped);
        return out;
    }

    private String stripLeadingArticle(String lower) {
        for (String article : new String[]{"a ", "an ", "the "}) {
            if (lower.startsWith(article) && lower.length() > article.length()) {
                return lower.substring(article.length()).trim();
            }
        }
        return lower;
    }

    private Optional<Result> fetchAndParse(String term, String lang) {
        try {
            String body = restClient.get()
                    .uri("https://freedictionaryapi.com/api/v1/entries/{lang}/{word}", lang, term)
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            JsonNode entries = root.path("entries");
            if (!entries.isArray() || entries.isEmpty()) return Optional.empty();

            JsonNode firstEntry = entries.get(0);
            String partOfSpeech = textOrNull(firstEntry.path("partOfSpeech"));

            // Duzlestir: her sense icin subsenses varsa onlari, yoksa sense'in kendisini al.
            List<String> definitions = new ArrayList<>();
            List<String> examples = new ArrayList<>();
            Set<String> synonyms = new LinkedHashSet<>();

            for (JsonNode sense : firstEntry.path("senses")) {
                JsonNode subsenses = sense.path("subsenses");
                if (subsenses.isArray() && !subsenses.isEmpty()) {
                    for (JsonNode sub : subsenses) {
                        collect(sub, definitions, examples, synonyms);
                    }
                } else {
                    collect(sense, definitions, examples, synonyms);
                }
            }

            // Entry seviyesindeki sinonimler (kelimenin kendisini ele)
            for (JsonNode s : firstEntry.path("synonyms")) {
                addSynonym(s.asText(null), term, synonyms);
            }

            String definition = definitions.isEmpty() ? null : definitions.get(0);
            List<String> topExamples = examples.size() > MAX_EXAMPLES ? examples.subList(0, MAX_EXAMPLES) : examples;
            List<String> topSynonyms = new ArrayList<>(synonyms);
            if (topSynonyms.size() > MAX_SYNONYMS) {
                topSynonyms = topSynonyms.subList(0, MAX_SYNONYMS);
            }

            if (definition == null && topExamples.isEmpty()) return Optional.empty();
            return Optional.of(new Result(partOfSpeech, definition, new ArrayList<>(topExamples), topSynonyms));
        } catch (Exception e) {
            log.info("Free Dictionary bulunamadi/hata ({}): {}", term, e.getMessage());
            return Optional.empty();
        }
    }

    private void collect(JsonNode node, List<String> definitions, List<String> examples, Set<String> synonyms) {
        String def = textOrNull(node.path("definition"));
        if (def != null) definitions.add(def);
        for (JsonNode ex : node.path("examples")) {
            String value = textOrNull(ex);
            if (value != null) examples.add(value);
        }
        for (JsonNode s : node.path("synonyms")) {
            addSynonym(s.asText(null), null, synonyms);
        }
    }

    private void addSynonym(String value, String excludeWord, Set<String> target) {
        if (value == null) return;
        String v = value.trim();
        if (v.isEmpty()) return;
        if (excludeWord != null && v.equalsIgnoreCase(excludeWord)) return;
        target.add(v);
    }

    private String textOrNull(JsonNode node) {
        String value = node.asText(null);
        return (value == null || value.isBlank()) ? null : value;
    }

    public record Result(String partOfSpeech, String definition, List<String> examples, List<String> synonyms) {
    }
}
