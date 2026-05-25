package com.razonapro.razonaprobackend.domain.competence.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CompetenceRequest {
    @NotBlank @Size(max = 30) private String competenceName;
    @Size(max = 100)          private String description;
}