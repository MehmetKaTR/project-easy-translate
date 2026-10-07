package com.mehmetkatr.project_easy_translate.dto.response;

import java.util.List;

/**
 * TaleMind Sozlugu zengin kart yaniti: DB'den cok-anlamli + CEFR + formlar,
 * her anlamin hedef dildeki cevirisi (lazy, cache'li).
 */
public record DictionaryCardResponse(
        String word,
        String cefr,
        boolean idiom,
        String targetLanguage,
        String primaryTranslation,   // kelime geneli kisa anlam (or. "gitmek")
        String definitionSource,     // or. "Wiktionary (CC BY-SA)"
        String translationSource,    // or. "Microsoft Translator"
        String exampleSource,        // or. "Wiktionary" veya "AI (Gemini)"
        List<Form> forms,
        List<Sense> senses
) {
    public record Form(String form, List<String> tags) {
    }

    public record Sense(
            String pos,
            String definition,              // EN
            String definitionTranslation,   // TR (lazy)
            List<String> examples,          // EN (arkaik olanlar elenmis)
            List<String> examplesTranslation, // TR (lazy, examples ile ayni sira)
            List<String> synonyms           // EN
    ) {
    }
}
