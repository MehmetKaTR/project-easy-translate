package com.mehmetkatr.project_easy_translate.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleTokenLoginRequest {

    @NotBlank
    private String idToken;

    private String preferredUsername;
}
