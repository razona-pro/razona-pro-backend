package com.razonapro.razonaprobackend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentRegisterRequest {

    @NotBlank(message = "El código de estudiante es obligatorio")
    @Pattern(regexp = "^[0-9]{7}$", message = "El código debe tener exactamente 7 dígitos")
    private String studentId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 15, message = "El nombre no puede superar 15 caracteres")
    private String firstName;

    @Size(max = 15, message = "El segundo nombre no puede superar 15 caracteres")
    private String secondName;

    @NotBlank(message = "El primer apellido es obligatorio")
    @Size(max = 15, message = "El apellido no puede superar 15 caracteres")
    private String firstSurname;

    @Size(max = 15, message = "El segundo apellido no puede superar 15 caracteres")
    private String secondSurname;

    @NotBlank(message = "El email es obligatorio")
    @Size(max = 50, message = "El email no puede superar 50 caracteres")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+\\-]+@[Uu][Ff][Pp][Ss][Oo]\\.[Ee][Dd][Uu]\\.[Cc][Oo]$",
            message = "El email debe pertenecer al dominio @ufpso.edu.co"
    )
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(
            regexp = "^\\+[1-9][0-9]{10,13}$",
            message = "El teléfono debe estar en formato internacional, ej: +573001234567"
    )
    private String phone;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "La contraseña debe tener al menos una mayúscula, una minúscula y un número"
    )
    private String password;
}