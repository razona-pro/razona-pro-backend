package com.razonapro.razonaprobackend.domain.aitried.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StartAiTriedRequest {
    /** Una o más competencias a practicar (la sesión rota entre ellas). */
    @NotNull @Size(min = 1, message = "Selecciona al menos una competencia")
    private java.util.List<@NotBlank @Size(max = 6) String> competenceIds;

    @NotNull @Min(1) @Max(20) private Integer totalQuestions;
    @Size(max = 200)          private String description;
}