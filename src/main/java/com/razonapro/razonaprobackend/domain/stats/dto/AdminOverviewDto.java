package com.razonapro.razonaprobackend.domain.stats.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class AdminOverviewDto {
    private long   totalStudents;
    private long   activeStudents;
    private long   inactiveStudents;
    private long   totalQuestions;
    private long   activeQuestions;
    private long   totalCompetences;
    private long   activeCompetences;
    private long   totalTests;
    private long   finishedTrieds;
    private long   inProgressTrieds;
    private long   openDoubts;
    private long   totalAiSessions;
    private double avgScore;
    private double satisfactionRate;
}
