package com.mehmetkatr.project_easy_translate.dto.response;

/**
 * Kelime listesi "liste görünümü" için hafif özet DTO'su (master-detail deseni).
 * Kelimeleri taşımaz; sadece meta + kelime sayısı. Kullanıcı listeye tıklayınca
 * kelimeler ayrı ve sayfalı olarak /api/words/byWordList üzerinden gelir.
 */
public record WordListSummaryResponse(
        Long id,
        String name,
        String color,
        long wordCount
) {
}
