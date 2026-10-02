package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.TranslationCache;
import com.mehmetkatr.project_easy_translate.repository.TranslationCacheRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Katmanli ceviri onerisi (web + mobil ortak):
 *  Katman 1: dile ozel ruleset (saçma girdiyi ele).
 *  Katman 2: paylasimli cache (varsa dis cagri yok).
 *  Katman 3: tek kelime -> ucretsiz Google.
 *  Katman 4: cok kelime -> once Google (filtre); cevrilebiliyorsa Gemini ile idiomatik duzelt.
 */
@Service
@RequiredArgsConstructor
public class SmartSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(SmartSuggestionService.class);

    private static final String ENGINE_GEMINI = "GEMINI";

    private final LanguageRuleset languageRuleset;
    private final TranslationCacheRepository translationCacheRepository;
    private final GeminiClient geminiClient;

    /** Oneri sonucu: metin + hangi kaynaktan geldigi (GEMINI | ""). */
    public record Suggestion(String text, String engine) {}

    /**
     * Oneri pipeline'i (web + mobil ortak):
     *  1) Ruleset — saçma girdiyi ele (hic dis cagri yok).
     *  2) Cache — varsa aninda dondur (bedava).
     *  3) Gemini — ceviriyi uret (idiom-farkinda), cache'e yaz.
     * Not: ucretsiz Google (gtx) sunucu IP'sinden 429 (rate limit) verdigi icin
     * oneride artik kullanilmiyor; Gemini key bazlidir, bu sorun yok.
     */
    @Transactional
    public Suggestion suggest(String rawText, String rawSource, String rawTarget) {
        String text = rawText == null ? "" : rawText.trim();
        String source = normalizeLang(rawSource, "en");
        String target = normalizeLang(rawTarget, "tr");
        if (text.isEmpty()) return new Suggestion("", "");
        if (source.equals(target)) return new Suggestion(text, "");

        // Katman 1 — saçma girdiyi ele
        if (!languageRuleset.isMeaningful(text, source)) {
            return new Suggestion("", "");
        }

        // Katman 2 — cache
        String cacheKey = text.toLowerCase();
        var cached = translationCacheRepository.findBySourceTextAndSourceLangAndTargetLang(cacheKey, source, target);
        if (cached.isPresent()) {
            return new Suggestion(cached.get().getTranslation(), cached.get().getEngine());
        }

        // Katman 3 — Gemini
        String result = safeGemini(text, source, target);
        if (result.isBlank()) {
            return new Suggestion("", "");
        }
        saveToCache(cacheKey, source, target, result, ENGINE_GEMINI);
        return new Suggestion(result, ENGINE_GEMINI);
    }

    private void saveToCache(String key, String source, String target, String translation, String engine) {
        try {
            translationCacheRepository.save(TranslationCache.builder()
                    .sourceText(key)
                    .sourceLang(source)
                    .targetLang(target)
                    .translation(translation)
                    .engine(engine)
                    .build());
        } catch (Exception e) {
            // Yaris durumunda unique cakismasi olabilir; onemli degil.
            log.debug("Ceviri cache yazilamadi: {}", e.getMessage());
        }
    }

    private String safeGemini(String text, String source, String target) {
        try {
            String prompt = """
                    Translate the following %s word or phrase into %s.
                    If it is an idiom or fixed expression, give the natural idiomatic equivalent,
                    NOT a literal word-by-word translation.
                    Return ONLY the translation text: no quotes, no notes, no alternatives.
                    Text: %s
                    """.formatted(source, target, text);
            String raw = geminiClient.generate(prompt);
            return cleanGemini(raw);
        } catch (Exception e) {
            log.info("Gemini oneri hatasi: {}", e.getMessage());
            return "";
        }
    }

    private String cleanGemini(String raw) {
        if (raw == null) return "";
        String out = raw.trim();
        // ilk satiri al
        int nl = out.indexOf('\n');
        if (nl >= 0) out = out.substring(0, nl).trim();
        // olasi "Translation:" onekini ve tirnaklari temizle
        out = out.replaceAll("(?i)^translation\\s*:\\s*", "").trim();
        out = out.replaceAll("^[\"'“”‘’]+|[\"'“”‘’]+$", "").trim();
        if (out.length() > 160) out = out.substring(0, 160).trim();
        return out;
    }

    private String normalizeLang(String input, String fallback) {
        String value = (input == null ? "" : input.trim().toLowerCase());
        return value.isBlank() ? fallback : value;
    }
}
