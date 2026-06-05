package com.razonapro.razonaprobackend.domain.question.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class QuestionUpdateRequest {
    @NotBlank @Size(max = 2000) private String statement;
    @Size(max = 2000)           private String explanation;
    @Size(max = 50)             private String source;
    /** Opcional: NULL/"" = "no aplica". */
    @Pattern(regexp = "^[BMA]?$") private String difficultyLevel;

    /**
     * Si se envía (non-null, ≥2 elementos), reemplaza completamente las opciones
     * de la pregunta. Si es null, las opciones actuales se conservan sin cambios.
     */
    @Valid
    @Size(min = 2, max = 6, message = "Se requieren entre 2 y 6 opciones.")
    private List<OptionRequest> options;
}
