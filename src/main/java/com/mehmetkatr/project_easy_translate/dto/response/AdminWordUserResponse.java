package com.mehmetkatr.project_easy_translate.dto.response;

/** Admin: bir global kelimeye sahip kullanici + o kullanicinin cevirisi. */
public record AdminWordUserResponse(
        Long userId,
        String username,
        String email,
        String translated
) {
}
