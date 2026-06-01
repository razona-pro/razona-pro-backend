package com.razonapro.razonaprobackend.domain.stats.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class QuestionTrendDto {
    private String questionId;
    private String competenceId;
    private String statement;
    private long   totalAnswers;
    private long   correctAnswers;
    private double errorRate;
}
