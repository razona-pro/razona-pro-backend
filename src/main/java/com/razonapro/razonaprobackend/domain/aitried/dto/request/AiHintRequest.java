// src/main/java/com/razonapro/razonaprobackend/domain/aitried/dto/request/AiHintRequest.java
package com.razonapro.razonaprobackend.domain.aitried.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AiHintRequest {
    @NotBlank          private String questionId;
    @Min(1) @Max(3)    private int    hintLevel;
}