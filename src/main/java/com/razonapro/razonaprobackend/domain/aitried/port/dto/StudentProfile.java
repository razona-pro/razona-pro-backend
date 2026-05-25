package com.razonapro.razonaprobackend.domain.aitried.port.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter @Builder
public class StudentProfile {
    private String studentId;
    private String competenceId;
    private double historicalScore;
    private int    totalAttempts;
    private List<String> weakTopics;
    private String preferredDifficulty;  // B | M | A
}