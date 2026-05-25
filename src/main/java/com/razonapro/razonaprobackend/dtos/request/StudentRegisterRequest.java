package com.razonapro.razonaprobackend.dtos.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StudentRegisterRequest {

    @NotBlank(message = "El código de estudiante es obligatorio")
    @Pattern(regexp = "^[0-9]{7}$", message = "El código debe tener exactamente 7 dígitos")
    private String studentId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 15)
    private String firstName;

    @Size(max = 15)
    private String secondName;

    @NotBlank(message = "El primer apellido es obligatorio")
    @Size(max = 15)
    private String firstSurname;

    @Size(max = 15)
    private String secondSurname;

    @NotBlank(message = "El email es obligatorio")
    @Email
    @Size(max = 50)
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 15)
    private String phone;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 64)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "La contraseña debe tener al menos una mayúscula, una minúscula y un número")
    private String password;
}