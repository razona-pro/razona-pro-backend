package com.razonapro.razonaprobackend.domain.admin.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AdminUpdateRequest {
    @Size(max = 15) private String firstName;
    @Size(max = 15) private String secondName;
    @Size(max = 15) private String firstSurname;
    @Size(max = 15) private String secondSurname;

    @Email @Size(max = 50) private String email;

    @Size(max = 15)
    @Pattern(regexp = "^$|^\\+[1-9][0-9]{10,13}$",
            message = "Formato internacional: +573001234567")
    private String phone;

    private Boolean isActive;
}