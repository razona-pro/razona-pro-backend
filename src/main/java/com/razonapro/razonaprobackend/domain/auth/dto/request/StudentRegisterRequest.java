package com.razonapro.razonaprobackend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StudentRegisterRequest {

    @NotBlank
    @Pattern(regexp = "^[0-9]{7}$", message = "El código debe tener exactamente 7 dígitos")
    private String studentId;

    @NotBlank @Size(max = 15) private String firstName;
    @Size(max = 15)           private String secondName;
    @NotBlank @Size(max = 15) private String firstSurname;
    @Size(max = 15)           private String secondSurname;

    @NotBlank @Size(max = 50)
    @Pattern(regexp = "^[A-Za-z0-9._%+\\-]+@[Uu][Ff][Pp][Ss][Oo]\\.[Ee][Dd][Uu]\\.[Cc][Oo]$",
            message = "El email debe pertenecer al dominio @ufpso.edu.co")
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\+[1-9][0-9]{10,13}$",
            message = "Formato internacional: +573001234567")
    private String phone;

    @NotBlank
    @Size(min = 8, max = 64)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "La contraseña debe tener mayúscula, minúscula y número")
    private String password;
}