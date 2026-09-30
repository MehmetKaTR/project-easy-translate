package com.mehmetkatr.project_easy_translate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class GenericMessageResponse {
    private String code;
    private String message;
}
