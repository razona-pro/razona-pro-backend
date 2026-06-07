package com.razonapro.razonaprobackend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Cambio de contraseña autenticado: requiere código (enviado al correo) + contraseña actual + nueva. */
@Getter @Setter
public class ChangePasswordRequest {
    @NotBlank private String code;
    @NotBlank private String currentPassword;
    @NotBlank @Size(min = 8, max = 72) private String newPassword;
}
