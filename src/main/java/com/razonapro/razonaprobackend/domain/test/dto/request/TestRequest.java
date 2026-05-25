package com.razonapro.razonaprobackend.domain.test.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TestRequest {

    @NotBlank @Size(max = 6)
    private String competenceId;

    @NotBlank @Size(max = 50)
    private String testName;

    @Size(max = 100)
    private String description;

    @NotNull @Min(value = 60, message = "Duración mínima: 60 segundos")
    private Integer durationSeconds;

    @Min(1)
    private Integer questionsToPresent;

    @NotBlank
    @Pattern(regexp = "^(PRACTICE|EXAM|TIMED)$", message = "Modo: PRACTICE, EXAM o TIMED")
    private String testMode;
}