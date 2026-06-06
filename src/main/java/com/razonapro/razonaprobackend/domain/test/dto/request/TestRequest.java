package com.razonapro.razonaprobackend.domain.test.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TestRequest {

    // Multicompetencia: la prueba ya NO se crea atada a una competencia.
    // Sus competencias surgen de las preguntas que se le asignan.

    @NotBlank @Size(max = 50)
    private String testName;

    @Size(max = 100)
    private String description;

    /** NULL = sin tiempo (solo válido en PRACTICE). Para EXAM/TIMED es obligatorio (validado en el service). */
    @Min(value = 60, message = "Duración mínima: 60 segundos")
    private Integer durationSeconds;

    @Min(1)
    private Integer questionsToPresent;

    @NotBlank
    @Pattern(regexp = "^(PRACTICE|EXAM|TIMED)$", message = "Modo: PRACTICE, EXAM o TIMED")
    private String testMode;

    /** Si el admin quiere notificar a todos los estudiantes al publicar el test. */
    private Boolean notifyStudents = Boolean.TRUE;
}