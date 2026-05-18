package com.razonapro.razonaprobackend.dtos.response;

import com.razonapro.razonaprobackend.models.Test;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class TestDto {
    private String testId;
    private String competenceId;
    private String competenceName;
    private String testName;
    private String description;
    private Integer durationSeconds;
    private Integer questionsToPresent;
    private String testMode;
    private Boolean isActive;

    public static TestDto from(Test t) {
        return TestDto.builder()
            .testId(t.getTestId())
            .competenceId(t.getCompetenceId())
            .competenceName(t.getCompetence() != null ? t.getCompetence().getCompetenceName() : null)
            .testName(t.getTestName())
            .description(t.getDescription())
            .durationSeconds(t.getDurationSeconds())
            .questionsToPresent(t.getQuestionsToPresent())
            .testMode(t.getTestMode())
            .isActive(t.getIsActive())
            .build();
    }
}
