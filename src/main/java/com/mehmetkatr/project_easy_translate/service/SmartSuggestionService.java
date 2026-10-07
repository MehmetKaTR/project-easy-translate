package com.mehmetkatr.project_easy_translate.service;

import com.mehmetkatr.project_easy_translate.entity.TranslationCache;
import com.mehmetkatr.project_easy_translate.repository.TranslationCacheRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ceviri onerisi (web + mobil ortak), tek NMT motoru:
 *  1) Ruleset — anlamsiz girdiyi ele (hic dis cagri yok).
 *  2) Cache — varsa aninda dondur (bedava).
 *  3) Microsoft Translator (key varsa) / yoksa MyMemory ile cevir (hizli, NMT).
 *  4) Sonuc cache'lenir.
 *
 * Not: Cumle URETME (sozluk ornekleri) ayri is, Gemini ile DictionaryService enrich'te yapilir.
 */
@Service
@RequiredArgsConstructor
public class SmartSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(SmartSuggestionService.class);

    private final LanguageRuleset languageRuleset;
    private final TranslationCacheRepository translationCacheRepository;
    private final MicrosoftTranslatorClient microsoftTranslator;
    private final MyMemoryTranslatorClient myMemoryTranslator;

    /** Oneri sonucu: metin + hangi kaynaktan geldigi (MICROSOFT | MYMEMORY | ""). */
    public record Suggestion(String text, String engine) {}

    @Transactional
    public Suggestion suggest(String rawText, String rawSource, String rawTarget) {
        String text = rawText == null ? "" : rawText.trim();
        String source = normalizeLang(rawSource, "en");
        String target = normalizeLang(rawTarget, "tr");
        if (text.isEmpty()) return new Suggestion("", "");
        if (source.equals(target)) return new Suggestion(text, "");

        // Katman 1 — anlamsiz girdiyi ele
        if (!languageRuleset.isMeaningful(text, source)) {
            return new Suggestion("", "");
        }

        // Katman 2 — cache
        String cacheKey = text.toLowerCase();
        var cached = translationCacheRepository
                .findBySourceTextAndSourceLangAndTargetLang(cacheKey, source, target);
        if (cached.isPresent()) {
            return new Suggestion(cached.get().getTranslation(), cached.get().getEngine());
        }

        // Katman 3 — NMT (Microsoft / MyMemory)
        String engine = microsoftTranslator.isConfigured() ? "MICROSOFT" : "MYMEMORY";
        String result = translate(text, source, target);
        if (result.isBlank()) {
            return new Suggestion("", "");
        }
        saveToCache(cacheKey, source, target, result, engine);
        return new Suggestion(result, engine);
    }

    private String translate(String text, String source, String target) {
        try {
            if (microsoftTranslator.isConfigured()) {
                List<String> r = microsoftTranslator.translate(List.of(text), source, target);
                if (!r.isEmpty() && r.get(0) != null && !r.get(0).isBlank()) return r.get(0).trim();
            }
            String mm = myMemoryTranslator.translateOne(text, source, target);
            return mm == null ? "" : mm.trim();
        } catch (Exception e) {
            log.info("oneri ceviri hatasi: {}", e.getMessage());
            return "";
        }
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
            log.debug("Ceviri cache yazilamadi: {}", e.getMessage());
        }
    }

    private String normalizeLang(String input, String fallback) {
        String value = (input == null ? "" : input.trim().toLowerCase());
        return value.isBlank() ? fallback : value;
    }
}
