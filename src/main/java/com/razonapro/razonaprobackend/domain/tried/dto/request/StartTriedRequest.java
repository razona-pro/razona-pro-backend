package com.razonapro.razonaprobackend.domain.tried.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StartTriedRequest {
    @NotBlank private String testId;
    @NotBlank private String competenceId;
}
