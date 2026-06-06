package com.razonapro.razonaprobackend.domain.test.dto.response;

import com.razonapro.razonaprobackend.domain.test.model.Test;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter @Builder
public class TestDto {
    private String  testId;
    private String  testName;
    private String  description;
    private Integer durationSeconds;
    private Integer questionsToPresent;
    private String  testMode;
    private Boolean isActive;
    /** Competencias asociadas a la prueba (multicompetencia). */
    private List<CompetenceRef> competences;
    /** Cantidad de preguntas por nivel: B=Básico, M=Medio, A=Alto */
    private Map<String, Long> difficultyBreakdown;
    /** Total de preguntas ASIGNADAS al test (suma del breakdown). Si questionsToPresent es null = se presentan todas. */
    private Long questionCount;

    @Getter @Builder
    public static class CompetenceRef {
        private String competenceId;
        private String competenceName;
    }

    public static TestDto from(Test t) {
        return TestDto.builder()
                .testId(t.getTestId())
                .testName(t.getTestName())
                .description(t.getDescription())
                .durationSeconds(t.getDurationSeconds())
                .questionsToPresent(t.getQuestionsToPresent())
                .testMode(t.getTestMode())
                .isActive(t.getIsActive())
                .build();
    }

    public static TestDto from(Test t, Map<String, Long> difficultyBreakdown, List<CompetenceRef> competences) {
        return TestDto.builder()
                .testId(t.getTestId())
                .testName(t.getTestName())
                .description(t.getDescription())
                .durationSeconds(t.getDurationSeconds())
                .questionsToPresent(t.getQuestionsToPresent())
                .testMode(t.getTestMode())
                .isActive(t.getIsActive())
                .competences(competences)
                .difficultyBreakdown(difficultyBreakdown)
                .questionCount(difficultyBreakdown == null ? null
                        : difficultyBreakdown.values().stream().filter(java.util.Objects::nonNull).mapToLong(Long::longValue).sum())
                .build();
    }
}
