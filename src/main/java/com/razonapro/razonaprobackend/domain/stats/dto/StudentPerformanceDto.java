package com.razonapro.razonaprobackend.domain.stats.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class StudentPerformanceDto {
    private String studentId;
    private long   totalTrieds;
    private double avgScore;
    private long   totalCorrect;
    private long   totalQuestions;
    private double accuracyRate;
}
