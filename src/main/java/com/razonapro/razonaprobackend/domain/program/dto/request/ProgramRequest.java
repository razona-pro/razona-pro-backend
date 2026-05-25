package com.razonapro.razonaprobackend.domain.program.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProgramRequest {
    @NotBlank @Size(min = 2, max = 3, message = "El ID del programa debe tener 2-3 caracteres")
    private String programId;

    @NotBlank @Size(max = 20)
    private String programName;

    @Size(max = 100)
    private String description;
}
