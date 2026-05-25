package com.razonapro.razonaprobackend.domain.question.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OptionRequest {
    @NotBlank @Size(max = 300) private String optionText;
    @NotNull                   private Boolean isCorrect;
}