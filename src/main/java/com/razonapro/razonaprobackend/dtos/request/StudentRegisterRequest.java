package com.razonapro.razonaprobackend.dtos.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StudentRegisterRequest {

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
    @Email(message = "Formato de email inválido")
    @Size(max = 50)
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 15, message = "El teléfono no puede superar 15 caracteres")
    private String phone;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
             message = "La contraseña debe tener al menos una mayúscula, una minúscula y un número")
    private String password;

    @NotBlank(message = "El programa es obligatorio")
    @Size(max = 3)
    private String programId;
}
