package com.mehmetkatr.project_easy_translate.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialLoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 2, max = 50)
    private String username;
}
