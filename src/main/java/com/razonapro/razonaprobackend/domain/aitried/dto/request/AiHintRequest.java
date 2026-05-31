// domain/aitried/dto/request/AiHintRequest.java  (reemplaza)
package com.razonapro.razonaprobackend.domain.aitried.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AiHintRequest {
    @NotBlank        private String aiQuestionId;
    @Min(1) @Max(3)  private int    hintLevel;
}