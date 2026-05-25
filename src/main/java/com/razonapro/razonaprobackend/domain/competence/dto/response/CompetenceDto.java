package com.razonapro.razonaprobackend.domain.competence.dto.response;

import com.razonapro.razonaprobackend.domain.competence.model.Competence;
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