package com.razonapro.razonaprobackend.domain.tried.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SubmitAnswerRequest {
    @NotBlank private String questionId;
    @NotBlank private String optionId;
    /**
     * Competencia de la pregunta DENTRO de esta prueba. Es opcional por
     * retrocompatibilidad, pero el frontend la envía siempre: como el banco
     * puede reutilizar el mismo question_id en varias competencias, sin ella
     * la resolución (test_id + question_id) es ambigua.
     */
    private String competenceId;
}