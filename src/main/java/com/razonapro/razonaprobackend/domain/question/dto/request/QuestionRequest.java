package com.razonapro.razonaprobackend.domain.question.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class QuestionRequest {

    @NotBlank @Size(max = 2000)
    private String statement;

    @Size(max = 2000) private String explanation;
    @Size(max = 50)   private String source;

    @NotBlank
    @Pattern(regexp = "^[BMA]$", message = "Dificultad: B=Básico, M=Medio, A=Alto")
    private String difficultyLevel;

    @NotNull @Size(min = 2, message = "Debe haber minimo 2 opciones")
    @Valid
    private List<OptionRequest> options;
}
