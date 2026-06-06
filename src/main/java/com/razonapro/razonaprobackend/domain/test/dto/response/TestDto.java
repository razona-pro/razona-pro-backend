package com.razonapro.razonaprobackend.domain.test.dto.response;

import com.razonapro.razonaprobackend.domain.test.model.Test;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter @Builder
public class TestDto {
    private String  testId;
    private String  competenceId;
    private String  competenceName;
    private String  testName;
    private String  description;
    private Integer durationSeconds;
    private Integer questionsToPresent;
    private String  testMode;
    private Boolean isActive;
    /** Cantidad de preguntas por nivel: B=Básico, M=Medio, A=Alto */
    private Map<String, Long> difficultyBreakdown;
    /** Total de preguntas ASIGNADAS al test (suma del breakdown). Si questionsToPresent es null = se presentan todas. */
    private Long questionCount;

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

    public static TestDto from(Test t, Map<String, Long> difficultyBreakdown) {
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
                .difficultyBreakdown(difficultyBreakdown)
                .questionCount(difficultyBreakdown == null ? null
                        : difficultyBreakdown.values().stream().filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum())
                .build();
    }
}
