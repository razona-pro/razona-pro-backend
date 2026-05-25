package com.razonapro.razonaprobackend.domain.question.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OptionRequest {
    @NotBlank @Size(max = 200) private String optionText;
    @NotNull                   private Boolean isCorrect;
}
