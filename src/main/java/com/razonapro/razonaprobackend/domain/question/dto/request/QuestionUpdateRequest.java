package com.razonapro.razonaprobackend.domain.question.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class QuestionUpdateRequest {
    @NotBlank @Size(max = 2000) private String statement;
    @Size(max = 2000)           private String explanation;
    @Size(max = 50)             private String source;
    @NotBlank @Pattern(regexp = "B|M|A") private String difficultyLevel;
}
