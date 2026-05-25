package com.razonapro.razonaprobackend.domain.aitried.port.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter @Builder
public class AiGeneratedQuestion {
    private String statement;
    private List<AiOption> options;
    private String correctOptionIndex;
    private String explanation;
    private String difficultyLevel;
    private Double modelConfidence;
    private String modelVersion;
}