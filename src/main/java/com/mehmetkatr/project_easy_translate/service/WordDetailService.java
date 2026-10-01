package com.mehmetkatr.project_easy_translate.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehmetkatr.project_easy_translate.dto.response.WordDetailResponse;
import com.mehmetkatr.project_easy_translate.entity.Word;
import com.mehmetkatr.project_easy_translate.entity.WordDetail;
import com.mehmetkatr.project_easy_translate.repository.WordDetailRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class WordDetailService {

    private static final Logger log = LoggerFactory.getLogger(WordDetailService.class);

    // Kaynak kodlari (her bolum icin ayri gosterilir)
    private static final String SRC_DICTIONARY = "FREE_DICTIONARY";
    private static final String SRC_TATOEBA = "TATOEBA";
    private static final String SRC_GEMINI = "GEMINI";
    private static final String SRC_GOOGLE = "GOOGLE_TRANSLATE";

    // Gercek bir kelime/obek: en az bir harf; sadece harf, bosluk, kesme ve tire.
    private static final Pattern WORD_LIKE = Pattern.compile("^\\p{L}[\\p{L} '’\\-]*$");

    private final WordDetailRepository wordDetailRepository;
    private final FreeDictionaryClient freeDictionaryClient;
    private final SentenceExampleClient sentenceExampleClient;
    private final TranslationSuggestionService translationSuggestionService;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    /**
     * Cache-first: detay varsa DB'den dondur (bedava), yoksa uret + kaydet.
     */
    public WordDetailResponse getOrCreate(Word word, String targetLanguage) {
        // Savunma: "%&/" gibi kelime olmayan girdiler icin disari hic cikma (Gemini bosa calismasin).
        if (!isWordLike(word.getText())) {
            return new WordDetailResponse(
                    word.getText(), word.getLanguageCode(), targetLanguage,
                    null, null, null, List.of(), List.of(), null, null, null, null);
        }
        return wordDetailRepository.findByWordIdAndTargetLanguage(word.getId(), targetLanguage)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(generateAndSave(word, targetLanguage)));
    }

    private boolean isWordLike(String text) {
        if (text == null) return false;
        String t = text.trim();
        return !t.isEmpty() && t.length() <= 100 && WORD_LIKE.matcher(t).matches();
    }

    private WordDetail generateAndSave(Word word, String targetLanguage) {
        WordDetail detail = freeDictionaryClient.lookup(word.getText(), word.getLanguageCode())
                .map(result -> buildFromDictionary(word, targetLanguage, result))
                .orElseGet(() -> buildFromGemini(word, targetLanguage));
        return wordDetailRepository.save(detail);
    }

    private WordDetail buildFromDictionary(Word word, String targetLanguage, FreeDictionaryClient.Result result) {
        List<String> sentences = result.examples();
        String examplesSource = (sentences != null && !sentences.isEmpty()) ? SRC_DICTIONARY : null;
        // Sozluk tanim verip ornek cumle vermediyse (or. "give rise to") Tatoeba'dan cek.
        if (sentences == null || sentences.isEmpty()) {
            sentences = sentenceExampleClient.lookup(word.getText(), word.getLanguageCode(), 2);
            if (!sentences.isEmpty()) examplesSource = SRC_TATOEBA;
        }

        List<WordDetailResponse.Example> examples = sentences.stream()
                .map(sentence -> new WordDetailResponse.Example(
                        sentence,
                        safeTranslate(sentence, word.getLanguageCode(), targetLanguage)))
                .toList();

        String definitionTranslation = safeTranslate(result.definition(), word.getLanguageCode(), targetLanguage);
        boolean anyTranslation = definitionTranslation != null
                || examples.stream().anyMatch(e -> e.translation() != null);

        return WordDetail.builder()
                .word(word)
                .targetLanguage(targetLanguage)
                .partOfSpeech(result.partOfSpeech())
                .definition(result.definition())
                .definitionTranslation(definitionTranslation)
                .synonyms(writeJson(result.synonyms()))
                .examples(writeJson(examples))
                .source(SRC_DICTIONARY)
                .definitionSource(SRC_DICTIONARY)
                .examplesSource(examplesSource)
                .translationSource(anyTranslation ? SRC_GOOGLE : null)
                .build();
    }

    private WordDetail buildFromGemini(Word word, String targetLanguage) {
        String prompt = """
                You are a dictionary. For the word "%s" in language code "%s", return ONLY a JSON object
                (no markdown, no code fences) with exactly this shape:
                {"partOfSpeech":"noun|verb|adjective|idiom|...",
                 "definition":"a short clear definition in language %s",
                 "definitionTranslation":"the definition translated into language code %s",
                 "synonyms":["..."],
                 "examples":[{"sentence":"example sentence in language %s","translation":"its translation in language code %s"}]}
                Provide 1-2 examples. Keep it concise.
                """.formatted(word.getText(), word.getLanguageCode(), word.getLanguageCode(),
                targetLanguage, word.getLanguageCode(), targetLanguage);

        String raw = geminiClient.generate(prompt);
        JsonNode json = parseJsonLenient(raw);

        String partOfSpeech = textOrNull(json.path("partOfSpeech"));
        String definition = textOrNull(json.path("definition"));
        String definitionTranslation = textOrNull(json.path("definitionTranslation"));
        if (definitionTranslation == null) {
            definitionTranslation = safeTranslate(definition, word.getLanguageCode(), targetLanguage);
        }

        List<String> synonyms = new ArrayList<>();
        for (JsonNode s : json.path("synonyms")) {
            synonyms.add(s.asText());
        }

        List<WordDetailResponse.Example> examples = new ArrayList<>();
        for (JsonNode ex : json.path("examples")) {
            examples.add(new WordDetailResponse.Example(
                    textOrNull(ex.path("sentence")),
                    textOrNull(ex.path("translation"))));
        }

        return WordDetail.builder()
                .word(word)
                .targetLanguage(targetLanguage)
                .partOfSpeech(partOfSpeech)
                .definition(definition)
                .definitionTranslation(definitionTranslation)
                .synonyms(writeJson(synonyms))
                .examples(writeJson(examples))
                .source(SRC_GEMINI)
                .definitionSource(SRC_GEMINI)
                .examplesSource(examples.isEmpty() ? null : SRC_GEMINI)
                .translationSource(SRC_GEMINI)
                .build();
    }

    private JsonNode parseJsonLenient(String raw) {
        try {
            String cleaned = raw == null ? "{}" : raw.trim();
            // Gemini bazen ```json ... ``` ile sarar; ilk { ... son } arasini al
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.warn("Gemini JSON parse hatasi: {}", e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    private String textOrNull(JsonNode node) {
        String value = node.asText(null);
        return (value == null || value.isBlank()) ? null : value;
    }

    private String safeTranslate(String text, String sourceLanguage, String targetLanguage) {
        try {
            return translationSuggestionService.suggest(text, sourceLanguage, targetLanguage);
        } catch (Exception e) {
            log.warn("Ornek cumle cevirisi basarisiz: {}", e.getMessage());
            return null;
        }
    }

    private WordDetailResponse toResponse(WordDetail detail) {
        return new WordDetailResponse(
                detail.getWord().getText(),
                detail.getWord().getLanguageCode(),
                detail.getTargetLanguage(),
                detail.getPartOfSpeech(),
                detail.getDefinition(),
                detail.getDefinitionTranslation(),
                parseSynonyms(detail.getSynonyms()),
                parseExamples(detail.getExamples()),
                detail.getSource(),
                detail.getDefinitionSource(),
                detail.getExamplesSource(),
                detail.getTranslationSource()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("JSON yazma hatasi", e);
            return "[]";
        }
    }

    private List<String> parseSynonyms(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("synonyms JSON parse hatasi", e);
            return List.of();
        }
    }

    private List<WordDetailResponse.Example> parseExamples(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<WordDetailResponse.Example>>() {});
        } catch (Exception e) {
            log.warn("examples JSON parse hatasi", e);
            return List.of();
        }
    }
}
