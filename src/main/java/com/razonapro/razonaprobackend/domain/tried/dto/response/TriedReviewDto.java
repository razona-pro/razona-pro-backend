package com.razonapro.razonaprobackend.domain.tried.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO completo para el post-test review.
 * Incluye cada pregunta con todas las opciones, cuál eligió el estudiante,
 * cuál era la correcta y la explicación de la pregunta.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TriedReviewDto {

    private String        triedId;
    private String        testId;
    private String        testName;
    private String        status;
    private BigDecimal    score;
    private Integer       totalQuestions;
    private Integer       correctAnswers;
    private Integer       timeSpentSeconds;
    private LocalDateTime attemptTimestamp;
    private LocalDateTime finishedAt;

    private List<QuestionReview> questions;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class QuestionReview {
        private String       questionId;
        private String       competenceId;
        private String       statement;
        private String       explanation;
        private String       source;
        private String       difficultyLevel;
        private Boolean      answeredCorrectly;
        private String       selectedOptionId;   // null si no respondió
        private String       correctOptionId;
        private List<OptionReview> options;
    }

    @Getter
    @Builder
    public static class OptionReview {
        private String  optionId;
        private String  optionText;
        private Boolean isCorrect;
        private Boolean wasSelected;
    }
}
