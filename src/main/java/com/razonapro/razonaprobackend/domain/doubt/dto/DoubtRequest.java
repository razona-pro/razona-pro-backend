// domain/doubt/dto/DoubtRequest.java
package com.razonapro.razonaprobackend.domain.doubt.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DoubtRequest {
    @NotBlank @Pattern(regexp = "STATIC|AI") private String source;
    @Size(max = 6)   private String competenceId;
    @Size(max = 7)   private String questionId;
    @Size(max = 12)  private String aiQuestionId;
    @Size(max = 500) private String message;
}