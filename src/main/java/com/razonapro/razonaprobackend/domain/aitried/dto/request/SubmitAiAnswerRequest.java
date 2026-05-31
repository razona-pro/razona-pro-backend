// domain/aitried/dto/request/SubmitAiAnswerRequest.java  (reemplaza)
package com.razonapro.razonaprobackend.domain.aitried.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SubmitAiAnswerRequest {
    @NotBlank          private String  aiQuestionId;
    @NotNull @Min(0)   private Integer selectedIndex;
}