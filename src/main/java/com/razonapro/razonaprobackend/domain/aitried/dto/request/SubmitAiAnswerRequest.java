// src/main/java/com/razonapro/razonaprobackend/domain/aitried/dto/request/SubmitAiAnswerRequest.java
// (Reemplaza el existente)
package com.razonapro.razonaprobackend.domain.aitried.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SubmitAiAnswerRequest {
    @NotBlank private String questionId;     // UUID de la pregunta generada
    @NotBlank private String selectedOptionId; // "OPT0", "OPT1", "OPT2", "OPT3"
}