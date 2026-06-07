package com.razonapro.razonaprobackend.domain.appeal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Consulta del estado de la cuenta (re-valida credenciales) para mostrar el flujo de apelación. */
@Getter @Setter
public class AccountStatusRequest {
    @NotBlank private String code;
    @NotBlank private String email;
    @NotBlank private String password;
}
