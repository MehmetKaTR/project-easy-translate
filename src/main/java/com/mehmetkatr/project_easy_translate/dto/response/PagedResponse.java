package com.mehmetkatr.project_easy_translate.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Sayfalanmış yanıtlar için ortak, framework'ten bağımsız API sözleşmesi.
 * Spring'in {@link Page} yapısını dışarı sızdırmadan sadece ihtiyaç duyulan
 * alanları döndürür. Tüm listeleme endpoint'leri bunu kullanır.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
