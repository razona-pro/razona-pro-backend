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

    @NotBlank @Email @Size(max = 50)
    private String email;

    @NotBlank @Size(max = 15)
    @Pattern(regexp = "^\\+[1-9][0-9]{10,13}$",
            message = "Formato internacional: +573001234567")
    private String phone;

    @NotBlank
    @Size(min = 8, max = 64)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "La contraseña debe tener mayúscula, minúscula y número")
    private String password;
}