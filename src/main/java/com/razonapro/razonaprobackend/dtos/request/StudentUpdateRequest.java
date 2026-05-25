package com.razonapro.razonaprobackend.dtos.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StudentUpdateRequest {
    @Size(max = 15) private String firstName;
    @Size(max = 15) private String secondName;
    @Size(max = 15) private String firstSurname;
    @Size(max = 15) private String secondSurname;
    @Size(max = 15) private String phone;
    private Boolean isActive;
}