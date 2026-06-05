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

    /** Opcional: NULL/"" = "no aplica". Solo es un filtro extra, no oficial Saber Pro. */
    @Pattern(regexp = "^[BMA]?$", message = "Dificultad: B=Básico, M=Medio, A=Alto, o vacío (no aplica)")
    private String difficultyLevel;

    @NotNull @Size(min = 2, message = "Debe haber mínimo 2 opciones")
    @Valid
    private List<OptionRequest> options;
}