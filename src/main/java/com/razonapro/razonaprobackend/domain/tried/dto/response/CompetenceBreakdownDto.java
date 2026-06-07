package com.razonapro.razonaprobackend.domain.tried.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Desglose de aciertos por competencia de un intento, SIN revelar respuestas correctas
 * ni explicaciones. Seguro para que el estudiante vea su propio rendimiento.
 */
@Getter @Builder
public class CompetenceBreakdownDto {
    private String competenceId;
    private int    correct;
    private int    total;
}
