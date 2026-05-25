package com.razonapro.razonaprobackend.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AdminForgotPasswordRequest {

    @NotBlank @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}[0-9]{3}$", message = "Código de administrador inválido")
    private String adminId;
}