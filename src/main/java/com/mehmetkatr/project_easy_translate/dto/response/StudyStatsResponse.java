package com.mehmetkatr.project_easy_translate.dto.response;

/** Calisma ozeti: bugun kac due, kac kelime calisildi, havuz buyuklugu. */
public record StudyStatsResponse(
        long dueToday,
        long studied,
        long poolTotal
) {}
