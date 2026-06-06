package com.razonapro.razonaprobackend.domain.admin.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AdminCreateRequest {

    @NotBlank @Size(max = 15) private String firstName;
    @Size(max = 15)           private String secondName;
    @NotBlank @Size(max = 15) private String firstSurname;
    @Size(max = 15)           private String secondSurname;

    // Solo se pide el correo: la contraseña se genera y se envía por email.
    // El teléfono ya no se solicita.
    @NotBlank @Email @Size(max = 50)
    private String email;
}