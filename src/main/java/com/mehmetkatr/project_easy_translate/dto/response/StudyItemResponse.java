package com.mehmetkatr.project_easy_translate.dto.response;

/** Quiz'de gosterilecek tek kelime (hafif — tam kart degil). */
public record StudyItemResponse(
        Long entryId,
        String word,
        String cefr,
        String pos,
        String meaningTr,     // birincil anlamin Turkcesi (varsa)
        String definitionEn,  // birincil anlam (EN)
        boolean isNew         // ilk kez mi geliyor (yeni) yoksa tekrar mi
) {}
