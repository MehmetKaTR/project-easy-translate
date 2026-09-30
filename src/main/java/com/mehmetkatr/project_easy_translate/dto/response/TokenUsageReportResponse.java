package com.mehmetkatr.project_easy_translate.dto.response;

import java.time.LocalDateTime;

/** Admin: bir kullanıcının belirli aralıktaki token kullanım raporu. */
public record TokenUsageReportResponse(
        Long userId,
        int days,
        int tokensUsed,
        long generationCount,
        LocalDateTime from,
        LocalDateTime to
) {
}
