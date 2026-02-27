package com.mehmetkatr.project_easy_translate.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleTokenLoginRequest {

    @NotBlank
    private String idToken;
}
