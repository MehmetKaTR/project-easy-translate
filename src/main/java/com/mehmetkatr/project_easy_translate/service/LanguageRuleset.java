package com.mehmetkatr.project_easy_translate.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Dile ozel "gercek kelime/obek mi?" kurallari (Katman 1).
 * Amac: "mck", "sdf" gibi saçma girdileri ceviriye (ozellikle Gemini'ye) hic gondermemek.
 *
 * Kural:
 *  - Token 1 harf ise: yalnizca o dilin gecerli tek-harfli kelime listesinde varsa gecerli.
 *  - Token 2+ harf ise: o dilin sesli harf kumesinden en az biri bulunmali.
 *  - Obek: tum tokenlar gecerli olmali.
 */
@Component
public class LanguageRuleset {

    private record Rule(Set<Character> vowels, Set<String> singleLetterWords) {}

    private static final Rule GENERIC = new Rule(
            setOf("aeiouyàâäáéèêëíìîïóòôöúùûü"),
            Set.of("a", "i"));

    private static final Map<String, Rule> RULES = Map.of(
            "en", new Rule(setOf("aeiouy"), Set.of("a", "i")),
            "tr", new Rule(setOf("aeıioöuü"), Set.of("o")),
            "de", new Rule(setOf("aeiouäöüy"), Set.of()),
            "es", new Rule(setOf("aeiouáéíóúüy"), Set.of("a", "e", "o", "u", "y"))
    );

    private static Set<Character> setOf(String letters) {
        return letters.chars().mapToObj(c -> (char) c).collect(java.util.stream.Collectors.toSet());
    }

    public boolean isMeaningful(String text, String languageCode) {
        if (text == null) return false;
        String trimmed = text.trim();
        if (trimmed.isEmpty() || trimmed.length() > 100) return false;

        Rule rule = RULES.getOrDefault(
                languageCode == null ? "en" : languageCode.trim().toLowerCase(), GENERIC);

        String[] tokens = trimmed.toLowerCase().split("\\s+");
        for (String rawToken : tokens) {
            // Kelime kenarindaki tire/kesme vb. temizle (icerik kalsin)
            String token = rawToken.replaceAll("^[-'’]+|[-'’]+$", "");
            if (token.isEmpty()) continue;
            if (!isValidToken(token, rule)) return false;
        }
        return true;
    }

    private boolean isValidToken(String token, Rule rule) {
        // Sadece harf (ve ic tire/kesme) olmali
        if (!token.matches("[\\p{L}'’-]+")) return false;

        String letters = token.replaceAll("[^\\p{L}]", "");
        if (letters.isEmpty()) return false;

        if (letters.length() == 1) {
            return rule.singleLetterWords().contains(letters);
        }
        for (int i = 0; i < letters.length(); i++) {
            if (rule.vowels().contains(letters.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
