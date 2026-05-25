package com.razonapro.razonaprobackend.domain.stats.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class HomeStatsDto {
    private long   activeStudents;
    private long   completedTrieds;
    private long   activeCompetences;
    private double satisfactionRate;
    private long   totalQuestions;
    private long   totalTests;
}