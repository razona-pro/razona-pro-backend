package com.razonapro.razonaprobackend.domain.question.dto.request;

import com.razonapro.razonaprobackend.dtos.request.OptionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class QuestionRequest {

    @NotBlank @Size(max = 300)
    private String statement;

    @Size(max = 200) private String explanation;
    @Size(max = 50)  private String source;

    @NotBlank
    @Pattern(regexp = "^[BMA]$", message = "Dificultad: B=Básico, M=Medio, A=Alto")
    private String difficultyLevel;

    @NotNull @Size(min = 2, max = 5, message = "Debe haber entre 2 y 5 opciones")
    @Valid
    private List<OptionRequest> options;
}
