package com.mehmetkatr.project_easy_translate.dto.response;

/** Admin panelinde global kelime listesi: kullanim sayisi + blok durumu. */
public record AdminWordSummaryResponse(
        Long id,
        String text,
        String languageCode,
        boolean blocked,
        long usageCount
) {
}
