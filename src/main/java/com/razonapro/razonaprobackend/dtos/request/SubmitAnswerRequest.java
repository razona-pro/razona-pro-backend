package com.razonapro.razonaprobackend.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SubmitAnswerRequest {
    @NotBlank private String questionId;
    @NotBlank private String optionId;
}
