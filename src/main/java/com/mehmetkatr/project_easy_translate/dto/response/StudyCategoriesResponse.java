package com.mehmetkatr.project_easy_translate.dto.response;

import java.util.List;

/** Calisma kategorileri: seviyeler, konular, ozel (Anki vb.). */
public record StudyCategoriesResponse(
        List<Cat> levels,
        List<Cat> topics,
        List<Cat> special
) {
    /** key: "level:A1" | "topic:Food & Drink" | "source:ANKI" ; next() bu key ile cagrilir. */
    public record Cat(String key, String label, long count) {}
}
