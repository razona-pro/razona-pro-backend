package com.razonapro.razonaprobackend.dtos.response;

import com.razonapro.razonaprobackend.models.Program;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class ProgramDto {
    private String programId;
    private String programName;
    private String description;
    private Boolean isActive;

    public static ProgramDto from(Program p) {
        return ProgramDto.builder()
            .programId(p.getProgramId())
            .programName(p.getProgramName())
            .description(p.getDescription())
            .isActive(p.getIsActive())
            .build();
    }
}
