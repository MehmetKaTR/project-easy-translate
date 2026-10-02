package com.mehmetkatr.project_easy_translate.dto.response;

/** Oturum acik kullanicinin guncel profili (plan senkronu icin). */
public record UserProfileResponse(
        Long userId,
        String username,
        String plan
) {
}
