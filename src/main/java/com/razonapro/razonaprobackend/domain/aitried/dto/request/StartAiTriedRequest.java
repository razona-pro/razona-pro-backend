package com.razonapro.razonaprobackend.domain.aitried.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StartAiTriedRequest {
    @NotBlank @Size(max = 6)  private String competenceId;
    @NotNull @Min(1) @Max(20) private Integer totalQuestions;
    @Size(max = 200)          private String description;
}