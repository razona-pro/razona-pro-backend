package com.razonapro.razonaprobackend.domain.aitried.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SubmitAiAnswerRequest {
    @NotBlank @Size(max = 2000) private String questionText;
    @NotBlank @Size(max = 300)  private String studentAnswer;
    @NotBlank @Size(max = 300)  private String correctAnswer;
    @NotNull                    private Boolean isCorrect;
    @NotBlank @Size(max = 6)    private String competenceId;
}