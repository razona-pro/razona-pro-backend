package com.razonapro.razonaprobackend.domain.appeal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Envío de apelación por un estudiante desactivado (sin sesión: se re-validan credenciales). */
@Getter @Setter
public class AppealSubmitRequest {
    @NotBlank private String code;      // código de estudiante (7 dígitos)
    @NotBlank private String email;
    @NotBlank private String password;
    @NotBlank @Size(min = 10, max = 1000)
    private String message;
}
