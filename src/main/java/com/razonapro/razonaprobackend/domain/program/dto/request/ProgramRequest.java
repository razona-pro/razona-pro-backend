package com.razonapro.razonaprobackend.domain.program.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProgramRequest {
    @NotBlank
    @Pattern(regexp = "^[0-9]{3}$", message = "El ID del programa debe ser exactamente 3 dígitos numéricos")
    private String programId;

    @NotBlank @Size(max = 50)
    private String programName;

    @Size(max = 100)
    private String description;
}
