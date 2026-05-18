package com.razonapro.razonaprobackend.dtos.response;

import com.razonapro.razonaprobackend.models.Competence;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class CompetenceDto {
    private String competenceId;
    private String competenceName;
    private String description;
    private Boolean isActive;

    public static CompetenceDto from(Competence c) {
        return CompetenceDto.builder()
            .competenceId(c.getCompetenceId())
            .competenceName(c.getCompetenceName())
            .description(c.getDescription())
            .isActive(c.getIsActive())
            .build();
    }
}
