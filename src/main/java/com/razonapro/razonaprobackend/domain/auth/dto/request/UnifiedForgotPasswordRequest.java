package com.razonapro.razonaprobackend.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UnifiedForgotPasswordRequest {

    @NotBlank @Email
    private String email;

    /** Código de admin (AMN001) o estudiante (0192250) */
    @NotBlank
    private String code;

    @NotBlank
    @Pattern(regexp = "^\\+[1-9][0-9]{10,13}$",
            message = "Formato internacional: +573001234567")
    private String phone;
}